import {JSONObject} from "@/api/recordModel";
import {PrimitiveValue} from "@/domain/entityModel";
import {
    isPrimitiveType,
    parseFieldTypeId,
    PrimitiveType,
    RawSchema,
    RecordId,
    SField,
    SForeignKey,
    SInterface,
    SItem,
    SStruct,
    STable
} from "@/api/schemaModel";

export class Schema {
    isEditable: boolean;
    itemMap: Map<string, SItem> = new Map<string, SItem>();
    itemIncludeImplMap: Map<string, SItem> = new Map<string, SItem>();
    lastModifiedMap: Map<string, Map<string, number>>;

    constructor(public rawSchema: RawSchema) {
        this.isEditable = rawSchema.isEditable;
        this.lastModifiedMap = obj2map(rawSchema.lastModifiedMap);
        // 结构名称->结构  方便查找
        for (const item of rawSchema.items) {
            if (item.type == 'interface') {
                const ii = item;
                // impl 附带上接口名称
                for (const impl of ii.impls) {
                    impl.extends = ii;
                    impl.id = ii.name + "." + impl.name;

                    this.itemIncludeImplMap.set(impl.id, impl);
                }
            }
            this.itemMap.set(item.name, item);
            this.itemIncludeImplMap.set(item.name, item);
        }

        // 构造map entry type（字段类型解析走 parseFieldTypeId 单一来源，替代手写 startsWith+substring+split）
        const mapEntryTypes = new Map<string, SStruct>();
        for (const item of this.itemIncludeImplMap.values()) {
            if (item.type != "interface") {
                const ss = item;
                for (const {name, type} of ss.fields) {
                    const ft = parseFieldTypeId(type);
                    if (ft.kind === 'map') {
                        const entryTypeName = getMapEntryTypeName(ss, name);
                        const entryType: SStruct = {
                            name: entryTypeName,
                            type: "struct",
                            comment: '',
                            fields: [{
                                name: 'key',
                                type: ft.key,
                                comment: '',
                            }, {
                                name: 'value',
                                type: ft.value,
                                comment: '',
                            }],
                        }
                        mapEntryTypes.set(entryTypeName, entryType);
                    }
                }
            }
        }
        for (const item of mapEntryTypes.values()) {
            this.itemIncludeImplMap.set(item.name, item);
        }

        // table内 id -> (id,title?) 方便查找
        for (const item of rawSchema.items) {
            this.getAllRefTablesByItem(item);
            if (item.type == 'table') {
                const st = item;
                st.refInTables = new Set<string>();
                st.idMap = new Map<string, RecordId>();
                for (const recordId of st.recordIds) {
                    st.idMap.set(recordId.id, recordId);
                }
            }
        }

        // table 被哪些表 外键连接。refTables 由上一轮 getAllRefTablesByItem(item) 填充（Nameable 上类型可选），
        // 用守卫替代 as Set<string> 强转：脏数据下安全跳过，而非吃 undefined。
        for (const item of rawSchema.items) {
            if (item.type == 'table') {
                const st = item;
                const refTables = st.refTables;
                if (!refTables) {
                    console.error(`table ${st.name} has no refTables (dirty schema?)`);
                    continue;
                }
                for (const refTable of refTables) {
                    // getSTable 返回 nullable：脏 schema（refTable 不存在）时跳过 + 打日志定位，
                    // 不再 as STable 强转吃 null、靠 ?. 静默丢反向索引条目。
                    const t = this.getSTable(refTable);
                    if (t) {
                        t.refInTables?.add(st.name);
                    } else {
                        console.error(`refTable ${refTable} not found in schema (dirty schema?)`);
                    }
                }
            }
        }
    }

    getSTable(name: string): STable | null {
        const item = this.itemMap.get(name);
        if (item && item.type == 'table') {
            return item;
        }
        return null;
    }

    /** 按 name 查 struct/interface（不含 table；字段类型为 list<struct>/struct/interface 引用时用）。
     *  返回 nullable：调用方须显式处理缺失，替代原先 itemIncludeImplMap.get(name) as SStruct|SInterface
     *  把 undefined 吃成对象、掩盖脏 schema 的 null 隐患。table 名查到这里返回 null（字段类型不该引用 table）。 */
    getStructOrInterface(name: string): SStruct | SInterface | null {
        const item = this.itemIncludeImplMap.get(name);
        if (item && item.type !== 'table') return item;
        return null;
    }

    hasId(table: STable, id: string): boolean {
        if (table.idMap) {
            return table.idMap.has(id);
        }
        return false;
    }

    getDirectDepStructsByItem(item: SItem): Set<string> {
        const map = this.getDirectDepStructsMapByItem(item);
        return new Set(map.keys())
    }

    getDirectDepStructsMapByItem(item: SItem): Map<string, string> {
        const depNameMap = new Map<string, string>();
        if (item.type == 'interface') {
            for (const impl of item.impls) {
                depNameMap.set(impl.id ?? impl.name, '@out');
            }
            return depNameMap;
        }

        for (const {name, type} of item.fields) {
            const ft = parseFieldTypeId(type);
            switch (ft.kind) {
                case 'primitive':
                    continue;
                case 'list':
                    if (!isPrimitiveType(ft.item)) depNameMap.set(ft.item, name);
                    break;
                case 'map':
                    if (!isPrimitiveType(ft.key)) depNameMap.set(ft.key, name);
                    if (!isPrimitiveType(ft.value)) depNameMap.set(ft.value, name);
                    break;
                case 'ref':
                    depNameMap.set(ft.name, name);
                    break;
            }
        }

        if (depNameMap.size > 0) {
            // 去掉对impl的依赖
            const depNameMapGlobal = new Map<string, string>();
            for (const [type, name] of depNameMap) {
                if (this.itemMap.has(type)) {
                    depNameMapGlobal.set(type, name);
                }
                // console.log(`getDepStructs ${item.name}, ${type} not found!`);
            }
            return depNameMapGlobal;
        }

        return depNameMap;
    }

    getDirectDepStructsByItems(items: SItem[]): Set<string> {
        const res = new Set<string>();
        for (const item of items) {
            const r = this.getDirectDepStructsByItem(item);
            setUnion(res, r);
        }
        return res;
    }


    private ids2items(ids: Set<string>): SItem[] {
        const ss: SItem[] = [];
        for (const id of ids) {
            const item = this.itemIncludeImplMap.get(id);
            if (item) {
                ss.push(item);
            } else {
                console.error(`${id} not found!`);
            }
        }
        return ss;
    }


    getAllDepStructs(item: SItem): Set<string> {
        const res = new Set<string>();
        res.add(item.id ?? item.name);
        let frontier = this.getDirectDepStructsByItem(item);
        frontier.delete(item.id ?? item.name);
        while (frontier.size > 0) {
            setUnion(res, frontier);
            const frontierItems = this.ids2items(frontier);
            const newFrontier = this.getDirectDepStructsByItems(frontierItems);
            setDelete(newFrontier, res);

            frontier = newFrontier;
        }
        return res;
    }


    getAllRefTablesByItem(item: SItem): Set<string> {
        if (item.refTables) {
            return item.refTables;
        }

        const allDepIds = this.getAllDepStructs(item);
        const allDepStructs = this.ids2items(allDepIds);

        const res = new Set<string>();

        for (const si of allDepStructs) {
            if (si.type == 'interface') {
                if (si.enumRef) {
                    res.add(si.enumRef);
                }
            } else {
                if (si.foreignKeys) {
                    for (const fk of si.foreignKeys) {
                        res.add(fk.refTable);
                    }
                }
            }
        }
        item.refTables = res;
        return res;
    }

    getAllRefTablesByItems(items: SItem[]): Set<string> {
        const res = new Set<string>();
        for (const item of items) {
            const r = this.getAllRefTablesByItem(item);
            setUnion(res, r);
        }
        return res;
    }

    defaultValue(sFieldable: SStruct | SInterface, visited: Set<string> = new Set()): JSONObject {
        if ('impls' in sFieldable) {
            return this.defaultValueOfInterface(sFieldable, visited);
        } else {
            return this.defaultValueOfStructural(sFieldable, visited);
        }
    }

    defaultValueOfStructural(sStruct: SStruct | STable, visited: Set<string> = new Set()): JSONObject {
        const res: JSONObject = {"$type": sStruct.id ?? sStruct.name};
        const name = sStruct.id ?? sStruct.name;
        // 路径式 visited（进入时加入、离开时删除）：只检测同一路径上的环（如 A 直接含 A 字段，
        // 不经 list<> 隔断时递归会栈溢出），兄弟分支重复引用同一类型不算环。
        // 命中环时返回只含 $type 的骨架对象，不再下钻。
        if (visited.has(name)) {
            return res;
        }
        visited.add(name);
        for (const field of sStruct.fields) {
            const ft = parseFieldTypeId(field.type);
            switch (ft.kind) {
                case 'primitive':
                    res[field.name] = defaultValueOfPrimitive(ft.name);
                    break;
                case 'list':
                case 'map':
                    // map<K,V> 在 cfggen 的 JSON 序列化里是 entry 结构体的 list（见构造器 mapEntryTypes 填充），
                    // 故 map 与 list 的默认值都是空数组 []。
                    res[field.name] = [];
                    break;
                case 'ref': {
                    const sf = this.getStructOrInterface(ft.name);
                    if (sf) {
                        res[field.name] = this.defaultValue(sf, visited);
                    }
                    break;
                }
            }
        }
        visited.delete(name);
        return res;
    }

    defaultValueOfInterface(sInterface: SInterface, visited: Set<string> = new Set()): JSONObject {
        // getImpl 返回 nullable：defaultImpl 指定但找不到（脏 schema）时 fallback impls[0]；
        // 两者皆空时返回空对象避免 NPE、打日志定位（不再 as SStruct 强转吃 null）。
        const impl = (sInterface.defaultImpl ? getImpl(sInterface, sInterface.defaultImpl) : null) ?? sInterface.impls[0];
        if (!impl) {
            console.error(`interface ${sInterface.name} has no impl (dirty schema?)`);
            return {$type: sInterface.name};
        }
        return this.defaultValueOfStructural(impl, visited);
    }


    getFkTargetHandle(fk: SForeignKey): string {
        if (fk.refKeys && fk.refKeys.length > 0) {
            return `@in_${fk.refKeys[0]}`;
        }
        const ref = this.getSTable(fk.refTable);
        if (ref) {
            // 脏 schema 下 pk 可能为空数组，避免产出 "@in_undefined"，与下方缺失表一样兜底 '@in'
            return ref.pk.length > 0 ? `@in_${ref.pk[0]}` : '@in';
        }
        return '@in';
    }

    getSTableByLastName(tableLabel: string): STable | undefined {
        // 收集全部同名末尾的表：恰好 1 个才返回；>1 个属于歧义（如 a.task / b.task 并存），
        // 命中先注册者会静默挂错表，故打日志并返回 undefined；0 个行为不变。
        let found: STable | undefined;
        const ambiguous: string[] = [];
        for (const item of this.itemMap.values()) {
            if (item.type == 'table') {
                let name = item.name
                const i = name.lastIndexOf('.');
                if (i != -1) {
                    name = name.substring(i + 1);
                }
                if (name == tableLabel) {
                    if (found) {
                        ambiguous.push(item.name);
                    } else {
                        found = item;
                    }
                }
            }
        }
        if (ambiguous.length > 0) {
            console.error(`getSTableByLastName(${tableLabel}) ambiguous: ${[found!.name, ...ambiguous].join(', ')}`);
            return undefined;
        }
        return found;
    }

    getIdTitle(table: string, id: string): string | undefined {
        const t = this.getSTable(table);
        if (t && t.idMap) {
            const r = t.idMap.get(id);
            return r?.title;
        }
    }
}

function setUnion(dst: Set<string>, from: Set<string>) {
    for (const s of from) {
        dst.add(s);
    }
}

function setDelete(dst: Set<string>, from: Set<string>) {
    for (const s of from) {
        dst.delete(s);
    }
}

/** 原始类型默认值（单一来源）：bool→false / int|long|float→0 / str|text→''。
 *  替代原先散落在 schema.defaultValueOfStructural / embedding.getFieldValue /
 *  recordEditEntityCreator.getPrimitiveValue 的三份重复 switch。
 *  PrimitiveType 字面量扩展时由 TS exhaustive switch 强制同步（漏一个即编译报错）。 */
export function defaultValueOfPrimitive(type: PrimitiveType): PrimitiveValue {
    switch (type) {
        case 'bool':
            return false;
        case 'int':
        case 'long':
        case 'float':
            return 0;
        case 'str':
        case 'text':
            return '';
    }
}

export function getField(structural: STable | SStruct, fieldName: string): SField | null {
    for (const field of structural.fields) {
        if (field.name == fieldName) {
            return field;
        }
    }
    return null;
}

export function getImpl(sInterface: SInterface, implName: string): SStruct | null {
    for (const impl of sInterface.impls) {
        if (impl.name == implName) {
            return impl;
        }
    }
    return null;
}

export function getNextId(sTable: STable, curId: string): number | null {
    if (!isPkInteger(sTable)) {
        return null;
    }
    let id = parseInt(curId);
    if (isNaN(id)) {
        id = 0;
    }

    const intIdSet = new Set<number>();
    for (const recordId of sTable.recordIds) {
        const v = parseInt(recordId.id);
        intIdSet.add(v);
    }

    id++;
    while (intIdSet.has(id)) {
        id++;
    }

    return id;
}

export function isPkInteger(sTable: STable) {
    if (sTable.pk.length > 1) {
        return false;
    }

    const field = getField(sTable, sTable.pk[0]);
    if (field == null) {
        return false;
    }

    return field.type == 'int' || field.type == 'long';
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function obj2map(v: any): any {
    if (Array.isArray(v)) return v.map(obj2map);
    return v && typeof v == "object" ? new Map(Object.entries(v).map(([k, v]) => [k, obj2map(v)])) : v;
}

export function getDefaultIdInTable(schema: Schema, tableId: string, curId: string) {
    const sTable = schema.getSTable(tableId);
    if (sTable && sTable.recordIds.length > 0) {
        return sTable.recordIds[0].id;
    }
    return curId;
}

export function getMapEntryTypeName(sItem: SItem, fieldName: string) {
    return "$" + (sItem.id ?? sItem.name) + "-" + fieldName; // 构造特殊名称
}


export const NEW_RECORD_ID = "+new";

export type SchemaTableType = {
    schema: Schema,
    notes?: Map<string, string>,
    curTable: STable
};

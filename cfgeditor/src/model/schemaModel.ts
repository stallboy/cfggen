import {JSONObject} from "./recordModel.ts";

export interface Namable {
    name: string;
    type: 'struct' | 'interface' | 'table';
    id?: string;  // 给impl唯一id，由json推理得到
    refTables?: Set<string> // 能索引到的表, cache，是个优化
}

export interface SField {
    name: string;
    type: string;
    comment: string;
}

export interface SForeignKey {
    name: string;
    keys: string[];
    refTable: string;
    refType: 'rPrimary' | 'rUniq' | 'rList' | 'rNullablePrimary' | 'rNullableUniq';
    refKeys?: string[];
}

export interface SStruct extends Namable {
    fields: SField[];
    foreignKeys?: SForeignKey[];
    extends?: SInterface; // 由json推理得到
}

export interface SInterface extends Namable {
    enumRef: string;
    defaultImpl?: string;
    impls: SStruct[];
}

export interface RecordId {
    id: string;
    desc?: string;
}

export interface STable extends Namable {
    pk: string[];
    uks: string[][];
    entryType: 'eNo' | 'eEnum' | 'eEntry';
    entryField?: string;
    fields: SField[];
    foreignKeys?: SForeignKey[];
    recordIds: RecordId[];
    isEditable: boolean;

    refInTables?: Set<string> // 被这些表索引， cache
    idSet?: Set<string>;
}

export type SItem = SStruct | SInterface | STable;

export interface RawSchema {
    isEditable: boolean;
    items: SItem[];
}

export class Schema {
    isEditable: boolean;
    itemMap: Map<string, SItem> = new Map<string, SItem>();
    itemIncludeImplMap: Map<string, SItem> = new Map<string, SItem>();

    constructor(rawSchema: RawSchema) {
        this.isEditable = rawSchema.isEditable;
        for (let item of rawSchema.items) {
            if (item.type == 'interface') {
                let ii = item as SInterface;
                for (let impl of ii.impls) {
                    impl.extends = ii;
                    impl.id = ii.name + "." + impl.name;

                    this.itemIncludeImplMap.set(impl.id, impl);
                }
            }
            this.itemMap.set(item.name, item);
            this.itemIncludeImplMap.set(item.name, item);
        }

        for (let item of rawSchema.items) {
            this.getAllRefTablesByItem(item);
            if (item.type == 'table') {
                let st = item as STable;
                st.refInTables = new Set<string>();
                st.idSet = new Set<string>();
                for (let recordId of st.recordIds) {
                    st.idSet.add(recordId.id);
                }
            }
        }

        for (let item of rawSchema.items) {
            if (item.type == 'table') {
                let st = item as STable;
                let refTables = st.refTables as Set<string>;
                for (let refTable of refTables) {
                    let t: STable = this.getSTable(refTable) as STable;
                    t.refInTables?.add(st.name);
                }
            }
        }
    }

    getFirstSTable(): STable | null {
        for (let item of this.itemMap.values()) {
            if (item.type == 'table') {
                return item as STable;
            }
        }
        return null;
    }

    getSTable(name: string): STable | null {
        let item = this.itemMap.get(name);
        if (item && item.type == 'table') {
            return item as STable;
        }
        return null;
    }

    hasId(table: STable, id: string): boolean {
        if (table.idSet) {
            return table.idSet.has(id);
        }
        return false;
    }

    getDirectDepStructsByItem(item: SItem): Set<string> {
        let depNameSet = new Set<string>();
        if (item.type == 'interface') {
            let ii = item as SInterface;
            for (let impl of ii.impls) {
                depNameSet.add(impl.id ?? impl.name);
            }
            return depNameSet;
        }

        item = item as SStruct | STable
        const primitiveTypeSet = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

        for (let field of item.fields) {
            let type = field.type;
            if (primitiveTypeSet.has(type)) {
                continue;
            }
            if (type.startsWith("list<")) {
                let itemType = type.slice(5, type.length - 1);
                if (!primitiveTypeSet.has(itemType)) {
                    depNameSet.add(itemType);
                }
            } else if (type.startsWith("map<")) {
                let item = type.slice(4, type.length - 1);
                let sp = item.split(",");
                let keyType = sp[0].trim();
                let valueType = sp[1].trim();
                if (!primitiveTypeSet.has(keyType)) {
                    depNameSet.add(keyType);
                }
                if (!primitiveTypeSet.has(valueType)) {
                    depNameSet.add(valueType);
                }
            } else {
                depNameSet.add(type);
            }
        }

        // 把局部空间的名字，全转换成全局空间的名字
        if (depNameSet.size > 0) {
            let interfaceNamespace;
            let implNameSet = new Set<string>();
            let pitem: SItem = item;
            if (item.type == 'struct') {
                let si = item as SStruct;
                if (si.extends) {
                    pitem = si.extends;
                    interfaceNamespace = `${si.extends.name}.`;
                    for (let impl of si.extends.impls) {
                        implNameSet.add(impl.name);
                    }
                }
            }

            let moduleNamespace;
            let lastIdx = pitem.name.lastIndexOf(".");
            if (lastIdx != -1) {
                moduleNamespace = pitem.name.substring(0, lastIdx + 1);
            }

            let depNameSetGlobal = new Set<string>();
            for (let n of depNameSet) {
                if (interfaceNamespace) {
                    if (implNameSet.has(n)) {
                        depNameSetGlobal.add(interfaceNamespace + n);
                        continue;
                    }
                }

                if (moduleNamespace) {
                    let fn = moduleNamespace + n;
                    if (this.itemMap.has(fn)) {
                        depNameSetGlobal.add(fn);
                        continue;
                    }
                }

                if (this.itemMap.has(n)) {
                    depNameSetGlobal.add(n);
                    continue;
                }

                console.log(`getDepStructs ${item.name}, ${n} not found!`);
            }
            return depNameSetGlobal;
        }

        return depNameSet;
    }

    getDirectDepStructsByItems(items: SItem[]): Set<string> {
        let res = new Set<string>();
        for (let item of items) {
            let r = this.getDirectDepStructsByItem(item);
            setUnion(res, r);
        }
        return res;
    }


    getDirectRefTables(items: SItem[]): Set<string> {
        let res = new Set<string>();
        for (let item of items) {
            if (item.type == 'interface') {
                let ii = item as SInterface;
                res.add(ii.enumRef);  // 这里不再遍历impls，因为假设impl被包含在参数items里
            } else {
                let si = item as (SStruct | STable)
                if (si.foreignKeys) {
                    for (let fk of si.foreignKeys) {
                        res.add(fk.refTable);
                    }
                }
            }
        }
        return res;
    }

    private ids2items(ids: Set<string>): SItem[] {
        let ss: SItem[] = [];
        for (let id of ids) {
            let item = this.itemIncludeImplMap.get(id);
            if (item) {
                ss.push(item);
            } else {
                console.error(`${id} not found!`);
            }
        }
        return ss;
    }


    getAllDepStructs(item: SItem): Set<string> {
        let res = new Set<string>();
        res.add(item.id ?? item.name);
        let frontier = this.getDirectDepStructsByItem(item);
        frontier.delete(item.id ?? item.name);
        while (frontier.size > 0) {
            setUnion(res, frontier);
            let frontierItems = this.ids2items(frontier);
            let newFrontier = this.getDirectDepStructsByItems(frontierItems);
            setDelete(newFrontier, res);

            frontier = newFrontier;
        }
        return res;
    }


    getAllRefTablesByItem(item: SItem): Set<string> {
        if (item.refTables) {
            return item.refTables;
        }

        let allDepIds = this.getAllDepStructs(item);
        let allDepStructs = this.ids2items(allDepIds);

        let res = new Set<string>();

        for (let si of allDepStructs) {
            if (si.type == 'interface') {
                let ii = si as SInterface;
                res.add(ii.enumRef);
            } else {
                si = si as (SStruct | STable)
                if (si.foreignKeys) {
                    for (let fk of si.foreignKeys) {
                        res.add(fk.refTable);
                    }
                }
            }
        }
        item.refTables = res;
        return res;
    }

    getAllRefTablesByItems(items: SItem[]): Set<string> {
        let res = new Set<string>();
        for (let item of items) {
            let r = this.getAllRefTablesByItem(item);
            setUnion(res, r);
        }
        return res;
    }


    defaultValueOfStruct(sStruct: SStruct): JSONObject {
        let res: JSONObject = {"$type": sStruct.id ?? sStruct.name};
        for (let field of sStruct.fields) {
            let n = field.name;
            let t = field.type;
            if (t == 'bool') {
                res[n] = false;
            } else if (t == 'int' || t == 'long' || t == 'float') {
                res[n] = 0;
            } else if (t == 'str' || t == 'text') {
                res[n] = '';
            } else if (t.startsWith('list<') || t.startsWith('map<')) {
                res[n] = [];
            } else {
                let sf = this.itemIncludeImplMap.get(t);
                if (sf) {
                    // TODO recursive check
                    if ('impls' in sf) {
                        res[n] = this.defaultValueOfInterface(sf as SInterface);
                    } else {
                        res[n] = this.defaultValueOfStruct(sf as SStruct);
                    }
                }
            }
        }
        return res;
    }

    defaultValueOfInterface(sInterface: SInterface): JSONObject {
        let impl: SStruct;
        if (sInterface.defaultImpl) {
            impl = getImpl(sInterface, sInterface.defaultImpl) as SStruct;
        } else {
            impl = sInterface.impls[0];
        }
        return this.defaultValueOfStruct(impl);
    }


}

function setUnion(dst: Set<string>, from: Set<string>) {
    for (let s of from) {
        dst.add(s);
    }
}

function setDelete(dst: Set<string>, from: Set<string>) {
    for (let s of from) {
        dst.delete(s);
    }
}

export function getField(item: STable | SStruct, fieldName: string): SField | null {
    for (let field of item.fields) {
        if (field.name == fieldName) {
            return field;
        }
    }
    return null;
}

export function getImpl(sInterface: SInterface, implName: string): SStruct | null {
    for (let impl of sInterface.impls) {
        if (impl.name == implName) {
            return impl;
        }
    }
    return null;
}

import {JSONObject} from "../record/recordModel.ts";
import {RawSchema, RecordId, SField, SForeignKey, SInterface, SItem, SStruct, STable} from "./schemaModel.ts";

export class Schema {
    isEditable: boolean;
    itemMap: Map<string, SItem> = new Map<string, SItem>();
    itemIncludeImplMap: Map<string, SItem> = new Map<string, SItem>();

    constructor(public rawSchema: RawSchema) {
        this.isEditable = rawSchema.isEditable;
        for (const item of rawSchema.items) {
            if (item.type == 'interface') {
                const ii = item as SInterface;
                for (const impl of ii.impls) {
                    impl.extends = ii;
                    impl.id = ii.name + "." + impl.name;

                    this.itemIncludeImplMap.set(impl.id, impl);
                }
            }
            this.itemMap.set(item.name, item);
            this.itemIncludeImplMap.set(item.name, item);
        }

        for (const item of rawSchema.items) {
            this.getAllRefTablesByItem(item);
            if (item.type == 'table') {
                const st = item as STable;
                st.refInTables = new Set<string>();
                st.idSet = new Set<string>();
                for (const recordId of st.recordIds) {
                    st.idSet.add(recordId.id);
                }
            }
        }

        for (const item of rawSchema.items) {
            if (item.type == 'table') {
                const st = item as STable;
                const refTables = st.refTables as Set<string>;
                for (const refTable of refTables) {
                    const t: STable = this.getSTable(refTable) as STable;
                    t.refInTables?.add(st.name);
                }
            }
        }
    }

    getFirstSTable(): STable | null {
        for (const item of this.itemMap.values()) {
            if (item.type == 'table') {
                return item as STable;
            }
        }
        return null;
    }

    getSTable(name: string): STable | null {
        const item = this.itemMap.get(name);
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
        const map = this.getDirectDepStructsMapByItem(item);
        return new Set(map.keys())
    }

    getDirectDepStructsMapByItem(item: SItem): Map<string, string> {
        const depNameMap = new Map<string, string>();
        if (item.type == 'interface') {
            const ii = item as SInterface;
            for (const impl of ii.impls) {
                depNameMap.set(impl.id ?? impl.name, '@out');
            }
            return depNameMap;
        }

        item = item as SStruct | STable
        const primitiveTypeSet = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

        for (const {name, type} of item.fields) {
            if (primitiveTypeSet.has(type)) {
                continue;
            }
            if (type.startsWith("list<")) {
                const itemType = type.slice(5, type.length - 1);
                if (!primitiveTypeSet.has(itemType)) {
                    depNameMap.set(itemType, name);
                }
            } else if (type.startsWith("map<")) {
                const item = type.slice(4, type.length - 1);
                const sp = item.split(",");
                const keyType = sp[0].trim();
                const valueType = sp[1].trim();
                if (!primitiveTypeSet.has(keyType)) {
                    depNameMap.set(keyType, name);
                }
                if (!primitiveTypeSet.has(valueType)) {
                    depNameMap.set(valueType, name);
                }
            } else {
                depNameMap.set(type, name);
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

        for (let si of allDepStructs) {
            if (si.type == 'interface') {
                const ii = si as SInterface;
                if (ii.enumRef) {
                    res.add(ii.enumRef);
                }
            } else {
                si = si as (SStruct | STable)
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

    defaultValue(sFieldable: SStruct | SInterface): JSONObject {
        if ('impls' in sFieldable) {
            return this.defaultValueOfInterface(sFieldable as SInterface);
        } else {
            return this.defaultValueOfStructural(sFieldable as SStruct);
        }
    }

    defaultValueOfStructural(sStruct: SStruct | STable): JSONObject {
        const res: JSONObject = {"$type": sStruct.id ?? sStruct.name};
        for (const field of sStruct.fields) {
            const n = field.name;
            const t = field.type;
            if (t == 'bool') {
                res[n] = false;
            } else if (t == 'int' || t == 'long' || t == 'float') {
                res[n] = 0;
            } else if (t == 'str' || t == 'text') {
                res[n] = '';
            } else if (t.startsWith('list<') || t.startsWith('map<')) {
                res[n] = [];
            } else {
                const sf = this.itemIncludeImplMap.get(t);
                if (sf) {
                    res[n] = this.defaultValue(sf);
                    // TODO recursive check
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
        return this.defaultValueOfStructural(impl);
    }


    getFkTargetHandle(fk: SForeignKey): string {
        if (fk.refKeys && fk.refKeys.length > 0) {
            return `@in_${fk.refKeys[0]}`;
        }
        const ref = this.getSTable(fk.refTable);
        if (ref) {
            return `@in_${ref.pk[0]}`;
        }
        return '@in';
    }

    getSTableByLastName(tableLabel:string) : STable|undefined{
        for (const item of this.itemMap.values()) {
            if (item.type == 'table') {
                let name = item.name
                const i = name.lastIndexOf('.');
                if (i != -1){
                    name = name.substring(i+1);
                }
                if (name == tableLabel){
                    return item as STable;
                }
            }
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

export function newSchema(schema: Schema, table: string, recordIds: RecordId[]) {
    const items: SItem[] = [];
    for (const item of schema.rawSchema.items) {
        if (item.name == table) {
            const updatedItem = {...item, recordIds: recordIds};
            items.push(updatedItem);
        } else {
            items.push(item);
        }
    }
    return new Schema({isEditable: schema.isEditable, items});
}

export function getNextId(sTable: STable, curId: string): number | null {
    if (!isPkInteger(sTable)) {
        return null;
    }
    let id = parseInt(curId);
    if (isNaN(id)) {
        id = 0;
    }

    const idSet = new Set<number>();
    for (const recordId of sTable.recordIds) {
        const v = parseInt(recordId.id);
        idSet.add(v);
    }

    id++;
    while (idSet.has(id)) {
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

export function getIdOptions(sTable: STable, valueToInteger: boolean = false) {
    const options = [];
    for (const {id, title} of sTable.recordIds) {
        const label = (title && title != id) ? `${id}-${title}` : id;
        options.push({label, value: valueToInteger ? parseInt(id) : id});
    }
    return options;
}


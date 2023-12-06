import {Schema} from "inspector";

export interface Namable {
    name: string;
    type: string;
}

export interface SField {
    name: string;
    type: string;
    comment: string;
}

export interface SForeignKey {
    name: string;
    keys: string[];
    refType: string;
    refKeys?: string[];
}

export interface SStruct extends Namable {
    fields: SField[];
    foreignKeys?: SForeignKey[];
    extends?: SInterface;
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
    entryType: string;
    entryField?: string;
    fields: SField[];
    foreignKeys?: SForeignKey[];
    recordCount: number;
    recordIds: RecordId[];
}

export type SItem = SStruct | SInterface | STable;

export interface RawSchema {
    items: SItem[];
}

export interface Schema {
    itemMap: Map<string, SItem>;
}

export function resolveSchema(rawSchema: RawSchema): Schema {
    let schema: Schema = {itemMap: new Map<string, SItem>()}
    for (let item of rawSchema.items) {
        if (item.type == 'interface') {
            let ii = item as SInterface;
            for (let impl of ii.impls) {
                impl.extends = ii;
            }
        }
        schema.itemMap.set(item.name, item);
    }
    return schema;
}

export function getFirstSTable(schema: Schema): STable | null {
    for (let item of schema.itemMap.values()) {
        if (item.type == 'table') {
            return item as STable;
        }
    }
    return null;
}

export function getSTable(schema: Schema, name: string): STable | null {
    let item = schema.itemMap.get(name);
    if (item && item.type == 'table') {
        return item as STable;
    }
    return null;
}

const set = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

export function getDepStructs(item: STable | SStruct, schema: Schema): Set<string> {
    let res = new Set<string>();
    for (let field of item.fields) {
        let type = field.type;
        if (set.has(type)) {
            continue;
        }
        if (type.startsWith("list")) {
            let itemType = type.slice(5, type.length - 1);
            if (!set.has(itemType)) {
                res.add(itemType);
            }
        } else if (type.startsWith("map")) {
            let item = type.slice(4, type.length - 1);
            let sp = item.split(",");
            let keyType = sp[0].trim();
            let valueType = sp[1].trim();
            if (!set.has(keyType)) {
                res.add(keyType);
            }
            if (!set.has(valueType)) {
                res.add(valueType);
            }
        } else {
            res.add(type);
        }
    }

    if (res.size > 0) {
        let interfaceNamespace;
        let implNameSet = new Set<string>();
        if (item.type == 'struct') {
            let si = item as SStruct;
            if (si.extends) {
                interfaceNamespace = si.extends.name + ".";
                for (let impl of si.extends.impls) {
                    implNameSet.add(impl.name);
                }
            }
        }

        let moduleNamespace;
        let lastIdx = item.name.lastIndexOf(".");
        if (lastIdx != -1) {
            moduleNamespace = item.name.substring(0, lastIdx + 1);
        }

        let fixedRes = new Set<string>();
        for (let n of res) {
            if (interfaceNamespace) {
                if (implNameSet.has(n)) {
                    fixedRes.add(interfaceNamespace + n);
                    continue;
                }
            }

            if (moduleNamespace) {
                let fn = moduleNamespace + n;
                if (schema.itemMap.has(fn)) {
                    fixedRes.add(fn);
                    continue;
                }
            }

            if (schema.itemMap.has(n)) {
                fixedRes.add(n);
                continue;
            }

            console.log("getDepStructs " + item.name + ", " + n + " not found!");
        }
        return fixedRes;
    }

    return res;
}


export function getDepStructs2(items: (STable | SStruct)[], schema: Schema): Set<string> {
    let res = new Set<string>();
    for (let item of items) {
        let r = getDepStructs(item, schema);
        for (let i of r) {
            res.add(i);
        }
    }
    return res;
}
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

export interface Schema {
    items: {
        [field: string]: SItem
    };
}

export function getFirstSTable(schema: Schema): STable | null {
    for (let k in schema.items) {
        let item = schema.items[k];
        if (item.type == 'table') {
            return item as STable;
        }
    }
    return null;
}

export function getSTable(schema: Schema, name: string): STable | null {
    let item = schema.items[name];
    if (item && item.type == 'table') {
        return item as STable;
    }
    return null;
}

const set = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

export function getDepStructs(item: STable | SStruct): Set<string> {
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
    return res;
}


export function getDepStructs2(items: (STable | SStruct)[]) : Set<string> {
    let res = new Set<string>();
    for (let item of items) {
        let r = getDepStructs(item);
        for (let i of r) {
            res.add(i);
        }
    }
    return res;
}
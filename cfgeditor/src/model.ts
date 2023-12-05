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

export interface Schema {
    items: (SStruct | SInterface | STable)[];


}

export function getSTable(schema: Schema, name: string): STable | null {
    for (let item of schema.items) {
        if (item.type == 'table' && item.name == name) {
            return item as STable;
        }
    }
    return null;
}
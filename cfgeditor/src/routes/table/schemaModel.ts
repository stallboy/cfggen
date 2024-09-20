export interface Namable {
    name: string;
    type: 'struct' | 'interface' | 'table';
    comment: string;
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
    enumRef?: string;
    defaultImpl?: string;
    impls: SStruct[];
}

export interface RecordId {
    id: string;
    title?: string;
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


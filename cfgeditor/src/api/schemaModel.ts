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

// ---------------------------------------------------------------------------
// 字段类型分类：原始类型 / 数字类型
// 这是后端 cfggen 类型系统的一部分（SField.type 的字面量分类），集中在此为单一权威，
// 供 domain / flow / routes 各层复用，消除散落的重复集合。
// ---------------------------------------------------------------------------

/** 原始字段类型（cfggen 后端基础类型字面量） */
export type PrimitiveType = 'bool' | 'int' | 'long' | 'float' | 'str' | 'text';

/** 原始类型集合（与 PrimitiveType 字面量一致） */
export const PRIMITIVE_TYPES = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

/** 数字类型集合（int/long/float） */
export const NUMBER_TYPES = new Set<string>(['int', 'long', 'float']);

/** 判断是否为原始类型 */
export function isPrimitiveType(type: string): boolean {
    return PRIMITIVE_TYPES.has(type);
}

/** 判断是否为数字类型 */
export function isNumberType(type: string): boolean {
    return NUMBER_TYPES.has(type);
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

    refInTables?: Set<string> // 被这些表索引， cache
    idMap?: Map<string, RecordId>;
}

export type SItem = SStruct | SInterface | STable;

export interface RawSchema {
    isEditable: boolean;
    items: SItem[];
    lastModifiedMap: Map<string, Map<string, number>>;
}


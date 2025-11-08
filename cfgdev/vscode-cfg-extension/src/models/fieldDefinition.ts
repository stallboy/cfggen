import { TextRange, Position } from './configFile';
import { ForeignKey } from './foreignKeyDefinition';
import { FieldMetadata } from './fieldMetadata';

export interface FieldDefinition {
    name: string;               // 字段名
    type: FieldType;            // 字段类型
    foreignKey?: ForeignKey;    // 可选外键引用
    metadata: FieldMetadata[];  // 元数据列表
    comment?: string;           // 注释
    position: TextRange;        // 位置
}

export type FieldType =
    | BaseType                  // bool, int, long, float, str, text
    | ListType                  // list<type>
    | MapType                   // map<key, value>
    | CustomType;               // struct/interface引用

export interface BaseType {
    kind: 'base';
    name: 'bool' | 'int' | 'long' | 'float' | 'str' | 'text';
}

export interface ListType {
    kind: 'list';
    elementType: FieldType;
}

export interface MapType {
    kind: 'map';
    keyType: FieldType;
    valueType: FieldType;
}

export interface CustomType {
    kind: 'custom';
    namespace: string;          // 完整类型名
    shortName: string;          // 短名称
    definition?: any;    // 解析后的定义引用
}

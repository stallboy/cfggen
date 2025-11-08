import { TextRange } from './configFile';

export interface Metadata {
    name: string;               // 元数据名
    value?: Literal;            // 可选值
    isNegative: boolean;        // 是否为负标识（-前缀）
    position: TextRange;        // 位置
}

export type Literal =
    | IntegerLiteral           // 整型常量
    | HexIntegerLiteral        // 十六进制整数
    | FloatLiteral             // 浮点常量
    | StringLiteral;           // 字符串常量

export interface IntegerLiteral {
    kind: 'integer';
    value: number;
    raw: string;
}

export interface HexIntegerLiteral {
    kind: 'hex';
    value: number;
    raw: string;
}

export interface FloatLiteral {
    kind: 'float';
    value: number;
    raw: string;
}

export interface StringLiteral {
    kind: 'string';
    value: string;
    raw: string;
}

// 常用元数据常量
export const COMMON_METADATA = [
    'nullable',         // 允许空值
    'mustFill',         // 必须填写
    'pack',             // 压缩存储
    'enumRef',          // 枚举引用
    'defaultImpl',      // 默认实现
    'fix',              // 固定长度
    'block',            // 数据块
    'noserver'          // 客户端专用
] as const;

export type CommonMetadataName = typeof COMMON_METADATA[number];

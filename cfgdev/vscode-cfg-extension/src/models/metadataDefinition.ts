import { TextRange } from './configFile';

export interface Metadata {
    name: string;               // 元数据名
    value?: Literal;            // 可选值
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
    kind: 'hexInteger';
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

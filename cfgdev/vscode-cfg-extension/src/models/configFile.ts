import { Definition } from './definition';

export interface ConfigFile {
    path: string;              // 文件绝对路径
    moduleName: string;        // 模块名（task, item, npc等）
    definitions: Definition[]; // 所有定义（struct/interface/table）
    symbols: any;              // 符号表（避免循环导入）
    errors: ParseError[];      // 解析错误
    lastModified: number;      // 最后修改时间
}

export interface ParseError {
    code: string;               // 错误码
    message: string;            // 错误消息
    position: TextRange;        // 错误位置
    severity: 'error' | 'warning' | 'info';
    source?: string;            // 错误来源（语法/语义）
}

export interface TextRange {
    start: Position;
    end: Position;
}

export interface Position {
    line: number;
    character: number;
}

export type DefinitionType = 'struct' | 'interface' | 'table';

export interface Reference {
    target: Definition;
    location: Location;
}

export interface Location {
    uri: string;
    range: TextRange;
}

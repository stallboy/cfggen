import * as vscode from 'vscode';
import { Metadata } from './metadataDefinition';

export interface ConfigFile {
    path: string;              // 文件绝对路径
    moduleName: string;        // 模块名（task, item, npc等）
    definitions: Definition[]; // 所有定义（struct/interface/table）
    symbols: SymbolTable;      // 符号表
    errors: ParseError[];      // 解析错误
    lastModified: number;      // 最后修改时间
}

export abstract class Definition {
    abstract name: string;              // 定义名称
    abstract namespace: string;         // 命名空间（module.qualifier）
    abstract metadata: Metadata[];      // 元数据列表
    abstract comment?: string;          // 可选的文档注释
    abstract position: TextRange;       // 源码位置
}

export interface TextRange {
    start: vscode.Position;
    end: vscode.Position;
}

export interface SymbolTable {
    // 添加定义
    add(definition: Definition): void;

    // 查找定义
    find(name: string, module?: string): Definition | null;
    findAll(type?: DefinitionType): Definition[];

    // 跨模块查找
    findInModule(module: string, name: string): Definition | null;

    // 获取引用关系
    getReferences(target: Definition): Reference[];
}

export interface ParseError {
    code: string;               // 错误码
    message: string;            // 错误消息
    position: TextRange;        // 错误位置
    severity: 'error' | 'warning' | 'info';
    source?: string;            // 错误来源（语法/语义）
}

export type DefinitionType = 'struct' | 'interface' | 'table';

export interface Reference {
    target: Definition;         // 引用的定义
    location: TextRange;        // 引用位置
    type: 'type' | 'foreignKey'; // 引用类型
}

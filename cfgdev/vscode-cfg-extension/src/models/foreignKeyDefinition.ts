import { TextRange } from './configFile';
import { FieldMetadata } from './fieldMetadata';
import { TableDefinition } from './tableDefinition';

export interface ForeignKey {
    name?: string;              // 外键名（可省略）
    referenceType: 'single' | 'list';  // 单值或列表引用
    operator: '->' | '=>';      // 引用操作符
    target: ReferenceTarget;    // 引用目标
    metadata: FieldMetadata[];  // 元数据
    position: TextRange;        // 位置
}

export interface ReferenceTarget {
    module: string;             // 模块名
    table: string;              // 表名
    field?: string;            // 字段名（可选）
    key?: string;              // 键名（可选）
    targetDefinition?: TableDefinition;  // 解析后的定义
}

import { Definition } from './configFile';
import { Metadata } from './metadataDefinition';
import { FieldDefinition } from './fieldDefinition';
import { ForeignKeyDefinition } from './foreignKeyDefinition';

export interface TableDefinition extends Definition {
    type: 'table';
    primaryKey: string[];       // 主键字段
    uniqueKeys: string[][];     // 唯一键列表
    fields: FieldDefinition[];  // 字段列表
    foreignKeys: ForeignKey[];  // 外键定义
    metadata: Metadata[];       // table元数据
}

// Table-specific metadata fields
export interface TableMetadataExtension {
    enumField?: string;         // 枚举字段名
    entryField?: string;        // 入口字段名
    json?: boolean;            // JSON格式
    description?: string;       // 描述
}

export interface ForeignKey extends ForeignKeyDefinition {
    // ForeignKey在table中的扩展
    sourceField: string;        // 源字段名
}

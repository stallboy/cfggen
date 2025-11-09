import { Definition } from './configFile';
import { Metadata } from './metadataDefinition';
import { FieldDefinition } from './fieldDefinition';
import { ForeignKeyDefinition } from './foreignKeyDefinition';

export interface StructDefinition extends Definition {
    type: 'struct';
    fields: FieldDefinition[];  // 字段列表
    foreignKeys: ForeignKey[];  // 外键定义
    metadata: Metadata[];       // struct元数据
}

// Struct-specific metadata fields
export interface StructMetadataExtension {
    sep?: string;              // 分隔符（如时间格式':'）
    pack?: boolean;            // 是否压缩
}

export interface ForeignKey extends ForeignKeyDefinition {
    // ForeignKey在struct中的扩展
    sourceField: string;        // 源字段名
}

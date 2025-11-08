import { Definition } from './definition';
import { FieldDefinition } from './fieldDefinition';
import { ForeignKey } from './foreignKeyDefinition';
import { TableMetadata } from './tableMetadata';
import { TextRange } from './configFile';

export class TableDefinition extends Definition {
    type: 'table' = 'table';
    primaryKey: string[];       // 主键字段
    uniqueKeys: string[][];     // 唯一键列表
    fields: FieldDefinition[];  // 字段列表
    foreignKeys: ForeignKey[];  // 外键定义
    metadata: TableMetadata;    // table特有元数据

    constructor(
        name: string,
        namespace: string,
        position: TextRange
    ) {
        super(name, namespace, position, 'table');
        this.fields = [];
        this.foreignKeys = [];
        this.primaryKey = [];
        this.uniqueKeys = [];
        this.metadata = {};
    }
}

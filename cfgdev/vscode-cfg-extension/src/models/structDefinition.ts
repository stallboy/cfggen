import { Definition } from './definition';
import { FieldDefinition } from './fieldDefinition';
import { ForeignKey } from './foreignKeyDefinition';
import { StructMetadata } from './structMetadata';
import { TextRange } from './configFile';

export class StructDefinition extends Definition {
    type: 'struct' = 'struct';
    fields: FieldDefinition[];  // 字段列表
    foreignKeys: ForeignKey[];  // 外键定义
    metadata: StructMetadata;   // struct特有元数据

    constructor(
        name: string,
        namespace: string,
        position: TextRange
    ) {
        super(name, namespace, position, 'struct');
        this.fields = [];
        this.foreignKeys = [];
        this.metadata = {};
    }
}

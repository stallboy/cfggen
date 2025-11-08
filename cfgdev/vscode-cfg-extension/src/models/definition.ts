import { TextRange, Position } from './configFile';
import { Metadata } from './metadataDefinition';
import { FieldDefinition } from './fieldDefinition';
import { ForeignKey } from './foreignKeyDefinition';

export abstract class Definition {
    name: string;              // 定义名称
    namespace: string;         // 命名空间（module.qualifier）
    abstract metadata: any;    // 元数据列表（具体类型由子类定义）
    comment?: string;          // 可选的文档注释
    position: TextRange;       // 源码位置
    type: 'struct' | 'interface' | 'table';

    constructor(
        name: string,
        namespace: string,
        position: TextRange,
        type: 'struct' | 'interface' | 'table'
    ) {
        this.name = name;
        this.namespace = namespace;
        this.position = position;
        this.type = type;
    }
}

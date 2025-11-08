import { Definition, TextRange } from './configFile';
import { Metadata } from './metadataDefinition';
import { StructDefinition } from './structDefinition';

export interface InterfaceDefinition extends Definition {
    type: 'interface';
    implementations: StructDefinition[];  // 实现类
    metadata: Metadata[];      // interface元数据

    // 多态访问器
    getImplementation(name: string): StructDefinition | null;
    getAllImplementations(): StructDefinition[];
}

// Interface-specific metadata fields
export interface InterfaceMetadataExtension {
    enumRef?: string;          // 枚举引用
    defaultImpl?: string;      // 默认实现
}

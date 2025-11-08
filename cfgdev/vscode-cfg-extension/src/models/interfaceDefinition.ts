import { Definition } from './definition';
import { StructDefinition } from './structDefinition';
import { InterfaceMetadata } from './interfaceMetadata';
import { TextRange } from './configFile';

export class InterfaceDefinition extends Definition {
    type: 'interface' = 'interface';
    implementations: StructDefinition[];  // 实现类
    metadata: InterfaceMetadata;          // interface特有元数据

    constructor(
        name: string,
        namespace: string,
        position: TextRange
    ) {
        super(name, namespace, position, 'interface');
        this.implementations = [];
        this.metadata = {};
    }

    // 多态访问器
    getImplementation(name: string): StructDefinition | null {
        return this.implementations.find(impl => impl.name === name) || null;
    }

    getAllImplementations(): StructDefinition[] {
        return [...this.implementations];
    }
}

import { Definition, Reference } from '../models';

export class SymbolTable {
    private definitions: Map<string, Definition[]> = new Map();
    private byModule: Map<string, Definition[]> = new Map();
    private byType: Map<string, Definition[]> = new Map();

    /**
     * 添加定义到符号表
     */
    add(definition: Definition): void {
        const qualifiedName = this.getQualifiedName(definition);

        if (!this.definitions.has(qualifiedName)) {
            this.definitions.set(qualifiedName, []);
        }
        this.definitions.get(qualifiedName)!.push(definition);

        // 按模块分组
        if (!this.byModule.has(definition.namespace)) {
            this.byModule.set(definition.namespace, []);
        }
        this.byModule.get(definition.namespace)!.push(definition);

        // 按类型分组
        const type = (definition as any).type;
        if (!this.byType.has(type)) {
            this.byType.set(type, []);
        }
        this.byType.get(type)!.push(definition);
    }

    /**
     * 查找定义
     */
    find(name: string, module?: string): Definition | null {
        const qualifiedName = module ? `${module}.${name}` : name;
        const defs = this.definitions.get(qualifiedName);
        return defs && defs.length > 0 ? defs[0] : null;
    }

    /**
     * 查找所有定义
     */
    findAll(type?: DefinitionType): Definition[] {
        if (type) {
            return this.byType.get(type) || [];
        }
        return Array.from(this.definitions.values()).flat();
    }

    /**
     * 在指定模块中查找定义
     */
    findInModule(module: string, name: string): Definition | null {
        return this.find(name, module);
    }

    /**
     * 获取引用关系
     */
    getReferences(target: Definition): Reference[] {
        // TODO: 实现引用查找
        return [];
    }

    private getQualifiedName(definition: Definition): string {
        return definition.namespace ? `${definition.namespace}.${definition.name}` : definition.name;
    }

    /**
     * 清空符号表
     */
    clear(): void {
        this.definitions.clear();
        this.byModule.clear();
        this.byType.clear();
    }

    /**
     * 获取统计信息
     */
    getStats() {
        return {
            totalDefinitions: this.definitions.size,
            moduleCount: this.byModule.size,
            typeCount: this.byType.size
        };
    }
}

type DefinitionType = 'struct' | 'interface' | 'table';

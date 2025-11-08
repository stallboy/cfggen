import { Definition } from './definition';
import { Reference } from './configFile';
import { DefinitionType } from './configFile';

export class SymbolTable {
    private definitions: Map<string, Definition[]>;        // 定义映射
    private byModule: Map<string, Definition[]>;          // 按模块分组
    private byType: Map<string, Definition[]>;            // 按类型分组

    constructor() {
        this.definitions = new Map();
        this.byModule = new Map();
        this.byType = new Map();
    }

    // 添加定义
    add(definition: Definition): void {
        const name = definition.name;
        const module = this.getModuleName(definition.namespace);
        const type = definition.type;

        // 按名称索引
        if (!this.definitions.has(name)) {
            this.definitions.set(name, []);
        }
        this.definitions.get(name)!.push(definition);

        // 按模块索引
        if (!this.byModule.has(module)) {
            this.byModule.set(module, []);
        }
        this.byModule.get(module)!.push(definition);

        // 按类型索引
        if (!this.byType.has(type)) {
            this.byType.set(type, []);
        }
        this.byType.get(type)!.push(definition);
    }

    // 查找定义
    find(name: string, module?: string): Definition | null {
        const defs = this.definitions.get(name);
        if (!defs || defs.length === 0) {
            return null;
        }

        if (module) {
            // 如果指定了模块，在该模块中查找
            const targetModule = module;
            for (const def of defs) {
                if (this.getModuleName(def.namespace) === targetModule) {
                    return def;
                }
            }
        }

        // 返回第一个匹配的定义
        return defs[0];
    }

    // 查找所有定义
    findAll(type?: DefinitionType): Definition[] {
        if (type) {
            return [...(this.byType.get(type) || [])];
        }
        const all: Definition[] = [];
        this.definitions.forEach(defs => {
            all.push(...defs);
        });
        return all;
    }

    // 跨模块查找
    findInModule(module: string, name: string): Definition | null {
        return this.find(name, module);
    }

    // 获取引用关系
    getReferences(target: Definition): Reference[] {
        // TODO: 实现引用查找逻辑
        return [];
    }

    // 辅助方法：提取模块名
    private getModuleName(namespace: string): string {
        const parts = namespace.split('.');
        return parts[0] || namespace;
    }

    // 清除所有定义
    clear(): void {
        this.definitions.clear();
        this.byModule.clear();
        this.byType.clear();
    }

    // 获取所有模块名
    getAllModules(): string[] {
        return Array.from(this.byModule.keys());
    }

    // 获取统计信息
    getStats(): { total: number; byType: Record<string, number> } {
        const total = this.findAll().length;
        const byType: Record<string, number> = {};
        this.byType.forEach((defs, type) => {
            byType[type] = defs.length;
        });
        return { total, byType };
    }
}

import { SymbolTable } from '../models/symbolTable';
import { CfgParserService } from './cfgParserService';
import { ModuleResolverService } from './moduleResolverService';
import { CacheService } from './cacheService';
import { Logger } from '../utils/logger';

export class SymbolTableService {
    private globalSymbolTable: SymbolTable;
    private parserService: CfgParserService;
    private moduleResolver: ModuleResolverService;
    private cacheService: CacheService;
    private logger: Logger;
    private fileSymbolTables: Map<string, SymbolTable>;

    constructor(
        parserService: CfgParserService,
        moduleResolver: ModuleResolverService,
        cacheService: CacheService,
        logger: Logger
    ) {
        this.parserService = parserService;
        this.moduleResolver = moduleResolver;
        this.cacheService = cacheService;
        this.logger = logger;
        this.globalSymbolTable = new SymbolTable();
        this.fileSymbolTables = new Map();
    }

    // 更新符号表
    async updateSymbolTable(uri: string): Promise<void> {
        this.logger.debug(`Updating symbol table for: ${uri}`);

        try {
            // 检查缓存
            const cached = this.cacheService.get(uri);
            if (cached) {
                this.logger.debug(`Using cached symbol table for: ${uri}`);
                // 从缓存恢复符号表
                this.restoreFromCache(uri, cached);
                return;
            }

            // 解析文档
            const configFile = await this.parserService.parseDocument({
                uri,
                getText: () => '',
                version: 1
            } as any);

            if (!configFile) {
                this.logger.warn(`Failed to parse document: ${uri}`);
                return;
            }

            // 创建文件级符号表
            const fileSymbolTable = new SymbolTable();

            // 添加定义到符号表
            for (const definition of configFile.definitions) {
                fileSymbolTable.add(definition);
                this.globalSymbolTable.add(definition);
            }

            // 存储文件符号表
            this.fileSymbolTables.set(uri, fileSymbolTable);

            this.logger.debug(`Symbol table updated for: ${uri}`);
        } catch (error) {
            this.logger.error(`Failed to update symbol table for ${uri}:`, error);
        }
    }

    // 查找定义
    findDefinition(name: string, module?: string): any | null {
        if (module) {
            return this.globalSymbolTable.findInModule(module, name);
        }
        return this.globalSymbolTable.find(name);
    }

    // 查找所有定义
    findAllDefinitions(type?: string): any[] {
        return this.globalSymbolTable.findAll(type as any);
    }

    // 跨模块查找
    findInModule(module: string, name: string): any | null {
        return this.globalSymbolTable.findInModule(module, name);
    }

    // 获取全局符号表
    getGlobalSymbolTable(): SymbolTable {
        return this.globalSymbolTable;
    }

    // 获取文件符号表
    getFileSymbolTable(uri: string): SymbolTable | null {
        return this.fileSymbolTables.get(uri) || null;
    }

    // 清除符号表
    clear(): void {
        this.globalSymbolTable.clear();
        this.fileSymbolTables.clear();
        this.cacheService.clear();
        this.logger.info('All symbol tables cleared');
    }

    // 重新加载文件
    async reload(uri: string): Promise<void> {
        this.logger.info(`Reloading symbol table for: ${uri}`);
        this.cacheService.invalidate(uri);
        this.fileSymbolTables.delete(uri);
        await this.updateSymbolTable(uri);
    }

    // 重新加载所有文件
    async reloadAll(): Promise<void> {
        this.logger.info('Reloading all symbol tables');
        this.clear();
        // TODO: 重新加载所有打开的.cfg文件
    }

    // 从缓存恢复
    private restoreFromCache(uri: string, cached: any): void {
        // TODO: 实现从缓存恢复逻辑
        this.logger.debug(`Restored from cache for: ${uri}`);
    }

    // 获取统计信息
    getStats(): { total: number; byType: Record<string, number>; modules: string[] } {
        const stats = this.globalSymbolTable.getStats();
        return {
            total: stats.total,
            byType: stats.byType,
            modules: this.globalSymbolTable.getAllModules()
        };
    }

    // 获取模块中的所有定义
    getDefinitionsInModule(module: string): any[] {
        const allDefs = this.globalSymbolTable.findAll();
        return allDefs.filter(def => {
            const parts = def.namespace.split('.');
            return parts[0] === module;
        });
    }
}

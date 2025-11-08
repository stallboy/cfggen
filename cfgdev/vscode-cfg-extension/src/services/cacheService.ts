import { SymbolTable } from '../models/symbolTable';
import { ConfigFile } from '../models/configFile';

export interface CachedFileData {
    // 解析后的符号
    symbols: Map<string, any>;
    symbolsByModule: Map<string, any[]>;
    symbolsByFile: Map<string, any[]>;

    // 依赖关系
    dependencies: string[];    // 依赖的其他文件路径

    // 元数据
    lastParsed: number;        // 最后解析时间戳
    fileVersion: number;       // 文件内容版本（基于文件修改时间或哈希）
    size: number;              // 缓存大小（字节）
    lastAccessed: number;      // 最后访问时间
}

export class CacheService {
    private cache = new Map<string, CachedFileData>();
    private maxCacheSize = 100; // 最大缓存文件数
    private maxMemorySize = 50 * 1024 * 1024; // 50MB
    private currentMemorySize = 0;

    // 获取缓存
    get(uri: string): CachedFileData | null {
        const cached = this.cache.get(uri);
        if (!cached) {
            return null;
        }

        // 更新访问时间
        cached.lastAccessed = Date.now();
        return cached;
    }

    // 设置缓存
    set(uri: string, data: CachedFileData): void {
        // 检查内存限制
        if (this.cache.size >= this.maxCacheSize) {
            this.evictLRU();
        }

        this.cache.set(uri, {
            ...data,
            lastAccessed: Date.now()
        });
        this.currentMemorySize += this.calculateSize(data);
    }

    // 失效缓存
    invalidate(uri: string): void {
        const cached = this.cache.get(uri);
        if (cached) {
            this.currentMemorySize -= this.calculateSize(cached);
            this.cache.delete(uri);
        }
    }

    // 清除所有缓存
    clear(): void {
        this.cache.clear();
        this.currentMemorySize = 0;
    }

    // 清除依赖链
    invalidateDependencies(uri: string): void {
        const cached = this.cache.get(uri);
        if (!cached) {
            return;
        }

        // 递归清除依赖此文件的文件
        const dependents = this.findDependents(uri);
        for (const dependent of dependents) {
            this.invalidate(dependent);
        }

        // 清除当前文件
        this.invalidate(uri);
    }

    // 查找依赖此文件的文件
    private findDependents(uri: string): string[] {
        const dependents: string[] = [];
        this.cache.forEach((cached, cachedUri) => {
            if (cached.dependencies.includes(uri)) {
                dependents.push(cachedUri);
            }
        });
        return dependents;
    }

    // LRU淘汰策略
    private evictLRU(): void {
        let oldestUri: string | null = null;
        let oldestTime = Date.now();

        this.cache.forEach((cached, uri) => {
            if (cached.lastAccessed < oldestTime) {
                oldestTime = cached.lastAccessed;
                oldestUri = uri;
            }
        });

        if (oldestUri) {
            this.invalidate(oldestUri);
        }
    }

    // 计算数据大小
    private calculateSize(data: CachedFileData): number {
        // 简化的大小计算
        return JSON.stringify(data).length;
    }

    // 获取缓存统计
    getStats(): { count: number; size: number; maxSize: number } {
        return {
            count: this.cache.size,
            size: this.currentMemorySize,
            maxSize: this.maxMemorySize
        };
    }

    // 检查是否应该启用缓存
    shouldEnableCache(fileSize: number): boolean {
        return fileSize < this.maxMemorySize * 0.1; // 文件小于可用内存的10%
    }
}

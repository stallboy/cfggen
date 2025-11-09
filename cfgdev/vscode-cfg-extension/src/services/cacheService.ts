import * as fs from 'fs';

export interface CachedFileData {
    // 解析后的符号
    symbols: Map<string, unknown>;
    symbolsByModule: Map<string, unknown[]>;
    symbolsByFile: Map<string, unknown[]>;

    // 依赖关系
    dependencies: string[];    // 依赖的其他文件路径

    // 元数据
    lastParsed: number;        // 最后解析时间戳
    fileVersion: number;       // 文件内容版本（基于文件修改时间或哈希）
    size: number;              // 缓存大小（字节）
}

export interface CacheManager {
    // 获取缓存
    get(uri: string): CachedFileData | null;

    // 设置缓存
    set(uri: string, data: CachedFileData): void;

    // 失效缓存
    invalidate(uri: string): void;

    // 清除所有缓存
    clear(): void;

    // 清除依赖链
    invalidateDependencies(uri: string): void;
}

export class CacheService implements CacheManager {
    private cache: Map<string, CachedFileData> = new Map();
    private maxCacheSize: number;
    private maxMemorySize: number;
    private currentMemorySize: number = 0;

    constructor() {
        this.maxCacheSize = 100;         // 最多缓存100个文件
        this.maxMemorySize = 50 * 1024 * 1024; // 50MB
    }

    /**
     * 获取缓存
     */
    get(uri: string): CachedFileData | null {
        const cached = this.cache.get(uri);
        if (!cached) {
            return null;
        }

        // 检查版本号是否匹配
        const currentVersion = this.getFileVersion(uri);
        if (currentVersion !== cached.fileVersion) {
            this.cache.delete(uri);
            this.currentMemorySize -= this.estimateSize(cached);
            return null;
        }

        return cached;
    }

    /**
     * 设置缓存
     */
    set(uri: string, data: CachedFileData): void {
        // 检查文件数量限制
        if (this.cache.size >= this.maxCacheSize) {
            this.evictLRU();
        }

        // 检查内存限制
        const dataSize = this.estimateSize(data);
        if (this.currentMemorySize + dataSize > this.maxMemorySize) {
            this.evictLRU();
        }

        this.cache.set(uri, data);
        this.currentMemorySize += dataSize;
    }

    /**
     * 失效缓存
     */
    invalidate(uri: string): void {
        const cached = this.cache.get(uri);
        if (cached) {
            this.cache.delete(uri);
            this.currentMemorySize -= this.estimateSize(cached);
        }
    }

    /**
     * 清除所有缓存
     */
    clear(): void {
        this.cache.clear();
        this.currentMemorySize = 0;
    }

    /**
     * 清除依赖链
     */
    invalidateDependencies(uri: string): void {
        const toInvalidate = new Set<string>();
        toInvalidate.add(uri);

        // 递归查找所有依赖
        for (const [cachedUri, cachedData] of this.cache) {
            if (cachedData.dependencies.includes(uri)) {
                toInvalidate.add(cachedUri);
            }
        }

        // 清除所有依赖
        for (const depUri of toInvalidate) {
            this.invalidate(depUri);
        }
    }

    /**
     * LRU淘汰策略
     */
    private evictLRU(): void {
        if (this.cache.size === 0) {
            return;
        }

        // 简单的LRU实现：删除第一个条目
        const oldest = this.cache.keys().next().value;
        if (oldest) {
            this.invalidate(oldest);
        }
    }

    /**
     * 获取文件版本号（基于修改时间）
     */
    private getFileVersion(uri: string): number {
        try {
            const stats = fs.statSync(uri);
            return stats.mtime.getTime();
        } catch (error) {
            console.error(`Error getting file version for ${uri}:`, error);
            return 0;
        }
    }

    /**
     * 估算缓存数据大小
     */
    private estimateSize(data: CachedFileData): number {
        // 简单的内存估算
        const symbolsSize = data.symbols.size * 100; // 估算每个符号100字节
        const moduleSize = data.symbolsByModule.size * 100;
        const fileSize = data.symbolsByFile.size * 100;
        const depsSize = data.dependencies.length * 100;

        return symbolsSize + moduleSize + fileSize + depsSize + 1000; // +1000字节基本开销
    }

    /**
     * 获取缓存统计信息
     */
    public getStats() {
        return {
            fileCount: this.cache.size,
            memorySize: this.currentMemorySize,
            maxFiles: this.maxCacheSize,
            maxMemory: this.maxMemorySize,
            utilization: {
                files: this.cache.size / this.maxCacheSize,
                memory: this.currentMemorySize / this.maxMemorySize
            }
        };
    }

    /**
     * 清理过期缓存
     */
    public cleanExpired(): void {
        const now = Date.now();
        const maxAge = 10 * 60 * 1000; // 10分钟

        for (const [uri, data] of this.cache) {
            if (now - data.lastParsed > maxAge) {
                this.invalidate(uri);
            }
        }
    }

    /**
     * 预热缓存（为已打开的文件）
     */
    public async warmup(openFiles: string[]): Promise<void> {
        for (const uri of openFiles) {
            const cached = this.get(uri);
            if (!cached) {
                // TODO: 解析并缓存文件
            }
        }
    }
}

import * as path from 'path';
import * as fs from 'fs';

export class ModuleResolverService {
    private rootPath: string;         // 根路径
    private modulePaths: Map<string, string>;  // 模块路径映射

    constructor(rootPath: string) {
        this.rootPath = rootPath;
        this.modulePaths = new Map();
        this.buildModuleIndex();
    }

    // 解析模块名
    parseModuleName(filePath: string): string {
        // 1. 获取目录名
        const dirName = path.basename(path.dirname(filePath));

        // 2. 截取第一个"."之前的部分
        const firstDot = dirName.indexOf('.');
        if (firstDot > 0) {
            return dirName.substring(0, firstDot);
        }

        // 3. 截取"_汉字"或纯汉字之前的部分
        const chineseMatch = dirName.match(/(.+?)(_[\u4e00-\u9fa5]+|[\u4e00-\u9fa5]+|$)/);
        return chineseMatch ? chineseMatch[1] : dirName;
    }

    // 查找模块路径
    resolveModule(moduleName: string): string | null {
        return this.modulePaths.get(moduleName) || null;
    }

    // 加载模块
    async loadModule(moduleName: string): Promise<string | null> {
        const modulePath = this.resolveModule(moduleName);
        if (!modulePath) {
            return null;
        }

        // 查找该目录下的所有.cfg文件
        try {
            const files = await fs.promises.readdir(modulePath);
            const cfgFiles = files.filter(f => f.endsWith('.cfg'));
            return cfgFiles.length > 0 ? path.join(modulePath, cfgFiles[0]) : null;
        } catch (error) {
            console.error(`Failed to load module ${moduleName}:`, error);
            return null;
        }
    }

    // 获取所有模块
    getAllModules(): string[] {
        return Array.from(this.modulePaths.keys());
    }

    // 构建模块索引
    private buildModuleIndex(): void {
        this.modulePaths.clear();

        if (!this.rootPath || !fs.existsSync(this.rootPath)) {
            return;
        }

        try {
            const entries = fs.readdirSync(this.rootPath, { withFileTypes: true });
            for (const entry of entries) {
                if (entry.isDirectory()) {
                    const moduleName = this.parseModuleName(path.join(this.rootPath, entry.name));
                    this.modulePaths.set(moduleName, path.join(this.rootPath, entry.name));
                }
            }
        } catch (error) {
            console.error('Failed to build module index:', error);
        }
    }

    // 获取模块的.cfg文件列表
    getModuleFiles(moduleName: string): string[] {
        const modulePath = this.resolveModule(moduleName);
        if (!modulePath) {
            return [];
        }

        try {
            const files = fs.readdirSync(modulePath);
            return files
                .filter(f => f.endsWith('.cfg'))
                .map(f => path.join(modulePath, f));
        } catch (error) {
            console.error(`Failed to get files for module ${moduleName}:`, error);
            return [];
        }
    }

    // 检查模块是否存在
    hasModule(moduleName: string): boolean {
        return this.modulePaths.has(moduleName);
    }

    // 获取根路径
    getRootPath(): string {
        return this.rootPath;
    }
}

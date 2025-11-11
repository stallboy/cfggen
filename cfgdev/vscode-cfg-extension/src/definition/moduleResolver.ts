import * as vscode from 'vscode';

/**
 * 模块解析器 - 处理包名和文件路径之间的映射
 */
export class ModuleResolver {
    /**
     * 查找根目录（包含config.cfg的目录）
     */
    async findRootDirectory(currentFile: string): Promise<string | null> {
        let currentDir = this.getDirname(currentFile);

        // 如果当前文件就是config.cfg，则当前目录就是根目录
        if (this.getBasename(currentFile) === 'config.cfg') {
            return currentDir;
        }

        // 向上搜索父目录，直到发现包含config.cfg文件的目录
        while (currentDir !== this.getDirname(currentDir)) {
            const configPath = this.joinPath(currentDir, 'config.cfg');
            if (await this.fileExists(configPath)) {
                return currentDir;
            }
            currentDir = this.getDirname(currentDir);
        }

        return null;
    }

    /**
     * 将文件路径转换为模块名
     * 规则：截取第一个"."之前的内容，再截取"_汉字"或汉字之前的部分
     */
    private resolvePathToModule(filePath: string): string | null {
        const fileName = this.getBasename(filePath, '.cfg');

        // 截取第一个"."之前的内容
        const firstDotIndex = fileName.indexOf('.');
        let moduleName = firstDotIndex >= 0 ? fileName.substring(0, firstDotIndex) : fileName;

        // 再截取"_汉字"或汉字之前的部分
        // 查找第一个汉字或"_汉字"的位置
        const chinesePattern = /[\u4e00-\u9fff]|_[\u4e00-\u9fff]/;
        const match = moduleName.match(chinesePattern);

        if (match && match.index !== undefined) {
            moduleName = moduleName.substring(0, match.index);
        }

        return moduleName || null;
    }

    /**
     * 查找模块对应的.cfg文件
     * 规则：pkg1.pkg2 -> pkg1_中文1/pkg2中文2/pkg2.cfg
     */
    async findModuleFile(moduleName: string, baseDir: string): Promise<string | null> {
        if (!moduleName) return null;

        // 将模块名按点分割
        const parts = moduleName.split('.');

        // 构建可能的目录结构
        let currentPath = baseDir;

        for (let i = 0; i < parts.length; i++) {
            const part = parts[i];

            // 中间部分，查找目录
            const possibleDir = await this.findDirectoryWithPrefix(currentPath, part);
            if (!possibleDir) {
                return null;
            }
            currentPath = this.joinPath(currentPath, possibleDir);

            // 如果是最后一部分，则查找.cfg文件
            if (i === parts.length - 1) {
                const cfgFile = this.joinPath(currentPath, `${part}.cfg`);
                if (await this.fileExists(cfgFile)) {
                    return cfgFile;
                }
            } 
        }

        return null;
    }

    /**
     * 查找以指定前缀开头的目录
     */
    private async findDirectoryWithPrefix(baseDir: string, prefix: string): Promise<string | null> {
        try {
            const entries = await vscode.workspace.fs.readDirectory(vscode.Uri.file(baseDir));
            for (const [name, type] of entries) {
                if (type === vscode.FileType.Directory && name.startsWith(prefix)) {
                    return name;
                }
            }
        } catch (error) {
            console.error(`Error reading directory ${baseDir}:`, error);
        }
        return null;
    }

    /**
     * 查找子目录
     */
    private async findSubdirectories(baseDir: string): Promise<string[]> {
        try {
            const entries = await vscode.workspace.fs.readDirectory(vscode.Uri.file(baseDir));
            return entries
                .filter(([name, type]) => type === vscode.FileType.Directory)
                .map(([name]) => name);
        } catch (error) {
            console.error(`Error reading directory ${baseDir}:`, error);
            return [];
        }
    }

    /**
     * 检查文件是否存在
     */
    private async fileExists(filePath: string): Promise<boolean> {
        try {
            await vscode.workspace.fs.stat(vscode.Uri.file(filePath));
            return true;
        } catch {
            return false;
        }
    }

    /**
     * 解析引用名称，分离模块名和表名
     */
    parseReference(refName: string): { modulePath?: string; typeOrTableName: string } {
        const parts = refName.split('.');

        if (parts.length === 1) {
            // 只有表名，没有模块名
            return { typeOrTableName: parts[0] };
        } else {
            // 有模块名和表名
            const tableName = parts[parts.length - 1];
            const moduleName = parts.slice(0, -1).join('.');
            return { modulePath: moduleName, typeOrTableName: tableName };
        }
    }

    // 路径处理辅助方法
    private getDirname(filePath: string): string {
        const parts = filePath.split(/[\\/]/);
        parts.pop();
        return parts.join('/');
    }

    private getBasename(filePath: string, ext?: string): string {
        const parts = filePath.split(/[\\/]/);
        let basename = parts[parts.length - 1];
        if (ext && basename.endsWith(ext)) {
            basename = basename.substring(0, basename.length - ext.length);
        }
        return basename;
    }

    private joinPath(...paths: string[]): string {
        return paths.join('/');
    }
}
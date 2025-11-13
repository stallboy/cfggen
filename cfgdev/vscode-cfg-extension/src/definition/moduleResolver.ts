import * as vscode from 'vscode';
import { PathUtils } from '../utils/pathUtils';
import { ErrorHandler } from '../utils/errorHandler';

/**
 * 模块解析器 - 处理包名和文件路径之间的映射
 */
export class ModuleResolver {
    
    /**
     * 解析引用名称，分离模块名和表名
     */
    static parseReference(refName: string): { modulePath?: string; typeOrTableName: string } {
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
    
    /**
     * 查找根目录（包含config.cfg的目录）
     */
    static async findRootDirectory(currentFile: string): Promise<string | null> {
        let currentDir = PathUtils.getDirname(currentFile);

        // 如果当前文件就是config.cfg，则当前目录就是根目录
        if (PathUtils.getBasename(currentFile) === 'config.cfg') {
            return currentDir;
        }

        // 向上搜索父目录，直到发现包含config.cfg文件的目录
        while (currentDir !== PathUtils.getDirname(currentDir)) {
            const configPath = PathUtils.joinPath(currentDir, 'config.cfg');
            if (await ModuleResolver.fileExists(configPath)) {
                return currentDir;
            }
            currentDir = PathUtils.getDirname(currentDir);
        }

        return null;
    }

    /**
     * 查找模块对应的.cfg文件
     * 规则：pkg1.pkg2 -> pkg1_中文1/pkg2中文2/pkg2.cfg
     */
    static async findModuleFile(moduleName: string, baseDir: string): Promise<string | null> {
        if (!moduleName) return null;

        // 将模块名按点分割
        const parts = moduleName.split('.');

        // 构建可能的目录结构
        let currentPath = baseDir;

        for (let i = 0; i < parts.length; i++) {
            const part = parts[i];

            // 中间部分，查找目录
            const possibleDir = await ModuleResolver.findDirectoryWithPrefix(currentPath, part);
            if (!possibleDir) {
                return null;
            }
            currentPath = PathUtils.joinPath(currentPath, possibleDir);

            // 如果是最后一部分，则查找.cfg文件
            if (i === parts.length - 1) {
                const cfgFile = PathUtils.joinPath(currentPath, `${part}.cfg`);
                if (await ModuleResolver.fileExists(cfgFile)) {
                    return cfgFile;
                }
            }
        }

        return null;
    }

    /**
     * 查找以指定前缀开头的目录
     */
    private static async findDirectoryWithPrefix(baseDir: string, prefix: string): Promise<string | null> {
        try {
            const entries = await vscode.workspace.fs.readDirectory(vscode.Uri.file(baseDir));
            for (const [name, type] of entries) {
                if (type === vscode.FileType.Directory) {
                    // 使用命名解析规则检查目录名
                    const resolvedName = ModuleResolver.resolvePathToModule(name);
                    if (resolvedName === prefix) {
                        return name;
                    }
                }
            }
        } catch (error) {
            ErrorHandler.logError('ModuleResolver.findDirectoryWithPrefix', `Error reading directory ${baseDir}: ${error}`);
        }
        return null;
    }

    /**
     * 检查文件是否存在
     */
    private static async fileExists(filePath: string): Promise<boolean> {
        try {
            await vscode.workspace.fs.stat(vscode.Uri.file(filePath));
            return true;
        } catch {
            return false;
        }
    }

    /**
     * 将文件路径转换为模块名
     * 规则：截取第一个"."之前的内容，再截取"_汉字"或"汉字"之前的部分
     */
    private static resolvePathToModule(filePath: string): string | null {
        const fileName = PathUtils.getBasename(filePath, '.cfg');

        // 步骤1：截取第一个"."之前的内容
        const firstDotIndex = fileName.indexOf('.');
        let moduleName = firstDotIndex >= 0 ? fileName.substring(0, firstDotIndex) : fileName;

        // 步骤2：截取"_汉字"或"汉字"之前的部分
        // 匹配中文字符（包括带下划线和不带下划线的情况）
        const chinesePattern = /[\u4e00-\u9fff]/;
        const chineseIndex = moduleName.search(chinesePattern);

        if (chineseIndex >= 0) {
            // 检查中文前面的字符是否为下划线，如果是则去掉下划线
            let endIndex = chineseIndex;
            if (chineseIndex > 0 && moduleName[chineseIndex - 1] === '_') {
                endIndex = chineseIndex - 1;
            }
            return moduleName.substring(0, endIndex);
        }

        return moduleName;
    }

}
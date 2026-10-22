import * as vscode from 'vscode';
import { PathUtils } from './pathUtils';
import { ErrorHandler } from './errorHandler';

/**
 * 模块解析器 - 处理包名和文件路径之间的映射
 */
export class ModuleResolver {

    /**
     * 查找根目录（包含config.cfg的目录）并设置模块名
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
    static async fileExists(filePath: string): Promise<boolean> {
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
    static resolvePathToModule(filePath: string): string {
        if (!/^[a-zA-Z]/.test(filePath)) return '';
        // 步骤1：截取第一个"."之前的内容
        const firstDotIndex = filePath.indexOf('.');
        const moduleName = firstDotIndex >= 0 ? filePath.substring(0, firstDotIndex) : filePath;

        // 步骤2：截取"_汉字"或"汉字"之前的部分
        // 与后端 UnicodeScript.HAN 一致，包含补充平面的汉字。
        const chinesePattern = /\p{Script=Han}/u;
        const chineseIndex = moduleName.search(chinesePattern);

        if (chineseIndex >= 0) {
            // 检查中文前面的字符是否为下划线，如果是则去掉下划线
            let endIndex = chineseIndex;
            if (chineseIndex > 0 && moduleName[chineseIndex - 1] === '_') {
                endIndex = chineseIndex - 1;
            }
            return moduleName.substring(0, endIndex).toLowerCase();
        }

        return moduleName.toLowerCase();
    }




    /**
     * 在根目录中查找.cfg文件
     */
    static async findCfgFilesInRoot(rootDir: string): Promise<string[]> {
        const cfgFiles: string[] = [];

        // 添加根目录的config.cfg
        const configPath = PathUtils.joinPath(rootDir, 'config.cfg');
        cfgFiles.push(configPath);


        // 递归查找子目录
        await this.findCfgFilesRecursive(rootDir, cfgFiles);
        return cfgFiles;
    }

    /**
     * 递归查找.cfg文件
     */
    private static async findCfgFilesRecursive(dir: string, cfgFiles: string[]): Promise<void> {
        const entries = await vscode.workspace.fs.readDirectory(vscode.Uri.file(dir));

        for (const [name, type] of entries) {
            const fullPath = PathUtils.joinPath(dir, name);

            if (type === vscode.FileType.Directory) {
                // 检查目录名是否符合模块名规则
                const modulePart = ModuleResolver.resolvePathToModule(name);
                if (modulePart.length > 0) {
                    // 查找目录中的[模块名].cfg文件
                    const cfgFile = PathUtils.joinPath(fullPath, `${modulePart}.cfg`);
                    if (await ModuleResolver.fileExists(cfgFile)) {
                        cfgFiles.push(cfgFile);
                    }

                    // 递归查找子目录
                    await this.findCfgFilesRecursive(fullPath, cfgFiles);
                }
            }
        }

    }


}

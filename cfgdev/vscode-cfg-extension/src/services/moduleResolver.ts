import * as vscode from 'vscode';
import * as path from 'path';

export class ModuleResolver {
    private rootPath: string | null = null;
    private modulePaths: Map<string, string> = new Map();
    private workspaceRoot: string | null = null;

    constructor() {
        this.workspaceRoot = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || null;
    }

    /**
     * 解析模块名
     * 标准结构: config/[module]/[module].cfg
     */
    parseModuleName(filePath: string): string {
        if (!this.workspaceRoot) {
            return 'default';
        }

        const relativePath = vscode.workspace.asRelativePath(filePath);
        const pathParts = relativePath.split('/');

        // 期望格式: config/module/module.cfg
        if (pathParts.length >= 2) {
            const moduleDir = pathParts[pathParts.length - 2];
            const moduleFile = pathParts[pathParts.length - 1];

            // 验证文件名与目录名匹配
            if (moduleFile === `${moduleDir}.cfg`) {
                // 截取第一个"."之前的部分
                const firstDot = moduleDir.indexOf('.');
                if (firstDot > 0) {
                    return moduleDir.substring(0, firstDot);
                }

                // 截取"_汉字"或纯汉字之前的部分
                const chineseMatch = moduleDir.match(/(.+?)(_[\u4e00-\u9fa5]+|[\u4e00-\u9fa5]+|$)/);
                return chineseMatch ? chineseMatch[1] : moduleDir;
            }
        }

        return 'default';
    }

    /**
     * 查找模块路径
     */
    resolveModule(moduleName: string): string | null {
        // 缓存中查找
        if (this.modulePaths.has(moduleName)) {
            return this.modulePaths.get(moduleName)!;
        }

        if (!this.workspaceRoot) {
            return null;
        }

        // 在工作区中搜索
        const pattern = new vscode.RelativePattern(
            this.workspaceRoot,
            `**/${moduleName}/${moduleName}.cfg`
        );

        // TODO: 异步搜索
        return null;
    }

    /**
     * 加载模块
     */
    async loadModule(moduleName: string): Promise<any> {
        const modulePath = this.resolveModule(moduleName);
        if (!modulePath) {
            return null;
        }

        try {
            const document = await vscode.workspace.openTextDocument(modulePath);
            return this.parseModule(document, moduleName);
        } catch (error) {
            console.error(`Error loading module ${moduleName}:`, error);
            return null;
        }
    }

    /**
     * 获取所有模块
     */
    getAllModules(): string[] {
        return Array.from(this.modulePaths.keys());
    }

    /**
     * 解析模块文档
     */
    private parseModule(document: vscode.TextDocument, moduleName: string): any {
        // TODO: 使用ANTLR4解析模块
        return {
            moduleName,
            filePath: document.fileName,
            definitions: []
        };
    }

    /**
     * 扫描工作区中的所有模块
     */
    async scanWorkspace(): Promise<void> {
        if (!this.workspaceRoot) {
            return;
        }

        const pattern = new vscode.RelativePattern(this.workspaceRoot, '**/*.cfg');
        const files = await vscode.workspace.findFiles(pattern, '**/node_modules/**', 1000);

        for (const file of files) {
            const moduleName = this.parseModuleName(file.fsPath);
            this.modulePaths.set(moduleName, file.fsPath);
        }
    }

    /**
     * 检查目录结构是否符合标准
     */
    isStandardDirectoryStructure(filePath: string): boolean {
        const relativePath = vscode.workspace.asRelativePath(filePath);
        const pathParts = relativePath.split('/');

        if (pathParts.length < 2) {
            return false;
        }

        const moduleDir = pathParts[pathParts.length - 2];
        const moduleFile = pathParts[pathParts.length - 1];

        return moduleFile === `${moduleDir}.cfg`;
    }

    /**
     * 解析引用中的模块名
     */
    parseReferenceModule(reference: string): string | null {
        // 格式: module.table 或 table
        const parts = reference.split('.');
        if (parts.length >= 2) {
            return parts[0];
        }
        return null;
    }

    /**
     * 获取模块中的所有定义
     */
    async getModuleDefinitions(moduleName: string): Promise<any[]> {
        const module = await this.loadModule(moduleName);
        return module ? module.definitions : [];
    }
}

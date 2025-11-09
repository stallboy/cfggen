import * as vscode from 'vscode';
import { ConfigFile, Definition } from '../models';

export class FileIndexService {
    private files: Map<string, ConfigFile> = new Map();
    private workspaceRoot: string | null = null;

    constructor() {
        this.workspaceRoot = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || null;
    }

    /**
     * 为工作区中的所有.cfg文件建立索引
     */
    public async indexWorkspace(): Promise<void> {
        if (!this.workspaceRoot) {
            return;
        }

        const pattern = new vscode.RelativePattern(this.workspaceRoot, '**/*.cfg');
        const files = await vscode.workspace.findFiles(pattern, '**/node_modules/**', 1000);

        for (const file of files) {
            await this.indexFile(file.fsPath);
        }
    }

    /**
     * 为单个文件建立索引
     */
    public async indexFile(filePath: string): Promise<ConfigFile | null> {
        try {
            const document = await vscode.workspace.openTextDocument(filePath);
            const configFile = await this.parseFile(document);

            if (configFile) {
                this.files.set(filePath, configFile);
            }

            return configFile;
        } catch (error) {
            console.error(`Error indexing file ${filePath}:`, error);
            return null;
        }
    }

    /**
     * 解析文件内容
     */
    private async parseFile(document: vscode.TextDocument): Promise<ConfigFile | null> {
        const filePath = document.fileName;
        const moduleName = this.parseModuleName(filePath);
        const lastModified = Date.now();

        // TODO: 使用ANTLR4解析文件
        // const ast = this.parseWithANTLR(document.getText());
        // const definitions = this.extractDefinitions(ast);

        return {
            path: filePath,
            moduleName,
            definitions: [], // TODO: 填充定义
            symbols: {
                add: (_definition: Definition): void => { },
                find: (_name: string, _module?: string): Definition | null => null,
                findAll: (_type?: string): Definition[] => [],
                findInModule: (_module: string, _name: string): Definition | null => null,
                getReferences: (_target: Definition): import('../models').Reference[] => []
            },
            errors: [], // TODO: 填充错误
            lastModified
        };
    }

    /**
     * 从文件路径解析模块名
     * 标准结构: config/[module]/[module].cfg
     */
    private parseModuleName(filePath: string): string {
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
     * 获取所有已索引的文件
     */
    public getAllFiles(): ConfigFile[] {
        return Array.from(this.files.values());
    }

    /**
     * 根据路径获取文件
     */
    public getFile(filePath: string): ConfigFile | null {
        return this.files.get(filePath) || null;
    }

    /**
     * 根据模块名获取文件
     */
    public getFilesByModule(moduleName: string): ConfigFile[] {
        return this.getAllFiles().filter(file => file.moduleName === moduleName);
    }

    /**
     * 刷新文件索引
     */
    public async refreshFile(filePath: string): Promise<void> {
        await this.indexFile(filePath);
    }

    /**
     * 移除文件索引
     */
    public removeFile(filePath: string): void {
        this.files.delete(filePath);
    }

    /**
     * 清空所有索引
     */
    public clear(): void {
        this.files.clear();
    }
}

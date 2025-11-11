import * as vscode from 'vscode';
import { FileCache } from './fileCache';
import { ModuleResolver } from './moduleResolver';
import { FileDefinitionAndRef, PositionRef, ResolvedLocation } from './types';

/**
 * CFG定义提供者 - 实现跳转功能
 */
export class CfgDefinitionProvider implements vscode.DefinitionProvider {
    private fileCache: FileCache;
    private moduleResolver: ModuleResolver;

    constructor() {
        this.fileCache = new FileCache();
        this.moduleResolver = new ModuleResolver();
    }

    /**
     * 提供定义位置
     */
    async provideDefinition(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): Promise<vscode.Location[] | undefined> {
        try {
            // 确保文件已解析
            const fileDef = await this.fileCache.getOrParseDefinitionAndRef(document);

            // 获取指定位置的引用信息
            const positionRef = fileDef.getRefAtPosition(position);
            if (!positionRef) {
                return undefined;
            }

            // 查找定义位置
            const resolvedLocation = await this.findDefinition(positionRef, fileDef, document.uri.fsPath);
            if (!resolvedLocation) {
                return undefined;
            }

            return [new vscode.Location(
                vscode.Uri.file(resolvedLocation.filePath),
                resolvedLocation.range
            )];
        } catch (error) {
            console.error('Error in provideDefinition:', error);
            return undefined;
        }
    }


    /**
     * 查找定义位置
     */
    private async findDefinition(positionRef: PositionRef, currentFileDef: FileDefinitionAndRef, currentFilePath: string): Promise<ResolvedLocation | undefined> {

        const { isRefType, name: typeName, inInterfaceName } = positionRef;
        const { modulePath, typeOrTableName } = this.moduleResolver.parseReference(typeName);

        if (positionRef.isRefType) {
            // 0. 如果当前在interface内，先在当前interface内查找
            if (inInterfaceName) {
                const interfaceDefs = currentFileDef.getDefinitionInInterface(inInterfaceName);
                const range = interfaceDefs?.get(typeOrTableName);
                if (range) {
                    return { filePath: currentFilePath, range };
                }
            }
        }

        // 1. 当前文件内查找
        let range = currentFileDef.getDefinition(typeOrTableName);
        if (range) {
            return { filePath: currentFilePath, range };
        }

        // 2. 模块内查找
        if (modulePath) {
            const baseDir = this.getDirname(currentFilePath);
            const result = await this.findDefinitionInDir(modulePath, typeOrTableName, baseDir);
            if (result) {
                return result;
            }
        }

        // 3. 根目录查找
        const rootDir = await this.moduleResolver.findRootDirectory(currentFilePath);
        if (rootDir) {
            if (modulePath) {
                const result = await this.findDefinitionInDir(modulePath, typeOrTableName, rootDir);
                if (result) {
                    return result;
                }
            } else {
                const configFile = this.joinPath(rootDir, 'config.cfg');
                const configFileDef = await this.ensureFileParsed(configFile);
                if (configFileDef) {
                    const range = currentFileDef.getDefinition(typeOrTableName);
                    if (range) {
                        return { filePath: configFile, range };
                    }
                }
            }
        }
    }

    /**
     * 在模块里，通过一层层目录查找
     */
    private async findDefinitionInDir(
        modulePath: string,
        typeOrTableName: string,
        baseDir: string
    ): Promise<ResolvedLocation | undefined> {
        const moduleFile = await this.moduleResolver.findModuleFile(modulePath, baseDir);
        if (moduleFile) {
            // 确保模块文件已解析
            const moduleFileDef = await this.ensureFileParsed(moduleFile);
            if (moduleFileDef) {
                const range = moduleFileDef.getDefinition(typeOrTableName);
                if (range) {
                    return { filePath: moduleFile, range };
                }
            }
        }
    }

    /**
     * 确保文件已解析
     */
    private async ensureFileParsed(filePath: string): Promise<FileDefinitionAndRef | undefined> {
        try {
            const document = await vscode.workspace.openTextDocument(vscode.Uri.file(filePath));
            return await this.fileCache.getOrParseDefinitionAndRef(document);
        } catch (error) {
            console.error(`Error parsing module file ${filePath}:`, error);
        }
    }


    // 路径处理辅助方法
    private getDirname(filePath: string): string {
        const parts = filePath.split(/[\\/]/);
        parts.pop();
        return parts.join('/');
    }

    private joinPath(...paths: string[]): string {
        return paths.join('/');
    }

    /**
     * 清理资源
     */
    dispose(): void {
        this.fileCache.clearAll();
    }

}
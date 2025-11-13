import * as vscode from 'vscode';
import { FileCache } from './fileCache';
import { ModuleResolver } from '../utils/moduleResolver';
import { FileDefinitionAndRef, PositionRef, ResolvedLocation } from './fileDefinitionAndRef';
import { PathUtils } from '../utils/pathUtils';
import { ErrorHandler } from '../utils/errorHandler';

/**
 * CFG定义提供者 - 实现跳转功能
 */
export class CfgDefinitionProvider implements vscode.DefinitionProvider {
    constructor() {
        // 不再需要实例化 ModuleResolver，所有方法都是静态的
    }

    /**
     * 提供定义位置
     */
    async provideDefinition(
        document: vscode.TextDocument,
        position: vscode.Position,
        _token: vscode.CancellationToken
    ): Promise<vscode.Location[] | undefined> {
        try {
            // 确保文件已解析
            const fileDef = await FileCache.getInstance().getOrParseDefinitionAndRef(document);

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
            ErrorHandler.logError('CfgDefinitionProvider.provideDefinition', error);
            return undefined;
        }
    }


    /**
     * 查找定义位置
     */
    private async findDefinition(positionRef: PositionRef, currentFileDef: FileDefinitionAndRef, currentFilePath: string): Promise<ResolvedLocation | undefined> {

        const { isRefType, name: typeName, inInterfaceName } = positionRef;
        const { modulePath, typeOrTableName } = ModuleResolver.parseReference(typeName);

        if (isRefType) {
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
        const tRange = currentFileDef.getDefinition(typeOrTableName);
        if (tRange) {
            return { filePath: currentFilePath, range: tRange.range };
        }

        // 2. 模块内查找
        if (modulePath) {
            const baseDir = PathUtils.getDirname(currentFilePath);
            const result = await this.findDefinitionInDir(modulePath, typeOrTableName, baseDir);
            if (result) {
                return result;
            }
        }

        // 3. 根目录查找
        const rootDir = await ModuleResolver.findRootDirectory(currentFilePath);
        if (rootDir) {
            if (modulePath) {
                const result = await this.findDefinitionInDir(modulePath, typeOrTableName, rootDir);
                if (result) {
                    return result;
                }
            } else {
                const configFile = PathUtils.joinPath(rootDir, 'config.cfg');
                const configFileDef = await FileCache.getInstance().getOrParse(configFile);
                if (configFileDef) {
                    const tRange = configFileDef.getDefinition(typeOrTableName);
                    if (tRange) {
                        return { filePath: configFile, range: tRange.range };
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
        const moduleFile = await ModuleResolver.findModuleFile(modulePath, baseDir);
        if (moduleFile) {
            // 确保模块文件已解析
            const moduleFileDef = await FileCache.getInstance().getOrParse(moduleFile);
            if (moduleFileDef) {
                const tRange = moduleFileDef.getDefinition(typeOrTableName);
                if (tRange) {
                    return { filePath: moduleFile, range: tRange.range };
                }
            }
        }
    }

}
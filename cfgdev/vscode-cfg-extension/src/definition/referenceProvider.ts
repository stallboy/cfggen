import * as vscode from 'vscode';
import { FileCache } from './fileCache';
import { FileDefinitionAndRef, TName, ImplName, Ref } from './fileDefinitionAndRef';
import { ModuleResolver } from './moduleResolver';
import { ErrorHandler } from '../utils/errorHandler';

/**
 * 引用提供者
 */
export class CfgReferenceProvider implements vscode.ReferenceProvider {

    /**
     * 提供引用位置
     */
    async provideReferences(
        document: vscode.TextDocument,
        position: vscode.Position,
        _context: vscode.ReferenceContext,
        _token: vscode.CancellationToken
    ): Promise<vscode.Location[]> {
        const refs: vscode.Location[] = [];

        try {
            // 1. 获取当前文件的定义和引用信息
            const curDef = await FileCache.getInstance().getOrParseDefinitionAndRef(document);
            const curFilePath = document.uri.fsPath;
            // 2. 从lineToDefinitions查找当前位置的定义
            const definition = curDef.getDefinitionAtPosition(position);
            if (!definition?.inInterfaceName) {
                await this.findReference(definition, curDef, curFilePath, refs);
            } else {
                // 3. 从lineToDefinitionInInterfaces查找当前位置的定义
                await this.findReferencesInInterface(definition, curDef, curFilePath, refs);
            }

        } catch (error) {
            ErrorHandler.logError('CfgReferenceProvider.provideReferences', error);
        }

        return refs;
    }


}
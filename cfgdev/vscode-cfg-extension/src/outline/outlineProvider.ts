import * as vscode from 'vscode';
import { FileCache } from '../definition/fileCache';
import { DefinitionType } from '../definition/fileDefinitionAndRef';
import { ErrorHandler } from '../utils/errorHandler';

/**
 * CFG文档符号提供者 - 实现outline功能
 */
export class CfgDocumentSymbolProvider implements vscode.DocumentSymbolProvider {

    /**
     * 提供文档符号
     */
    async provideDocumentSymbols(
        document: vscode.TextDocument,
        _token: vscode.CancellationToken
    ): Promise<vscode.DocumentSymbol[]> {
        try {
            // 获取文件的定义信息
            const fileDef = await FileCache.getInstance().getOrParseDefinitionAndRef(document);

            const symbols: vscode.DocumentSymbol[] = [];

            // 处理全局定义 (struct, interface, table)
            for (const [name, tRange] of fileDef.definitions.entries()) {
                // 使用TRange中的类型信息
                const kind = this.getSymbolKindFromType(tRange.type);
                const symbol = new vscode.DocumentSymbol(
                    name,
                    this.getSymbolDetailFromType(tRange.type),
                    kind,
                    tRange.range,
                    tRange.range
                );
                symbols.push(symbol);

                // 如果是interface，添加其内部的定义
                if (fileDef.definitionsInInterface.has(name)) {
                    const interfaceDefs = fileDef.definitionsInInterface.get(name);
                    if (interfaceDefs) {
                        for (const [childName, childRange] of interfaceDefs.entries()) {
                            // interface内部只能是struct
                            const childSymbol = new vscode.DocumentSymbol(
                                childName,
                                'struct',
                                vscode.SymbolKind.Struct,
                                childRange,
                                childRange
                            );
                            symbol.children.push(childSymbol);
                        }
                    }
                }
            }

            return symbols;
        } catch (error) {
            ErrorHandler.logError('CfgDocumentSymbolProvider.provideDocumentSymbols', error);
            return [];
        }
    }

    /**
     * 根据类型获取符号类型
     */
    private getSymbolKindFromType(type: DefinitionType): vscode.SymbolKind {
        switch (type) {
            case 'interface':
                return vscode.SymbolKind.Interface;
            case 'struct':
                return vscode.SymbolKind.Struct;
            case 'table':
                return vscode.SymbolKind.Class; // 使用Class表示table
            default:
                return vscode.SymbolKind.Class;
        }
    }

    /**
     * 根据类型获取符号详细描述
     */
    private getSymbolDetailFromType(type: DefinitionType): string {
        return type;
    }
}
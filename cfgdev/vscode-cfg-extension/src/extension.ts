
import * as vscode from 'vscode';
import { SemanticTokensProvider } from './highlight/semanticTokensProvider';
import { CfgDefinitionProvider } from './definition/definitionProvider';
import { CfgDocumentSymbolProvider } from './outline/outlineProvider';
import { CfgReferenceProvider } from './definition/referenceProvider';
import { FileCache } from './definition/fileCache';

export function activate(context: vscode.ExtensionContext) {
    // 1. 注册语义标记提供者（两层高亮中的第二层）
    const semanticTokensProvider = new SemanticTokensProvider();
    const legend = semanticTokensProvider.getLegend();
    context.subscriptions.push(
        vscode.languages.registerDocumentSemanticTokensProvider(
            { language: 'cfg' },
            semanticTokensProvider,
            legend
        )
    );

    // 2. 注册定义提供者用于跳转到定义
    context.subscriptions.push(
        vscode.languages.registerDefinitionProvider(
            { language: 'cfg' },
            new CfgDefinitionProvider()
        )
    );

    // 3. 注册文档符号提供者用于大纲视图
    context.subscriptions.push(
        vscode.languages.registerDocumentSymbolProvider(
            { language: 'cfg' },
            new CfgDocumentSymbolProvider()
        )
    );

    // 4. 注册引用提供者用于查找所有引用
    context.subscriptions.push(
        vscode.languages.registerReferenceProvider(
            { language: 'cfg' },
            new CfgReferenceProvider()
        )
    );
}

export function deactivate() {
    // 清理文件缓存
    FileCache.getInstance().clearAll();
}

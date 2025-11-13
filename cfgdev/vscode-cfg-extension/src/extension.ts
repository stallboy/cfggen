
import * as vscode from 'vscode';
import { SemanticTokensProvider } from './highlight/semanticTokensProvider';
import { CfgDefinitionProvider } from './definition/definitionProvider';
import { FileCache } from './definition/fileCache';

export function activate(context: vscode.ExtensionContext) {
    // 1. Register semantic tokens provider (Layer 2 of two-layer highlighting)
    const semanticTokensProvider = new SemanticTokensProvider();
    const legend = semanticTokensProvider.getLegend();
    context.subscriptions.push(
        vscode.languages.registerDocumentSemanticTokensProvider(
            { language: 'cfg' },
            semanticTokensProvider,
            legend
        )
    );

    // 2. Register definition provider for jump-to-definition
    context.subscriptions.push(
        vscode.languages.registerDefinitionProvider(
            { language: 'cfg' },
            new CfgDefinitionProvider()
        )
    );
}

export function deactivate() {
    // 清理文件缓存
    FileCache.getInstance().clearAll();
}

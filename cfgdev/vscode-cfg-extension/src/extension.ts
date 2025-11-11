
import * as vscode from 'vscode';
import { SemanticTokensProvider } from './highlight/semanticTokensProvider';
import { CfgDefinitionProvider } from './definition/definitionProvider';

let definitionProvider: CfgDefinitionProvider | undefined;

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
    definitionProvider = new CfgDefinitionProvider();
    context.subscriptions.push(
        vscode.languages.registerDefinitionProvider(
            { language: 'cfg' },
            definitionProvider
        )
    );
}

export function deactivate() {
    // 清理文件缓存
    if (definitionProvider) {
        definitionProvider.dispose();
    }
}

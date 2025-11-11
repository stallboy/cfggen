
import * as vscode from 'vscode';
import { SemanticTokensProvider } from './highlight/semanticTokensProvider';
import { CfgDefinitionProvider } from './definition/definitionProvider';

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
    const definitionProvider = new CfgDefinitionProvider();
    context.subscriptions.push(
        vscode.languages.registerDefinitionProvider(
            { language: 'cfg' },
            definitionProvider
        )
    );
}

export function deactivate() {
}

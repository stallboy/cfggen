
import * as vscode from 'vscode';
import { SemanticTokensProvider } from './highlight/semanticTokensProvider';
import { CfgDefinitionProvider } from './definition/definitionProvider';
import { CfgDocumentSymbolProvider } from './outline/outlineProvider';
import { CfgReferenceProvider } from './definition/referenceProvider';
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

    // 3. Register document symbol provider for outline
    context.subscriptions.push(
        vscode.languages.registerDocumentSymbolProvider(
            { language: 'cfg' },
            new CfgDocumentSymbolProvider()
        )
    );

    // 4. Register reference provider for find all references
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

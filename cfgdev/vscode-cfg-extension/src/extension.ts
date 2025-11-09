/**
 * CFG Language Support Extension
 * Activates the extension and registers all language features
 * Implements two-layer syntax highlighting: TextMate + Semantic Tokens
 *
 * TextMate layer: Basic syntax highlighting (keywords, strings, etc.)
 * - Configured in package.json via tokenColors
 * - Uses fixed colors that work with most themes
 *
 * Semantic layer: Intelligent highlighting using AST analysis (struct names, type references, etc.)
 * - Configured via semanticTokenTypes in package.json
 * - Uses VSCode's built-in themes automatically
 */

import * as vscode from 'vscode';
import { SemanticTokensProvider } from './providers/semanticTokensProvider';

// This method is called when your extension is activated
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

    // 2. Register completion provider
    context.subscriptions.push(
        vscode.languages.registerCompletionItemProvider(
            { language: 'cfg' },
            {
                provideCompletionItems: () => []
            }
        )
    );

    // 3. Register definition provider
    context.subscriptions.push(
        vscode.languages.registerDefinitionProvider(
            { language: 'cfg' },
            {
                provideDefinition: () => null
            }
        )
    );

    // 4. Register hover provider
    context.subscriptions.push(
        vscode.languages.registerHoverProvider(
            { language: 'cfg' },
            {
                provideHover: () => null
            }
        )
    );

    // 5. Register reference provider
    context.subscriptions.push(
        vscode.languages.registerReferenceProvider(
            { language: 'cfg' },
            {
                provideReferences: () => []
            }
        )
    );
}

// This method is called when your extension is deactivated
export function deactivate() {
    // Cleanup if needed
}

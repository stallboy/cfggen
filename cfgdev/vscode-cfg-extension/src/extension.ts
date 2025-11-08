/**
 * CFG Language Support Extension
 * Activates the extension and registers all language features
 * Implements two-layer syntax highlighting: TextMate + Semantic Tokens
 */

import * as vscode from 'vscode';
import { ThemeManager } from './providers/themeManager';
import { ThemeService } from './services/themeService';
import { SemanticTokensProvider } from './providers/semanticTokensProvider';

// This method is called when your extension is activated
export function activate(context: vscode.ExtensionContext) {
    console.log('CFG Language Support extension is now active');

    // 1. Initialize theme system (critical for two-layer highlighting)
    const themeManager = ThemeManager.getInstance();
    themeManager.initialize();

    const themeService = themeManager.getThemeService();

    // 2. Apply current theme
    const currentTheme = themeManager.getCurrentTheme();
    themeService.applyTheme(currentTheme);

    // 3. Listen for theme changes and refresh providers
    themeManager.subscribe((theme) => {
        console.log(`Theme changed to: ${theme}`);
        themeService.applyTheme(theme);
        refreshAllProviders();
    });

    // 4. Register semantic tokens provider (Layer 2 of two-layer highlighting)
    const semanticTokensProvider = new SemanticTokensProvider(themeService);
    context.subscriptions.push(
        vscode.languages.registerDocumentSemanticTokensProvider(
            { language: 'cfg' },
            semanticTokensProvider,
            semanticTokensProvider.getLegend()
        )
    );

    // 5. Register completion provider (User Story 3 - Autocompletion)
    // TODO: Implement in Phase 5
    context.subscriptions.push(
        vscode.languages.registerCompletionItemProvider(
            { language: 'cfg' },
            {
                provideCompletionItems: () => []
            }
        )
    );

    // 6. Register definition provider (User Story 2 - Go-to-Definition)
    // TODO: Implement in Phase 4
    context.subscriptions.push(
        vscode.languages.registerDefinitionProvider(
            { language: 'cfg' },
            {
                provideDefinition: () => null
            }
        )
    );

    // 7. Register hover provider (Polish Phase)
    // TODO: Implement in Phase 6
    context.subscriptions.push(
        vscode.languages.registerHoverProvider(
            { language: 'cfg' },
            {
                provideHover: () => null
            }
        )
    );

    // 8. Register reference provider (Polish Phase)
    // TODO: Implement in Phase 6
    context.subscriptions.push(
        vscode.languages.registerReferenceProvider(
            { language: 'cfg' },
            {
                provideReferences: () => []
            }
        )
    );

    console.log('CFG extension activated successfully with two-layer syntax highlighting');
    console.log('Language ID: cfg');
    console.log('TextMate grammar: source.cfg');
    console.log('Semantic tokens: 8 token types');
    console.log(`Current theme: ${currentTheme}`);
}

// This method is called when your extension is deactivated
export function deactivate() {
    console.log('CFG Language Support extension is now deactivated');
}

/**
 * Force refresh all providers
 * Called when theme changes
 */
function refreshAllProviders(): void {
    // Force VSCode to refresh semantic tokens
    vscode.workspace.textDocuments.forEach(doc => {
        if (doc.languageId === 'cfg') {
            vscode.commands.executeCommand(
                'vscode.refreshSemanticTokens',
                doc.uri
            );
        }
    });
}

/**
 * Semantic Tokens Provider
 * Provides semantic highlighting for CFG files using ANTLR4 parser
 * Works with ThemeService to apply theme colors
 */

import * as vscode from 'vscode';
import { ANTLRInputStream, CommonTokenStream } from 'antlr4ts';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { CfgHighlightingListener } from './semanticTokensProvider.highlightListener';
import { ThemeService, ThemeName } from '../services/themeService';

export class SemanticTokensProvider implements vscode.DocumentSemanticTokensProvider {
    private themeService: ThemeService;
    private legend: vscode.SemanticTokensLegend;

    // Semantic token types as per the Constitution
    public static readonly TOKEN_TYPES = [
        'structureDefinition',  // 0: struct/interface/table names
        'typeIdentifier',       // 1: custom types (non-basic)
        'fieldName',            // 2: field names
        'foreignKey',           // 3: foreign key references
        'comment',              // 4: comments
        'metadata',             // 5: metadata keywords
        'primaryKey',           // 6: primary key fields
        'uniqueKey'             // 7: unique key fields
    ];

    constructor(themeService: ThemeService) {
        this.themeService = themeService;
        this.legend = new vscode.SemanticTokensLegend(
            SemanticTokensProvider.TOKEN_TYPES,
            []  // modifiers
        );
    }

    /**
     * Get the semantic tokens legend
     */
    public getLegend(): vscode.SemanticTokensLegend {
        return this.legend;
    }

    /**
     * Provide semantic tokens for a document
     */
    public provideDocumentSemanticTokens(
        document: vscode.TextDocument,
        token: vscode.CancellationToken
    ): vscode.ProviderResult<vscode.SemanticTokens> {
        try {
            // Create ANTLR4 input stream from document
            const inputStream = new ANTLRInputStream(document.getText());
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);

            // Parse the document
            const parser = new CfgParser(tokenStream);
            const parseTree = parser.schema();

            // Create highlighting listener with theme service
            const theme = this.themeService.getThemeColors();
            const builder = new vscode.SemanticTokensBuilder(this.legend);
            const listener = new CfgHighlightingListener(
                builder,
                document,
                theme
            );

            // Walk the parse tree to collect semantic tokens
            listener.walk(parseTree);

            return builder.build();
        } catch (error) {
            console.error('Error providing semantic tokens:', error);
            return new vscode.SemanticTokens(new Uint32Array(0), undefined);
        }
    }

    /**
     * Refresh semantic tokens for all CFG documents
     */
    public refreshAll(): void {
        vscode.workspace.textDocuments.forEach(doc => {
            if (doc.languageId === 'cfg') {
                vscode.commands.executeCommand(
                    'vscode.refreshSemanticTokens',
                    doc.uri
                );
            }
        });
    }
}

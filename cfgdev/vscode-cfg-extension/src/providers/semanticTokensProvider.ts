/**
 * Semantic Tokens Provider
 * Provides semantic highlighting for CFG files using ANTLR4 parser
 * Semantic tokens use VSCode's built-in themes automatically
 */

import * as vscode from 'vscode';
import { CommonTokenStream } from 'antlr4ng';
import { CharStream } from 'antlr4ng';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { CfgHighlightingListener } from './semanticTokensProvider.highlightListener';

export class SemanticTokensProvider implements vscode.DocumentSemanticTokensProvider {
    private legend: vscode.SemanticTokensLegend;

    // Semantic token types
    public static readonly TOKEN_TYPES = [
        'structureDefinition',  // 0: struct/interface/table names
        'typeIdentifier',       // 1: custom types (non-basic)
        'foreignKey',           // 2: foreign key references
        'comment',              // 3: comments
        'metadata',             // 4: metadata keywords
        'primaryKey',           // 5: primary key fields
        'uniqueKey'             // 6: unique key fields
    ];

    constructor() {
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
        _token: vscode.CancellationToken
    ): vscode.ProviderResult<vscode.SemanticTokens> {
        try {
            // Create ANTLR4 input stream from document
            const inputStream = CharStream.fromString(document.getText());
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);

            // Parse the document
            const parser = new CfgParser(tokenStream);
            const parseTree = parser.schema();

            // Create builder with legend
            const builder = new vscode.SemanticTokensBuilder(this.legend);

            // Create highlighting listener
            // Semantic tokens use VSCode's built-in themes automatically
            const listener = new CfgHighlightingListener(
                builder,
                document
            );

            // Walk the parse tree to collect semantic tokens
            listener.walk(parseTree);

            // Build the semantic tokens
            const tokens = builder.build();

            return tokens;
        } catch (error) {
            console.error('[SemanticTokens] Error providing semantic tokens:', error);
            console.error('[SemanticTokens] Stack:', error instanceof Error ? error.stack : '');
            return new vscode.SemanticTokens(new Uint32Array(0), undefined);
        }
    }
}

/**
 * Semantic Tokens Provider
 * Provides semantic highlighting for CFG files using ANTLR4 parser
 * Works with ThemeService to apply theme colors
 */

import * as vscode from 'vscode';
import { CommonTokenStream } from 'antlr4ts';
import { CharStreams } from 'antlr4ts';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { CfgHighlightingListener } from './semanticTokensProvider.highlightListener';
import { ThemeService } from '../services/themeService';

export class SemanticTokensProvider implements vscode.DocumentSemanticTokensProvider {
    private themeService: ThemeService;
    private legend: vscode.SemanticTokensLegend;

    // Semantic token types as per the Constitution
    public static readonly TOKEN_TYPES = [
        'structureDefinition',  // 0: struct/interface/table names
        'typeIdentifier',       // 1: custom types (non-basic)
        'foreignKey',           // 2: foreign key references
        'comment',              // 3: comments
        'metadata',             // 4: metadata keywords
        'primaryKey',           // 5: primary key fields
        'uniqueKey'             // 6: unique key fields
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
        _token: vscode.CancellationToken
    ): vscode.ProviderResult<vscode.SemanticTokens> {
        try {
            // Create ANTLR4 input stream from document using CharStreams (replaces deprecated ANTLRInputStream)
            const inputStream = CharStreams.fromString(document.getText());
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

            // Build the semantic tokens
            const tokens = builder.build();

            // Debug: Print semantic tokens for debugging
            this.debugPrintTokens(document, tokens);

            return tokens;
        } catch (error) {
            console.error('Error providing semantic tokens:', error);
            return new vscode.SemanticTokens(new Uint32Array(0), undefined);
        }
    }

    /**
     * Debug: Print semantic tokens for debugging purposes
     * Note: VSCode uses delta encoding for semantic tokens.
     * data format: [deltaLine, deltaStartChar, length, tokenType, tokenModifiers]
     * We need to decode these deltas to get absolute positions.
     */
    private debugPrintTokens(document: vscode.TextDocument, tokens: vscode.SemanticTokens | null): void {
        if (!tokens) {
            console.log('[SemanticTokens] No tokens generated');
            return;
        }

        console.log('[SemanticTokens] Document:', document.fileName);
        console.log('[SemanticTokens] Token count:', tokens.data.length / 5);

        // Print first 10 tokens for debugging
        const maxTokens = Math.min(20, tokens.data.length / 5);

        // Also print the raw data for debugging
        console.log('[SemanticTokens] Raw data (first 50 values):', Array.from(tokens.data.slice(0, 50)));

        // Decode delta encoding to get absolute positions
        let currentLine = 0;
        let currentChar = 0;

        for (let i = 0; i < maxTokens; i++) {
            const offset = i * 5;
            const deltaLine = tokens.data[offset];
            const deltaStartChar = tokens.data[offset + 1];
            const length = tokens.data[offset + 2];
            const tokenType = tokens.data[offset + 3];
            const tokenModifiers = tokens.data[offset + 4];

            // Decode delta to absolute position
            const absLine = currentLine + deltaLine;
            const absChar = deltaLine === 0 ? currentChar + deltaStartChar : deltaStartChar;

            // Get token type name
            const tokenTypeName = tokenType < SemanticTokensProvider.TOKEN_TYPES.length
                ? SemanticTokensProvider.TOKEN_TYPES[tokenType]
                : `Unknown(${tokenType})`;

            // Get the actual token text from the document
            const startPos = new vscode.Position(absLine, absChar);
            const endPos = new vscode.Position(absLine, absChar + length);
            const tokenText = document.getText(new vscode.Range(startPos, endPos));

            console.log(`[SemanticTokens] Token ${i + 1}:`, {
                absLine,
                absChar,
                length,
                text: tokenText,
                tokenType: tokenTypeName,
                tokenModifiers
            });

            // Update current position for next token
            currentLine = absLine;
            currentChar = absChar;
        }

        if (tokens.data.length / 5 > maxTokens) {
            console.log(`[SemanticTokens] ... and ${tokens.data.length / 5 - maxTokens} more tokens`);
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

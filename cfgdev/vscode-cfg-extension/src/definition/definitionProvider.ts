/**
 * Definition Provider
 * Provides jump-to-definition functionality for CFG files
 * Supports foreign key references, type references, and field references
 */

import * as vscode from 'vscode';
import { CommonTokenStream } from 'antlr4ng';
import { CharStream } from 'antlr4ng';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { LocationVisitor } from './locationVisitor';
import { SymbolTableManager } from './symbolTableManager';
import { ScopeManager } from './scopeManager';
import { ModuleResolver } from './moduleResolver';

export class DefinitionProvider implements vscode.DefinitionProvider {
    private symbolTableManager: SymbolTableManager;
    private scopeManager: ScopeManager;
    private moduleResolver: ModuleResolver;

    constructor() {
        this.symbolTableManager = SymbolTableManager.getInstance();
        this.scopeManager = new ScopeManager();
        this.moduleResolver = new ModuleResolver();
    }

    /**
     * Provide definition locations for the given position
     */
    public async provideDefinition(
        document: vscode.TextDocument,
        position: vscode.Position,
        _token: vscode.CancellationToken
    ): Promise<vscode.Definition | undefined> {
        try {
            // First, ensure symbols are collected for this document
            await this.collectSymbols(document);

            // Get the word at cursor position
            const wordRange = document.getWordRangeAtPosition(position);
            if (!wordRange) {
                return undefined;
            }

            const word = document.getText(wordRange);

            // Get current scope (if inside an interface)
            const currentScope = this.scopeManager.getCurrentScope(document, position);

            // Parse the context to determine what type of reference this is
            const referenceType = await this.parseReferenceType(document, position, word);

            switch (referenceType.type) {
                case 'foreign_key':
                    return await this.handleForeignKeyReference(document, position, referenceType, currentScope);

                case 'type_reference':
                    return await this.handleTypeReference(document, position, referenceType, currentScope);

                case 'field_reference':
                    return await this.handleFieldReference(document, position, referenceType, currentScope);

                default:
                    return await this.handleSimpleReference(document, word, currentScope);
            }
        } catch (error) {
            console.error('[DefinitionProvider] Error providing definition:', error);
            return undefined;
        }
    }

    /**
     * Collect symbols from the document using LocationVisitor
     */
    private async collectSymbols(document: vscode.TextDocument): Promise<void> {
        // Clear existing symbols for this file
        this.symbolTableManager.clearFileSymbols(document.uri);

        try {
            // Create ANTLR4 input stream from document
            const inputStream = CharStream.fromString(document.getText());
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);

            // Parse the document
            const parser = new CfgParser(tokenStream);
            const parseTree = parser.schema();

            // Create location visitor to collect symbols
            const visitor = new LocationVisitor(document);

            // Walk the parse tree to collect symbol locations
            visitor.walk(parseTree);
        } catch (error) {
            console.error('[DefinitionProvider] Error collecting symbols:', error);
        }
    }

    /**
     * Parse the reference type at the given position
     */
    private async parseReferenceType(
        document: vscode.TextDocument,
        position: vscode.Position,
        _word: string
    ): Promise<{ type: string; details?: unknown }> {
        const lineText = document.lineAt(position.line).text;
        const cursorIndex = position.character;

        // Check for foreign key references (-> or =>)
        const fkMatch = lineText.match(/(->|=>)\s*([\w.]+)(?:\[(\w+)\])?/);
        if (fkMatch) {
            const operator = fkMatch[1];
            const tableRef = fkMatch[2];
            const fieldRef = fkMatch[3];

            // Check if cursor is on the table reference or field reference
            const tableStart = lineText.indexOf(tableRef);
            const tableEnd = tableStart + tableRef.length;

            if (cursorIndex >= tableStart && cursorIndex <= tableEnd) {
                return {
                    type: 'foreign_key',
                    details: {
                        operator,
                        tableRef,
                        fieldRef,
                        isTableRef: true
                    }
                };
            }

            if (fieldRef && cursorIndex >= lineText.indexOf(fieldRef)) {
                return {
                    type: 'field_reference',
                    details: {
                        operator,
                        tableRef,
                        fieldRef
                    }
                };
            }
        }

        // Check for type references in field declarations
        const typeMatch = lineText.match(/\w+\s*:\s*([\w.]+)/);
        if (typeMatch) {
            const typeRef = typeMatch[1];
            const typeStart = lineText.indexOf(typeRef);
            const typeEnd = typeStart + typeRef.length;

            if (cursorIndex >= typeStart && cursorIndex <= typeEnd) {
                return {
                    type: 'type_reference',
                    details: {
                        typeRef
                    }
                };
            }
        }

        // Default to simple reference
        return { type: 'simple_reference' };
    }

    /**
     * Handle foreign key references (->table, =>table[field])
     */
    private async handleForeignKeyReference(
        document: vscode.TextDocument,
        _position: vscode.Position,
        reference: { type: string; details?: unknown },
        currentScope?: string
    ): Promise<vscode.Definition | undefined> {
        const details = reference.details as { tableRef: string; fieldRef?: string; isTableRef: boolean };
        const { tableRef, isTableRef } = details;

        if (isTableRef) {
            // Handle table reference
            const parsedName = this.scopeManager.parseQualifiedName(tableRef);

            if (parsedName.packagePath) {
                // Qualified reference (pkg.table)
                const location = await this.moduleResolver.findSymbolLocation(
                    parsedName.symbolName,
                    document.uri,
                    parsedName.packagePath
                );

                if (location) {
                    return location;
                }
            } else {
                // Simple reference (table)
                // First try scope-based lookup
                const symbol = this.scopeManager.findSymbolWithScopePriority(
                    parsedName.symbolName,
                    document.uri,
                    currentScope
                );

                if (symbol) {
                    return new vscode.Location(symbol.uri, symbol.range);
                }

                // Then try module resolver
                const location = await this.moduleResolver.findSymbolLocation(
                    parsedName.symbolName,
                    document.uri
                );

                if (location) {
                    return location;
                }
            }
        }

        return undefined;
    }

    /**
     * Handle type references (TypeName, pkg.TypeName)
     */
    private async handleTypeReference(
        document: vscode.TextDocument,
        _position: vscode.Position,
        reference: { type: string; details?: unknown },
        currentScope?: string
    ): Promise<vscode.Definition | undefined> {
        const details = reference.details as { typeRef: string };
        const { typeRef } = details;
        const parsedName = this.scopeManager.parseQualifiedName(typeRef);

        if (parsedName.packagePath) {
            // Qualified reference (pkg.TypeName)
            const location = await this.moduleResolver.findSymbolLocation(
                parsedName.symbolName,
                document.uri,
                parsedName.packagePath
            );

            if (location) {
                return location;
            }
        } else {
            // Simple reference (TypeName)
            // Use scope-based lookup
            const symbol = this.scopeManager.findSymbolWithScopePriority(
                parsedName.symbolName,
                document.uri,
                currentScope
            );

            if (symbol) {
                return new vscode.Location(symbol.uri, symbol.range);
            }
        }

        return undefined;
    }

    /**
     * Handle field references (=>table[field])
     */
    private async handleFieldReference(
        document: vscode.TextDocument,
        _position: vscode.Position,
        reference: { type: string; details?: unknown },
        currentScope?: string
    ): Promise<vscode.Definition | undefined> {
        const details = reference.details as { tableRef: string; fieldRef?: string };
        const { tableRef } = details;

        // First find the table definition
        const parsedTableName = this.scopeManager.parseQualifiedName(tableRef);
        let tableSymbol: { uri: vscode.Uri; range: vscode.Range } | undefined;

        if (parsedTableName.packagePath) {
            // Find table in package
            const tableLocation = await this.moduleResolver.findSymbolLocation(
                parsedTableName.symbolName,
                document.uri,
                parsedTableName.packagePath
            );

            if (tableLocation) {
                // For now, we'll return the table location
                // In a more sophisticated implementation, we would parse the table
                // and find the specific field definition
                return tableLocation;
            }
        } else {
            // Find table in current scope
            tableSymbol = this.scopeManager.findSymbolWithScopePriority(
                parsedTableName.symbolName,
                document.uri,
                currentScope
            );

            if (tableSymbol) {
                // For now, we'll return the table location
                // Field-level jump would require parsing the table definition
                return new vscode.Location(tableSymbol.uri, tableSymbol.range);
            }
        }

        return undefined;
    }

    /**
     * Handle simple references (fallback)
     */
    private async handleSimpleReference(
        document: vscode.TextDocument,
        word: string,
        currentScope?: string
    ): Promise<vscode.Definition | undefined> {
        const symbol = this.scopeManager.findSymbolWithScopePriority(
            word,
            document.uri,
            currentScope
        );

        if (symbol) {
            return new vscode.Location(symbol.uri, symbol.range);
        }

        return undefined;
    }

    /**
     * Dispose the definition provider
     */
    public dispose(): void {
        this.symbolTableManager.dispose();
    }
}
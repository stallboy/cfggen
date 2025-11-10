/**
 * Scope Manager
 * Manages nested scopes for symbol lookup with priority
 * Supports interface-level, module-level, and cross-module scope resolution
 */

import * as vscode from 'vscode';
import { SymbolTableManager, SymbolLocation } from './symbolTableManager';

export class ScopeManager {
    private symbolTableManager: SymbolTableManager;

    constructor() {
        this.symbolTableManager = SymbolTableManager.getInstance();
    }

    /**
     * Find symbol with scope priority according to JumpRule.md
     */
    public findSymbolWithScopePriority(
        name: string,
        currentUri: vscode.Uri,
        currentScope?: string
    ): SymbolLocation | undefined {
        // 1. If in interface, search in current interface first
        if (currentScope && this.isInterfaceScope(currentScope)) {
            const symbol = this.symbolTableManager.findSymbol(name, currentUri, currentScope);
            if (symbol) {
                return symbol;
            }
        }

        // 2. Search in current module (current file)
        const symbolInCurrentFile = this.symbolTableManager.findSymbolInFile(name, currentUri);
        if (symbolInCurrentFile) {
            return symbolInCurrentFile;
        }

        // 3. Search globally (across all files)
        const globalSymbol = this.symbolTableManager.findSymbol(name, currentUri);
        if (globalSymbol) {
            return globalSymbol;
        }

        return undefined;
    }

    /**
     * Check if a scope name represents an interface
     */
    private isInterfaceScope(scopeName: string): boolean {
        // For now, we assume any scope that's not undefined is an interface scope
        // In a more sophisticated implementation, we could check the symbol type
        return scopeName !== undefined;
    }

    /**
     * Get the current scope from cursor position
     * This determines if we're inside an interface declaration
     */
    public getCurrentScope(document: vscode.TextDocument, position: vscode.Position): string | undefined {
        const symbols = this.symbolTableManager.getSymbolsInFile(document.uri);

        // Find the innermost interface that contains the current position
        let currentInterface: string | undefined;

        for (const symbol of symbols) {
            if (symbol.type === 'interface' && symbol.range.contains(position)) {
                // If we find an interface that contains the position, and it's deeper than the current one
                if (!currentInterface) {
                    currentInterface = symbol.name;
                    break;
                }
            }
        }

        return currentInterface;
    }

    /**
     * Parse a qualified name (e.g., "pkg1.pkg2.StructD") and extract components
     */
    public parseQualifiedName(qualifiedName: string): {
        packagePath?: string[];
        symbolName: string;
    } {
        const parts = qualifiedName.split('.');

        if (parts.length === 1) {
            return {
                symbolName: parts[0]
            };
        }

        // Last part is the symbol name, the rest is package path
        const symbolName = parts[parts.length - 1];
        const packagePath = parts.slice(0, parts.length - 1);

        return {
            packagePath,
            symbolName
        };
    }

    /**
     * Check if a symbol reference is qualified (has package path)
     */
    public isQualifiedReference(reference: string): boolean {
        return reference.includes('.');
    }

    /**
     * Get all available scopes in a document
     */
    public getAvailableScopes(document: vscode.TextDocument): string[] {
        const symbols = this.symbolTableManager.getSymbolsInFile(document.uri);
        const scopes = new Set<string>();

        symbols.forEach(symbol => {
            if (symbol.scope) {
                scopes.add(symbol.scope);
            }
        });

        return Array.from(scopes);
    }

    /**
     * Clear scope information for a specific file
     */
    public clearFileScopes(_uri: vscode.Uri): void {
        // Scope information is managed by the symbol table manager
        // This method is for future extension if needed
    }
}
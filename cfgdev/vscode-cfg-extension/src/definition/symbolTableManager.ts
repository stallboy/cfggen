/**
 * Symbol Table Manager
 * Manages symbols across multiple files with support for nested scopes
 * Supports interface-level, module-level, and cross-module symbol lookup
 */

import * as vscode from 'vscode';

export interface SymbolLocation {
    uri: vscode.Uri;
    range: vscode.Range;
    name: string;
    type: SymbolType;
    scope?: string; // For nested scopes (e.g., interface name)
}

export enum SymbolType {
    STRUCT = 'struct',
    INTERFACE = 'interface',
    TABLE = 'table',
    FIELD = 'field',
    TYPE = 'type'
}

export class SymbolTableManager {
    private static instance: SymbolTableManager;
    private symbols: Map<string, SymbolLocation[]> = new Map(); // fileUri -> symbols
    private scopeSymbols: Map<string, Map<string, SymbolLocation>> = new Map(); // scopeName -> symbolName -> location
    private fileWatchers: Map<string, vscode.FileSystemWatcher> = new Map();

    private constructor() {
        this.setupFileWatchers();
    }

    public static getInstance(): SymbolTableManager {
        if (!SymbolTableManager.instance) {
            SymbolTableManager.instance = new SymbolTableManager();
        }
        return SymbolTableManager.instance;
    }

    /**
     * Add a symbol to the symbol table
     */
    public addSymbol(uri: vscode.Uri, symbol: Omit<SymbolLocation, 'uri'>): void {
        const fileUri = uri.toString();

        if (!this.symbols.has(fileUri)) {
            this.symbols.set(fileUri, []);
        }

        const symbolWithUri: SymbolLocation = {
            ...symbol,
            uri
        };

        this.symbols.get(fileUri)!.push(symbolWithUri);

        // Also add to scope-based lookup if scope is provided
        if (symbol.scope) {
            if (!this.scopeSymbols.has(symbol.scope)) {
                this.scopeSymbols.set(symbol.scope, new Map());
            }
            this.scopeSymbols.get(symbol.scope)!.set(symbol.name, symbolWithUri);
        }
    }

    /**
     * Find symbol by name with scope priority
     */
    public findSymbol(
        name: string,
        currentUri: vscode.Uri,
        currentScope?: string
    ): SymbolLocation | undefined {
        // 1. Search in current scope first (if provided)
        if (currentScope && this.scopeSymbols.has(currentScope)) {
            const scopeSymbols = this.scopeSymbols.get(currentScope)!;
            if (scopeSymbols.has(name)) {
                return scopeSymbols.get(name);
            }
        }

        // 2. Search in current file
        const currentFileUri = currentUri.toString();
        if (this.symbols.has(currentFileUri)) {
            const fileSymbols = this.symbols.get(currentFileUri)!;
            const symbolInFile = fileSymbols.find(s => s.name === name);
            if (symbolInFile) {
                return symbolInFile;
            }
        }

        // 3. Search in all files (global lookup)
        for (const [_fileUri, symbols] of this.symbols) {
            const symbol = symbols.find(s => s.name === name);
            if (symbol) {
                return symbol;
            }
        }

        return undefined;
    }

    /**
     * Find symbol by name in specific file
     */
    public findSymbolInFile(name: string, uri: vscode.Uri): SymbolLocation | undefined {
        const fileUri = uri.toString();
        if (this.symbols.has(fileUri)) {
            return this.symbols.get(fileUri)!.find(s => s.name === name);
        }
        return undefined;
    }

    /**
     * Get all symbols in a file
     */
    public getSymbolsInFile(uri: vscode.Uri): SymbolLocation[] {
        const fileUri = uri.toString();
        return this.symbols.get(fileUri) || [];
    }

    /**
     * Clear symbols for a specific file
     */
    public clearFileSymbols(uri: vscode.Uri): void {
        const fileUriString = uri.toString();

        // Remove from main symbols map
        if (this.symbols.has(fileUriString)) {
            const symbols = this.symbols.get(fileUriString)!;

            // Also remove from scope symbols
            symbols.forEach(symbol => {
                if (symbol.scope && this.scopeSymbols.has(symbol.scope)) {
                    this.scopeSymbols.get(symbol.scope)!.delete(symbol.name);
                }
            });

            this.symbols.delete(fileUriString);
        }
    }

    /**
     * Clear all symbols
     */
    public clearAll(): void {
        this.symbols.clear();
        this.scopeSymbols.clear();
    }

    /**
     * Get all symbols (for debugging)
     */
    public getAllSymbols(): Map<string, SymbolLocation[]> {
        return new Map(this.symbols);
    }

    /**
     * Setup file watchers to clear symbols when files change
     */
    private setupFileWatchers(): void {
        const workspaceFolders = vscode.workspace.workspaceFolders;
        if (!workspaceFolders) {
            return;
        }

        workspaceFolders.forEach(folder => {
            const pattern = new vscode.RelativePattern(folder, '**/*.cfg');
            const watcher = vscode.workspace.createFileSystemWatcher(pattern);

            watcher.onDidChange(uri => {
                this.clearFileSymbols(uri);
            });

            watcher.onDidDelete(uri => {
                this.clearFileSymbols(uri);
            });

            this.fileWatchers.set(folder.uri.toString(), watcher);
        });
    }

    /**
     * Dispose the symbol table manager
     */
    public dispose(): void {
        this.fileWatchers.forEach(watcher => watcher.dispose());
        this.fileWatchers.clear();
        this.clearAll();
    }
}
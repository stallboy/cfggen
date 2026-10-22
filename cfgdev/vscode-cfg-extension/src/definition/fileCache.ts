import * as vscode from 'vscode';
import {LocationVisitor} from './locationVisitor';
import {FileDefinitionAndRef} from './fileDefinitionAndRef';
import {parseCfg} from '../utils/cfgDocument';
import {ErrorHandler} from '../utils/errorHandler';

export class FileCache {
    private static readonly instance = new FileCache();
    private readonly cache = new Map<string, {document: vscode.TextDocument; version: number; file: FileDefinitionAndRef}>();
    private constructor() {}
    static getInstance(): FileCache { return this.instance; }

    async getOrParse(filePath: string): Promise<FileDefinitionAndRef | undefined> {
        try {
            return this.getOrParseDefinitionAndRef(await vscode.workspace.openTextDocument(vscode.Uri.file(filePath)));
        } catch (error) { ErrorHandler.logError('FileCache.getOrParse', error); }
    }
    async getOrParseDefinitionAndRef(document: vscode.TextDocument): Promise<FileDefinitionAndRef> {
        const key = document.uri.toString();
        const cached = this.cache.get(key);
        if (cached?.document === document && cached.version === document.version) return cached.file;
        const file = new FileDefinitionAndRef(document.uri.fsPath);
        const {tree, positions} = parseCfg(document);
        tree.accept(new LocationVisitor(file, positions));
        this.cache.set(key, {document, version: document.version, file});
        return file;
    }
    remove(document: vscode.TextDocument): void { this.cache.delete(document.uri.toString()); }
    clearAll(): void { this.cache.clear(); }
}

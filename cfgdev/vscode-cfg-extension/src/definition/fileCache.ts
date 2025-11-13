import * as vscode from 'vscode';
import { CommonTokenStream } from 'antlr4ng';
import { CharStream } from 'antlr4ng';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { LocationVisitor } from './locationVisitor';
import { FileDefinitionAndRef } from './fileDefinitionAndRef';
import { ErrorHandler } from '../utils/errorHandler';

/**
 * 文件缓存管理器
 */
export class FileCache {
    private static instance: FileCache;
    private cache: Map<string, FileDefinitionAndRef> = new Map();

    /**
     * 私有构造函数，确保只能通过getInstance创建实例
     */
    private constructor() { }

    /**
     * 获取FileCache单例实例
     */
    public static getInstance(): FileCache {
        if (!FileCache.instance) {
            FileCache.instance = new FileCache();
        }
        return FileCache.instance;
    }

    
    /**
     * 获取文件定义
     */
    async getOrParse(filePath: string): Promise<FileDefinitionAndRef | undefined> {
        try {
            const document = await vscode.workspace.openTextDocument(vscode.Uri.file(filePath));
            return await this.getOrParseDefinitionAndRef(document);
        } catch (error) {
            ErrorHandler.logError('FileCache.getFileDefinition', `Error parsing file ${filePath}: ${error}`);
            return undefined;
        }
    }

    /**
     * 获取或解析文件的定义和引用信息
     * 如果缓存有效则返回缓存，否则解析文件并缓存结果
     */
    async getOrParseDefinitionAndRef(document: vscode.TextDocument): Promise<FileDefinitionAndRef> {
        const filePath = document.uri.fsPath;
        let stats;
        // 从缓存中读
        const cached = this.cache.get(filePath);
        if (cached) {
            stats = await this.getFileStats(filePath);
            if (stats && stats.mtime === cached.lastModified && stats.size === cached.fileSize) {
                return cached;
            }

            this.cache.delete(filePath);
        }

        // 解析文件
        const fileDef = await this.parseFile(document);

        // 缓存
        if (!cached) {
            stats = await this.getFileStats(filePath);
        }
        if (stats) {
            fileDef.lastModified = stats.mtime;
            fileDef.fileSize = stats.size;
        } 
        this.cache.set(filePath, fileDef);

        return fileDef;
    }


    /**
     * 获取文件统计信息
     */
    private async getFileStats(filePath: string): Promise<{ mtime: number; size: number } | undefined> {
        try {
            const stats = await vscode.workspace.fs.stat(vscode.Uri.file(filePath));
            return { mtime: stats.mtime, size: stats.size };
        } catch {
            return undefined;
        }
    }

    /**
     * 解析文件，收集定义和引用
     */
    private async parseFile(document: vscode.TextDocument): Promise<FileDefinitionAndRef> {
        const fileDef = new FileDefinitionAndRef(document.uri.fsPath);

        try {
            // 创建语法树
            const inputStream = CharStream.fromString(document.getText());
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);
            const parser = new CfgParser(tokenStream);

            // 构建语法树
            const tree = parser.schema();

            // 使用位置访问者收集信息
            const visitor = new LocationVisitor(fileDef);
            visitor.walk(tree);

        } catch (error) {
            ErrorHandler.logError('FileCache.parseFile', error);
        }

        return fileDef;
    }


    /**
     * 清除所有缓存
     */
    clearAll(): void {
        this.cache.clear();
    }

}
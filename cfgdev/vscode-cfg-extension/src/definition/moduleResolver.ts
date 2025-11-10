/**
 * Module Resolver
 * Resolves module names to file paths according to JumpRule.md rules
 * Supports Chinese directory names and multi-level package names
 */

import * as vscode from 'vscode';
import { CommonTokenStream } from 'antlr4ng';
import { CharStream } from 'antlr4ng';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { LocationVisitor } from './locationVisitor';
import { SymbolTableManager, SymbolType } from './symbolTableManager';

export class ModuleResolver {
    /**
     * Extract module name from directory name according to JumpRule.md rules
     * 1. 截取第一个"."之前的内容
     * 2. 再截取"_汉字"或汉字之前的部分
     */
    public extractModuleNameFromDirectory(dirName: string): string {
        let moduleName = dirName;

        // 1. 截取第一个"."之前的内容
        const firstDotIndex = moduleName.indexOf('.');
        if (firstDotIndex !== -1) {
            moduleName = moduleName.substring(0, firstDotIndex);
        }

        // 2. 再截取"_汉字"或汉字之前的部分
        // Find the position of first Chinese character or "_汉字" pattern
        let cutIndex = -1;

        // Look for "_汉字" pattern
        const underscoreChineseMatch = moduleName.match(/_(\p{Script=Han})/u);
        if (underscoreChineseMatch && underscoreChineseMatch.index !== undefined) {
            cutIndex = underscoreChineseMatch.index;
        } else {
            // Look for first Chinese character
            const chineseMatch = moduleName.match(/\p{Script=Han}/u);
            if (chineseMatch && chineseMatch.index !== undefined) {
                cutIndex = chineseMatch.index;
            }
        }

        if (cutIndex !== -1) {
            moduleName = moduleName.substring(0, cutIndex);
        }

        return moduleName;
    }

    /**
     * Determine the root directory according to JumpRule.md rules
     * 1. 从当前.cfg文件如果是`config.cfg`，则当前目录就是`<本配置所属根目录>`
     * 2. 否则向上搜索父目录，直到发现包含`config.cfg`文件的目录，该目录即为`<本配置所属根目录>`
     */
    public async findRootDirectory(currentFileUri: vscode.Uri): Promise<vscode.Uri | undefined> {
        const currentFileName = currentFileUri.path.split('/').pop();

        // 1. If current file is config.cfg, current directory is root
        if (currentFileName === 'config.cfg') {
            return vscode.Uri.joinPath(currentFileUri, '..');
        }

        // 2. Search upward for directory containing config.cfg
        let currentDir = vscode.Uri.joinPath(currentFileUri, '..');

        while (currentDir.path !== '') {
            const configUri = vscode.Uri.joinPath(currentDir, 'config.cfg');

            try {
                await vscode.workspace.fs.stat(configUri);
                return currentDir;
            } catch {
                // config.cfg not found in this directory, continue upward
            }

            const parentDir = vscode.Uri.joinPath(currentDir, '..');

            // Check if we've reached the filesystem root
            if (parentDir.path === currentDir.path) {
                break;
            }

            currentDir = parentDir;
        }

        return undefined;
    }

    /**
     * Resolve package path to file path according to JumpRule.md rules
     * `pkg1.pkg2`可能对应的文件路径是`pkg1_中文1/pkg2中文2/pkg2.cfg`
     */
    public async resolvePackageToFilePath(
        packagePath: string[],
        searchFromUri: vscode.Uri
    ): Promise<vscode.Uri | undefined> {
        const searchDir = vscode.Uri.joinPath(searchFromUri, '..');

        // Build the expected directory structure
        let currentUri = searchDir;

        for (let i = 0; i < packagePath.length; i++) {
            const packageName = packagePath[i];
            const foundDir = await this.findMatchingDirectory(currentUri, packageName);

            if (!foundDir) {
                return undefined;
            }

            currentUri = foundDir;
        }

        // The file should be named after the last package component
        const fileName = `${packagePath[packagePath.length - 1]}.cfg`;
        const fileUri = vscode.Uri.joinPath(currentUri, fileName);

        try {
            await vscode.workspace.fs.stat(fileUri);
            return fileUri;
        } catch {
            return undefined;
        }
    }

    /**
     * Find directory that matches the package name according to extraction rules
     */
    private async findMatchingDirectory(parentUri: vscode.Uri, packageName: string): Promise<vscode.Uri | undefined> {
        try {
            const entries = await vscode.workspace.fs.readDirectory(parentUri);

            for (const [entryName, entryType] of entries) {
                if (entryType === vscode.FileType.Directory) {
                    const extractedModuleName = this.extractModuleNameFromDirectory(entryName);

                    if (extractedModuleName === packageName) {
                        return vscode.Uri.joinPath(parentUri, entryName);
                    }
                }
            }
        } catch (error) {
            console.error(`Error reading directory ${parentUri.path}:`, error);
        }

        return undefined;
    }

    /**
     * Search for symbol in two strategies according to JumpRule.md
     * 1. 从当前.cfg文件所在目录开始查找
     * 2. 从根目录开始查找
     */
    public async findSymbolLocation(
        symbolName: string,
        currentUri: vscode.Uri,
        packagePath?: string[]
    ): Promise<vscode.Location | undefined> {
        // Strategy 1: Search from current directory
        if (packagePath) {
            const fileFromCurrentDir = await this.resolvePackageToFilePath(packagePath, currentUri);
            if (fileFromCurrentDir) {
                const symbolInFile = await this.findSymbolInFile(symbolName, fileFromCurrentDir);
                if (symbolInFile) {
                    return symbolInFile;
                }
            }
        }

        // Strategy 2: Search from root directory
        const rootDir = await this.findRootDirectory(currentUri);
        if (rootDir) {
            if (!packagePath) {
                // Search in config.cfg at root
                const configFile = vscode.Uri.joinPath(rootDir, 'config.cfg');
                const symbolInConfig = await this.findSymbolInFile(symbolName, configFile);
                if (symbolInConfig) {
                    return symbolInConfig;
                }
            } else {
                // Search from root for package
                const fileFromRoot = await this.resolvePackageToFilePath(packagePath, rootDir);
                if (fileFromRoot) {
                    const symbolInFile = await this.findSymbolInFile(symbolName, fileFromRoot);
                    if (symbolInFile) {
                        return symbolInFile;
                    }
                }
            }
        }

        return undefined;
    }

    /**
     * Find symbol in a specific file
     */
    private async findSymbolInFile(symbolName: string, fileUri: vscode.Uri): Promise<vscode.Location | undefined> {
        try {
            // Check if file exists
            await vscode.workspace.fs.stat(fileUri);

            // Use the symbol table manager to find the symbol definition
            // This ensures we only find actual definitions, not usages
            const symbolTableManager = SymbolTableManager.getInstance();
            const symbol = symbolTableManager.findSymbolInFile(symbolName, fileUri);

            if (symbol) {
                // Only return if it's a definition (not a type reference)
                if (symbol.type !== SymbolType.TYPE) {
                    return new vscode.Location(symbol.uri, symbol.range);
                }
            }

            // If symbol table doesn't have it, parse the file to collect symbols
            const document = await vscode.workspace.openTextDocument(fileUri);

            // Create ANTLR4 input stream from document
            const inputStream = CharStream.fromString(document.getText());
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);

            // Parse the document
            const parser = new CfgParser(tokenStream);
            const parseTree = parser.schema();

            // Create location visitor to collect symbols
            const visitor = new LocationVisitor(document);
            visitor.walk(parseTree);

            // Try again with updated symbol table
            const updatedSymbol = symbolTableManager.findSymbolInFile(symbolName, fileUri);
            if (updatedSymbol && updatedSymbol.type !== SymbolType.TYPE) {
                return new vscode.Location(updatedSymbol.uri, updatedSymbol.range);
            }
        } catch (error) {
            // File doesn't exist or other error
            console.error(`Error searching in file ${fileUri.fsPath}:`, error);
        }

        return undefined;
    }

    /**
     * Get all available modules in the workspace
     */
    public async getAvailableModules(): Promise<Map<string, vscode.Uri>> {
        const modules = new Map<string, vscode.Uri>();
        const workspaceFolders = vscode.workspace.workspaceFolders;

        if (!workspaceFolders) {
            return modules;
        }

        for (const folder of workspaceFolders) {
            await this.collectModulesFromDirectory(folder.uri, modules);
        }

        return modules;
    }

    /**
     * Recursively collect modules from directory
     */
    private async collectModulesFromDirectory(
        dirUri: vscode.Uri,
        modules: Map<string, vscode.Uri>
    ): Promise<void> {
        try {
            const entries = await vscode.workspace.fs.readDirectory(dirUri);

            for (const [entryName, entryType] of entries) {
                if (entryType === vscode.FileType.Directory) {
                    const moduleName = this.extractModuleNameFromDirectory(entryName);
                    const moduleUri = vscode.Uri.joinPath(dirUri, entryName);

                    if (moduleName && !modules.has(moduleName)) {
                        modules.set(moduleName, moduleUri);
                    }

                    // Recursively search subdirectories
                    await this.collectModulesFromDirectory(moduleUri, modules);
                }
            }
        } catch (error) {
            console.error(`Error collecting modules from ${dirUri.fsPath}:`, error);
        }
    }
}
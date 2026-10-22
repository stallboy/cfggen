import * as vscode from 'vscode';
import {FileCache} from './fileCache';
import {CfgDefinitionProvider} from './definitionProvider';
import {ModuleResolver} from '../utils/moduleResolver';

export class CfgReferenceProvider implements vscode.ReferenceProvider {
    async provideReferences(document: vscode.TextDocument, position: vscode.Position,
                            context: vscode.ReferenceContext, token: vscode.CancellationToken): Promise<vscode.Location[]> {
        const cache = FileCache.getInstance();
        const current = await cache.getOrParseDefinitionAndRef(document);
        const definition = current.getDefinitionAtPosition(position);
        if (!definition) return [];
        const root = await ModuleResolver.findRootDirectory(current.filePath);
        const paths = root ? await ModuleResolver.findCfgFilesInRoot(root) : [current.filePath];
        const locations = context.includeDeclaration ? [new vscode.Location(document.uri, definition.range)] : [];
        const provider = new CfgDefinitionProvider();
        for (const path of paths) {
            if (token.isCancellationRequested) break;
            const file = await cache.getOrParse(path);
            if (!file) continue;
            for (const ref of file.references) {
                if (ref.name !== definition.name && !ref.name.endsWith('.' + definition.name)) continue;
                const found = await provider.resolve(ref, file);
                if (found && vscode.Uri.file(found.filePath).toString() === document.uri.toString() && found.range.isEqual(definition.range)) {
                    locations.push(new vscode.Location(vscode.Uri.file(path), ref.range));
                }
            }
        }
        return locations;
    }
}

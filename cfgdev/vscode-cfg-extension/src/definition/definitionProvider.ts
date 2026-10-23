import * as vscode from 'vscode';
import {FileCache} from './fileCache';
import {FileDefinitionAndRef, PositionRef, ResolvedLocation} from './fileDefinitionAndRef';
import {ModuleResolver} from '../utils/moduleResolver';
import {PathUtils} from '../utils/pathUtils';

export class CfgDefinitionProvider implements vscode.DefinitionProvider {
    async provideDefinition(document: vscode.TextDocument, position: vscode.Position): Promise<vscode.Location[] | undefined> {
        const file = await FileCache.getInstance().getOrParseDefinitionAndRef(document);
        const ref = file.getRefAtPosition(position);
        const found = ref && await this.resolve(ref, file);
        return found ? [new vscode.Location(vscode.Uri.file(found.filePath), found.range)] : undefined;
    }

    async resolve(ref: PositionRef, current: FileDefinitionAndRef): Promise<ResolvedLocation | undefined> {
        const local = this.inFile(current, ref.name, ref);
        if (local) return local;
        const root = await ModuleResolver.findRootDirectory(current.filePath);
        const bases = [PathUtils.getDirname(current.filePath), ...(root ? [root] : [])];
        const parts = ref.name.split('.');
        for (let split = parts.length - 1; split > 0; split--) {
            for (const base of bases) {
                const path = await ModuleResolver.findModuleFile(parts.slice(0, split).join('.'), base);
                const file = path && await FileCache.getInstance().getOrParse(path);
                const found = file && this.inFile(file, parts.slice(split).join('.'), {...ref, inInterfaceName: undefined});
                if (found) return found;
            }
        }
        if (root) {
            const file = await FileCache.getInstance().getOrParse(PathUtils.joinPath(root, 'config.cfg'));
            if (file) return this.inFile(file, ref.name, {...ref, inInterfaceName: undefined});
        }
    }

    private inFile(file: FileDefinitionAndRef, name: string, ref: PositionRef): ResolvedLocation | undefined {
        if (ref.isRefType) {
            const parts = name.split('.');
            const owner = parts.length > 1 ? parts.slice(0, -1).join('.') : ref.inInterfaceName;
            const range = owner && file.definitionsInInterface.get(owner)?.get(parts[parts.length - 1]);
            if (range) return {filePath: file.filePath, range};
        }
        const def = file.definitions.get(name);
        if (def && (ref.isRefType ? def.type !== 'table' || def.isEnum : def.type === 'table' || def.type === 'enum')) {
            return {filePath: file.filePath, range: def.range};
        }
    }
}

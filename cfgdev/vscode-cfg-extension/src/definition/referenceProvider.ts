import * as vscode from 'vscode';
import { FileCache } from './fileCache';
import { FileDefinitionAndRef, Ref, PositionDef, RefLoc } from './fileDefinitionAndRef';
import { ModuleResolver } from '../utils/moduleResolver';
import { ErrorHandler } from '../utils/errorHandler';

/**
 * 引用提供者
 */
export class CfgReferenceProvider implements vscode.ReferenceProvider {

    /**
     * 提供引用位置
     */
    async provideReferences(
        document: vscode.TextDocument,
        position: vscode.Position,
        _context: vscode.ReferenceContext,
        _token: vscode.CancellationToken
    ): Promise<vscode.Location[]> {
        const refs: vscode.Location[] = [];

        try {
            // 1. getOrParseDefinitionAndRef得到curDef
            const curDef = await FileCache.getInstance().getOrParseDefinitionAndRef(document);

            const definition = curDef.getDefinitionAtPosition(position);
            if (definition) {
                if (definition.inInterfaceName) {
                    await this.findReferencesInInterface(definition, curDef, refs);
                } else {
                    await this.findReferences(definition, curDef, refs);
                }
            }
        } catch (error) {
            ErrorHandler.logError('CfgReferenceProvider.provideReferences', error);
        }

        return refs;
    }

    /**
     * 查找引用
     */
    private async findReferences(definition: PositionDef, curDef: FileDefinitionAndRef, refs: vscode.Location[]): Promise<void> {
        try {
            // 2.1 找到<本配置所属根目录>，同时找到文件所在的模块全称
            const rootDir = await ModuleResolver.findRootDirectory(curDef.filePath);
            if (!rootDir) {
                return;
            }

            curDef.setModuleNameByRootDir(rootDir);

            // 构建definition的全称
            const fullDefinitionName = curDef.moduleName.length > 0 ? curDef.moduleName + '.' + definition.name : definition.name;

            // 2.2 遍历<本配置所属根目录>，depth最多2层
            const cfgFiles = await ModuleResolver.findCfgFilesInRoot(rootDir, 2);

            for (const cfgFile of cfgFiles) {
                const targetDef = await FileCache.getInstance().getOrParse(cfgFile);
                if (!targetDef) {
                    continue;
                }

                // 对每个def，遍历lineToRefs
                for (const [line, ref] of targetDef.lineToRefs) {
                    // 检查引用匹配
                    const loc = this.getRefLocIfMatch(ref, definition.name, fullDefinitionName, curDef.moduleName, targetDef.moduleName);
                    if (loc) {
                        const location = new vscode.Location(
                            vscode.Uri.file(cfgFile),
                            new vscode.Range(line, loc.start, line, loc.end)
                        );
                        refs.push(location);
                    }
                }
            }
        } catch (error) {
            ErrorHandler.logError('CfgReferenceProvider.findReferencesForTName', error);
        }
    }

    /**
     * 为interface里的struct类型查找引用
     */
    private async findReferencesInInterface(definition: PositionDef, curDef: FileDefinitionAndRef, refs: vscode.Location[]): Promise<void> {

        // 遍历lineToRefs，如果inInterfaceName == implName.inInterfaceName，且refType == implName.name则加到refs里
        for (const [line, ref] of curDef.lineToRefs) {
            if (ref.inInterfaceName === definition.inInterfaceName && ref.refType === definition.name) {
                const location = new vscode.Location(
                    vscode.Uri.file(curDef.filePath),
                    new vscode.Range(line, ref.refTypeStart, line, ref.refTypeEnd)
                );
                refs.push(location);
            }
        }

    }


    /**
     * 检查引用是否匹配
     */
    private getRefLocIfMatch(ref: Ref, defName: string, fullDefName: string,
        defModuleName: string, refModuleName: string): RefLoc | undefined {
        const loc = ref.match(fullDefName);
        if (loc) {
            return loc;
        }


        // 如果 curDef.module.length > 0 且 curDef.module.startWith(def.module + ".") 则找到relativeModuleName
        if (defModuleName.length > 0 &&
            refModuleName.length > 0 &&
            defModuleName.startsWith(refModuleName + ".")) {

            const relativeModuleName = defModuleName.substring(refModuleName.length + 1);
            const relativeDefName = relativeModuleName + '.' + defName;

            const loc = ref.match(relativeDefName);
            if (loc) {
                return loc;
            }
        }
    }

}
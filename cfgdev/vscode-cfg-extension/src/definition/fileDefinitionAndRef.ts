import * as vscode from 'vscode';
import { ModuleResolver } from '../utils/moduleResolver';


export interface RefLoc {
    start: number
    end: number
}

/**
 * 引用信息
 */
export class Ref {
    constructor(
        public refType?: string,
        public refTypeStart: number = 0,
        public refTypeEnd: number = 0,
        public refTable?: string,
        public refTableStart: number = 0,
        public refTableEnd: number = 0,
        public inInterfaceName?: string
    ) { }

    match(defintion: string): RefLoc | undefined {
        if (this.refType === defintion) {
            return {
                start: this.refTypeStart,
                end: this.refTypeEnd
            };
        }

        // 如果 refTable == tname.name也放到refs
        if (this.refTable === defintion) {
            return {
                start: this.refTableStart,
                end: this.refTableEnd
            };
        }
    }
}

/**
 * 定义类型
 */
export type DefinitionType = 'struct' | 'interface' | 'table';

/**
 * 定义范围信息
 */
export interface TRange {
    type: DefinitionType;
    range: vscode.Range;
}

/**
 * 名称信息
 */
export interface TName extends TRange {
    name: string;
}

/**
 * 接口内定义名称
 */
export interface ImplName {
    name: string;
    inInterfaceName: string;
    range: vscode.Range;
}

/**
 * 文件定义和引用信息
 */
export class FileDefinitionAndRef {
    public filePath: string;
    public definitions: Map<string, TRange> = new Map();
    public definitionsInInterface: Map<string, Map<string, vscode.Range>> = new Map();
    public lineToRefs: Map<number, Ref> = new Map();
    public lastModified: number = 0;
    public fileSize: number = 0;
    public moduleName: string = "";

    private _lineToDefinitions?: Map<number, TName>;
    private _lineToDefinitionInInterfaces?: Map<number, ImplName>;

    /**
     * 创建新的 FileDefinitionAndRef 实例
     */
    constructor(filePath: string) {
        this.filePath = filePath;
    }

    setModuleNameByRootDir(rootDir: string) {
        this.moduleName = ModuleResolver.resolvePathToFullModuleName(this.filePath, rootDir);
    }

    /**
     * 获取指定接口的定义
     */
    getDefinitionInInterface(interfaceName: string): Map<string, vscode.Range> | undefined {
        return this.definitionsInInterface.get(interfaceName);
    }

    /**
     * 获取全局定义
     */
    getDefinition(name: string): TRange | undefined {
        return this.definitions.get(name);
    }

    /**
    * 获取指定位置的引用信息
    */
    getRefAtPosition(position: vscode.Position): PositionRef | undefined {
        const ref = this.lineToRefs.get(position.line);
        if (!ref) {
            return undefined;
        }

        const character = position.character;

        // 检查是否在refType范围内
        if (ref.refType && character >= ref.refTypeStart && character <= ref.refTypeEnd) {
            return {
                isRefType: true,
                name: ref.refType,
                inInterfaceName: ref.inInterfaceName
            };
        }

        // 检查是否在refTable范围内
        if (ref.refTable && character >= ref.refTableStart && character <= ref.refTableEnd) {
            return {
                isRefType: false,
                name: ref.refTable,
                inInterfaceName: ref.inInterfaceName
            };
        }
    }
    /**
     * 获取指定位置的定义信息
     * @param position 位置信息
     * @returns 定义信息，如果没有找到则返回 undefined
     */
    getDefinitionAtPosition(position: vscode.Position): PositionDef | undefined {
        const line = position.line;
        const character = position.character;
        // 首先检查全局定义
        const def = this.lineToDefinitions.get(line);
        if (def) {
            if (character >= def.range.start.character && character <= def.range.end.character) {
                return def;
            }
        } else {
            // 然后检查接口内定义
            const idef = this.lineToDefinitionInInterfaces.get(line);
            if (idef && character >= idef.range.start.character && character <= idef.range.end.character) {
                return idef;
            }
        }
    }

    /**
     * 获取行到定义的映射（延迟初始化）
     */
    get lineToDefinitions(): Map<number, TName> {
        if (!this._lineToDefinitions) {
            this._lineToDefinitions = new Map();
            // 从definitions构造lineToDefinitions
            for (const [name, tRange] of this.definitions) {
                const line = tRange.range.start.line;
                this._lineToDefinitions.set(line, {
                    type: tRange.type,
                    name: name,
                    range: tRange.range
                });
            }
        }
        return this._lineToDefinitions;
    }

    /**
     * 获取行到接口内定义的映射（延迟初始化）
     */
    get lineToDefinitionInInterfaces(): Map<number, ImplName> {
        if (!this._lineToDefinitionInInterfaces) {
            this._lineToDefinitionInInterfaces = new Map();
            // 从definitionsInInterface构造lineToDefinitionInInterfaces
            for (const [interfaceName, structMap] of this.definitionsInInterface) {
                for (const [structName, range] of structMap) {
                    const line = range.start.line;
                    this._lineToDefinitionInInterfaces.set(line, {
                        name: structName,
                        inInterfaceName: interfaceName,
                        range: range
                    });
                }
            }
        }
        return this._lineToDefinitionInInterfaces;
    }


}


/**
 * 位置引用信息
 */
export interface PositionRef {
    isRefType: boolean;           // true表示类型引用，false表示外键引用
    name: string;                 // 引用名称
    inInterfaceName?: string;     // 所在接口名称
}

/**
 * 位置类型定义信息
 */
export interface PositionDef {
    name: string;                 // 引用名称
    inInterfaceName?: string;     // 所在接口名称
}

/**
 * 解析结果
 */
export interface ResolvedLocation {
    filePath: string;
    range: vscode.Range;
}
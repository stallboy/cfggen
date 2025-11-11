import * as vscode from 'vscode';



/**
 * 文件定义和引用信息
 */
export class FileDefinitionAndRef {
    constructor(
        public definitions: Map<string, vscode.Range> = new Map(),
        public definitionsInInterface: Map<string, Map<string, vscode.Range>> = new Map(),
        public lineToRefs: Map<number, Ref> = new Map(),
        public lastModified: number = 0,
        public fileSize: number = 0
    ) { }


    /**
     * 获取指定接口的定义
     */
    getDefinitionInInterface(interfaceName: string): Map<string, vscode.Range> | undefined {
        return this.definitionsInInterface.get(interfaceName);
    }

    /**
     * 获取全局定义
     */
    getDefinition(name: string): vscode.Range | undefined {
        return this.definitions.get(name);
    }

    /**
     * 获取指定行的引用信息
     */
    getRefAtLine(line: number): Ref | undefined {
        return this.lineToRefs.get(line);
    }

    /**
     * 获取指定位置的引用信息
     */
    getRefAtPosition(position: vscode.Position): PositionRef | undefined {
        const ref = this.getRefAtLine(position.line);
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

        return undefined;
    }
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
 * 解析结果
 */
export interface ResolvedLocation {
    filePath: string;
    range: vscode.Range;
}
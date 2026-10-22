import * as vscode from 'vscode';

export type DefinitionType = 'struct' | 'interface' | 'table' | 'enum';
export interface TRange { type: DefinitionType; range: vscode.Range; isEnum?: boolean; }
export interface PositionRef { name: string; isRefType: boolean; inInterfaceName?: string; }
export interface DefinitionRef extends PositionRef { range: vscode.Range; }
export interface PositionDef { name: string; range: vscode.Range; }
export interface ResolvedLocation { filePath: string; range: vscode.Range; }

export class FileDefinitionAndRef {
    readonly definitions = new Map<string, TRange>();
    readonly definitionsInInterface = new Map<string, Map<string, vscode.Range>>();
    readonly references: DefinitionRef[] = [];
    constructor(public readonly filePath: string) {}

    getRefAtPosition(position: vscode.Position): DefinitionRef | undefined {
        return this.references.find(ref => contains(ref.range, position));
    }
    getDefinitionAtPosition(position: vscode.Position): PositionDef | undefined {
        for (const [name, {range}] of this.definitions) {
            if (contains(range, position)) return {name, range};
        }
        for (const definitions of this.definitionsInInterface.values()) {
            for (const [name, range] of definitions) {
                if (contains(range, position)) return {name, range};
            }
        }
    }
}

function contains(range: vscode.Range, position: vscode.Position): boolean {
    return range.contains(position) && position.isBefore(range.end);
}

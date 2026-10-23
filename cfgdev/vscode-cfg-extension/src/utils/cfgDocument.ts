import * as vscode from 'vscode';
import {BaseErrorListener, CharStream, CommonTokenStream, ParserRuleContext, Token} from 'antlr4ng';
import {CfgLexer} from '../grammar/CfgLexer';
import {CfgParser} from '../grammar/CfgParser';

/** ANTLR counts code points; VSCode and JavaScript strings count UTF-16 units. */
export class SourcePositions {
    private readonly offsets = [0];
    constructor(private readonly document: vscode.TextDocument) {
        let offset = 0;
        for (const char of document.getText()) {
            offset += char.length;
            this.offsets.push(offset);
        }
    }
    range(context: ParserRuleContext): vscode.Range | undefined {
        return context.start && context.stop ? this.tokenRange(context.start, context.stop) : undefined;
    }
    private tokenRange(start: Token, end: Token): vscode.Range | undefined {
        if (start.start < 0 || end.stop < start.start) return undefined;
        return new vscode.Range(
            this.document.positionAt(this.offsets[start.start]),
            this.document.positionAt(this.offsets[end.stop + 1])
        );
    }
}

class SyntaxErrors extends BaseErrorListener {
    readonly messages: string[] = [];
    override syntaxError(_recognizer: unknown, _symbol: unknown, line: number, column: number, message: string): void {
        this.messages.push(`${line}:${column + 1} ${message}`);
    }
}

export function parseCfg(document: vscode.TextDocument) {
    const lexer = new CfgLexer(CharStream.fromString(document.getText()));
    const errors = new SyntaxErrors();
    lexer.removeErrorListeners();
    lexer.addErrorListener(errors);
    const parser = new CfgParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(errors);
    return {tree: parser.schema(), positions: new SourcePositions(document), errors: errors.messages};
}

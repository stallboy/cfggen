import * as vscode from 'vscode';
import {HighlightingVisitor} from './highlightingVisitor';
import {TOKEN_TYPE_NAMES} from './tokenTypes';
import {parseCfg} from '../utils/cfgDocument';
import {ErrorHandler} from '../utils/errorHandler';

export class SemanticTokensProvider implements vscode.DocumentSemanticTokensProvider {
    private readonly legend = new vscode.SemanticTokensLegend(TOKEN_TYPE_NAMES, []);
    getLegend(): vscode.SemanticTokensLegend { return this.legend; }
    provideDocumentSemanticTokens(document: vscode.TextDocument): vscode.SemanticTokens {
        try {
            const {tree, positions} = parseCfg(document);
            const builder = new vscode.SemanticTokensBuilder(this.legend);
            tree.accept(new HighlightingVisitor(builder, positions));
            return builder.build();
        } catch (error) {
            ErrorHandler.logError('SemanticTokensProvider', error);
            return new vscode.SemanticTokens(new Uint32Array());
        }
    }
}

import * as assert from 'node:assert/strict';
import * as fs from 'node:fs';
import * as path from 'node:path';
import * as vscode from 'vscode';
import {parseCfg} from '../../src/utils/cfgDocument';
import {FileCache} from '../../src/definition/fileCache';
import {SemanticTokensProvider} from '../../src/highlight/semanticTokensProvider';
import {TOKEN_TYPE_NAMES} from '../../src/highlight/tokenTypes';

const extension = path.resolve(__dirname, '../../..');
const workspace = path.join(extension, 'test/workspace');
const file = (name: string) => vscode.Uri.file(path.join(workspace, name));

function at(document: vscode.TextDocument, needle: string, offset = 0): vscode.Position {
    const found = document.getText().indexOf(needle);
    assert.notEqual(found, -1, needle);
    return document.positionAt(found + offset);
}

async function definition(document: vscode.TextDocument, needle: string, offset = 0): Promise<vscode.Location[]> {
    return await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeDefinitionProvider', document.uri, at(document, needle, offset)) ?? [];
}

async function undoAndWait(document: vscode.TextDocument, expected: string): Promise<void> {
    let change: vscode.Disposable | undefined;
    let deadline: NodeJS.Timeout | undefined;
    const restored = new Promise<void>((resolve, reject) => {
        change = vscode.workspace.onDidChangeTextDocument(event => {
            if (event.document === document && document.getText() === expected) resolve();
        });
        deadline = setTimeout(() => reject(new Error('Undo did not restore the expected document text')), 5000);
    });
    try {
        await Promise.all([vscode.commands.executeCommand('undo'), restored]);
    } finally {
        change?.dispose();
        clearTimeout(deadline);
    }
}

export async function run(): Promise<void> {
    await vscode.extensions.getExtension('thyai.cfg-support')!.activate();
    const root = await vscode.workspace.openTextDocument(file('config.cfg'));
    const combat = await vscode.workspace.openTextDocument(file('combat/combat.cfg'));
    let passed = 0;
    async function check(name: string, test: () => Promise<void> | void) {
        await test();
        console.log(`PASS ${name}`);
        passed++;
    }

    await check('shared grammar and actual game schema parse', async () => {
        const canonical = path.resolve(extension, '../../app/src/main/java/configgen/schema/cfg/Cfg.g4');
        assert.equal(fs.readFileSync(path.join(extension, 'src/grammar/Cfg.g4'), 'utf8').replace(/\r\n/g, '\n'),
            fs.readFileSync(canonical, 'utf8').replace(/\r\n/g, '\n'));
        assert.deepEqual(parseCfg(root).errors, []);
        assert.deepEqual(parseCfg(combat).errors, []);
        const game = await vscode.workspace.openTextDocument(vscode.Uri.file(path.resolve(extension, '../../example/config/combat/combat.cfg')));
        assert.deepEqual(parseCfg(game).errors, []);
    });
    await check('table navigation skips same-named local struct', async () => {
        const [target] = await definition(combat, '->ability[code]', 3);
        assert.equal(target.uri.toString(), root.uri.toString());
        assert.equal(root.getText(target.range), 'ability');
        const [legacy] = await definition(combat, '->ability;', 3);
        assert.equal(legacy.uri.toString(), root.uri.toString());
    });
    await check('all mixed ancestors and qualified interface implementation navigate', async () => {
        for (const name of ['AbilityUse', 'MachineUse']) {
            const [target] = await definition(combat, '@' + name, 1);
            assert.equal(root.getText(target.range), name);
        }
        const [impl] = await definition(combat, '@Command.StateMachine', 1);
        assert.equal(root.getText(impl.range), 'StateMachine');
    });
    await check('qualified module types do not inherit the source interface scope', async () => {
        const other = await vscode.workspace.openTextDocument(file('other/other.cfg'));
        const [target] = await definition(root, 'other.Foo', 1);
        const topLevelRefs = await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeReferenceProvider', other.uri, at(other, 'struct Foo', 7));
        const implRefs = await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeReferenceProvider', other.uri, at(other, 'struct Foo { name', 7));
        assert.deepEqual({
            definitionUri: target?.uri.toString(),
            definitionLine: target?.range.start.line,
            topLevelRefs: topLevelRefs?.filter(ref => ref.uri.toString() === root.uri.toString()).length,
            implRefs: implRefs?.filter(ref => ref.uri.toString() === root.uri.toString()).length,
        }, {definitionUri: other.uri.toString(), definitionLine: 0, topLevelRefs: 1, implRefs: 0});
    });
    await check('multiple names on one line and Unicode positions', async () => {
        for (const name of ['first:AbilityUse', 'second:MachineUse']) {
            const [target] = await definition(combat, name, name.indexOf(':') + 1);
            assert.equal(root.getText(target.range), name.split(':')[1]);
        }
        const parsed = await FileCache.getInstance().getOrParseDefinitionAndRef(combat);
        for (const ref of parsed.references) assert.equal(combat.getText(ref.range), ref.name);
        for (const [name, declaration] of parsed.definitions) assert.equal(combat.getText(declaration.range), name);
    });
    await check('internal fields and root scope never jump to a same-named table', async () => {
        assert.deepEqual(await definition(combat, 'machines.phases', 10), []);
        assert.deepEqual(await definition(combat, '->::machines', 4), []);
        assert.deepEqual(await definition(combat, '@AbilityUse::abilityId', 14), []);
    });
    await check('find references shares resolved ranges and namespaces', async () => {
        const refs = await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeReferenceProvider', root.uri, at(root, 'table ability', 6));
        assert.equal(refs?.filter(ref => ref.uri.toString() === combat.uri.toString()).length, 2);
        const uses = await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeReferenceProvider', root.uri, at(root, 'struct AbilityUse', 7));
        assert.equal(uses?.filter(ref => ref.uri.toString() === combat.uri.toString()).length, 2);
        const impl = await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeReferenceProvider', root.uri, at(root, 'struct StateMachine', 7));
        assert.equal(impl?.filter(ref => ref.uri.toString() === combat.uri.toString()).length, 1);
    });
    await check('find references reaches modules deeper than two directories', async () => {
        const deep = await vscode.workspace.openTextDocument(file('world/boss/phase/phase.cfg'));
        const [target] = await definition(deep, '@DeepScope', 1);
        assert.equal(target.uri.toString(), root.uri.toString());
        assert.equal(root.getText(target.range), 'DeepScope');
        const refs = await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeReferenceProvider', root.uri, at(root, 'struct DeepScope', 7));
        assert.equal(refs?.filter(ref => ref.uri.toString() === deep.uri.toString()).length, 2);
    });
    await check('JSON and auxiliary directories never contribute schema references', async () => {
        const ignored = ['_records/_records.cfg', '_records/nested/nested.cfg', '123cache/123cache.cfg'].map(name => file(name).toString());
        const refs = await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeReferenceProvider', root.uri, at(root, 'struct DeepScope', 7));
        assert.deepEqual(refs?.filter(ref => ignored.includes(ref.uri.toString())), []);
    });
    await check('module naming recognizes supplementary Han suffixes like the Java loader', async () => {
        const [target] = await definition(root, 'spells.SupplementaryHan', 1);
        assert.equal(target?.uri.toString(), file('spells_𠀀/spells.cfg').toString());
        const spells = await vscode.workspace.openTextDocument(target.uri);
        assert.equal(spells.getText(target.range), 'SupplementaryHan');
    });
    await check('semantic tokens preserve exact UTF-16 identifier ranges', () => {
        const tokens = new SemanticTokensProvider().provideDocumentSemanticTokens(combat).data;
        let line = 0, column = 0;
        const actual: {name: string; type: string; start: vscode.Position}[] = [];
        for (let i = 0; i < tokens.length; i += 5) {
            line += tokens[i]; column = tokens[i] === 0 ? column + tokens[i + 1] : tokens[i + 1];
            const start = new vscode.Position(line, column);
            const name = combat.getText(new vscode.Range(start, start.translate(0, tokens[i + 2])));
            assert.match(name, /^[a-zA-Z_][a-zA-Z0-9_]*$/);
            actual.push({name, type: TOKEN_TYPE_NAMES[tokens[i + 3]], start});
        }
        const sameLine = actual.find(item => item.name === 'AbilityUse' && item.start.line === 2);
        assert.equal(sameLine?.start.character, combat.lineAt(2).text.indexOf('AbilityUse'));
        assert.ok(actual.some(item => item.name === 'phases' && item.type === 'property'));
        assert.ok(actual.some(item => item.name === 'ability' && item.type === 'foreignKey'));
    });
    await check('unsaved edit and undo refresh cached definitions', async () => {
        const editor = await vscode.window.showTextDocument(combat);
        const original = combat.getText();
        const before = await FileCache.getInstance().getOrParseDefinitionAndRef(combat);
        assert.ok(await editor.edit(edit => edit.insert(new vscode.Position(0, 0), '// 😀 unsaved\n')));
        const after = await FileCache.getInstance().getOrParseDefinitionAndRef(combat);
        assert.notEqual(before, after);
        const [target] = await definition(combat, '@AbilityUse', 1);
        assert.equal(root.getText(target.range), 'AbilityUse');
        assert.equal(after.definitions.get('Use')!.range.start.line, 3);
        await undoAndWait(combat, original);
        assert.equal(combat.getText(), original);
        assert.equal((await FileCache.getInstance().getOrParseDefinitionAndRef(combat)).definitions.get('Use')!.range.start.line, 2);
    });
    console.log(`CFG ExtensionHost: ${passed} tests passed`);
}

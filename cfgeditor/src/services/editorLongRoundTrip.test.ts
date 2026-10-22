import {expect, it} from 'vitest';
import {EditingSession} from './editingSession';
import {Schema} from '@/domain/schema';
import {encodeEditorRecord} from '@/domain/editorValueEncoding';
import {field, makeRawSchema, makeTable} from '@/test/fixtures';
import {getFieldPrimitiveTypeConverter} from './editingObjectUtils';

it('unmodified submission and editing undo/redo keep decimal strings exact', () => {
    const table = makeTable('T', [field('id', 'long'), field('ids', 'list<long>')], {pk: ['id']});
    const schema = new Schema({...makeRawSchema([table]), editorValueEncoding: 'long-decimal-string'});
    const object = {$type: 'T', id: '9007199254740993', ids: ['9223372036854775807']};
    const session = new EditingSession({resultCode: 'ok', table: 'T', id: object.id, maxObjs: 0, object, refs: []});
    session.initUndoBaseline();
    expect(encodeEditorRecord(schema, table, session.getEditingObject())).toEqual(object);
    session.updateFormValues(schema, {id: '9223372036854775807', ids: ['-9223372036854775808']}, [], {id: '9223372036854775807'});
    session.undo(); // undo closes the pending value group before restoring its baseline.
    expect(session.getEditingObject()).toEqual(object);
    session.redo();
    expect(session.getEditingObject().id).toBe('9223372036854775807');
    expect(session.getEditingObject().ids).toEqual(['-9223372036854775808']);
    session.dispose();
});

it('clearing scalar or list long fields restores decimal zero without coercing invalid values', () => {
    const table = makeTable('T', [field('id', 'long'), field('ids', 'list<long>')]);
    for (const name of ['id', 'ids']) {
        const convert = getFieldPrimitiveTypeConverter(name, table)!;
        expect(convert(null)).toBe('0');
        expect(convert('')).toBe('0');
        expect(convert('9007199254740993')).toBe('9007199254740993');
        expect(convert(9007199254740992)).toBe(9007199254740992);
        expect(convert('bad')).toBe('bad');
    }
});

import {describe, expect, it} from 'vitest';
import {encodeEditorRecord, parseCheckedEditorRecord} from './editorValueEncoding';
import {getNextId, Schema} from './schema';
import {field, makeInterface, makeRawSchema, makeStruct, makeTable} from '@/test/fixtures';
import {JSONObject} from '@/api/recordModel';
import {CheckJsonResult} from '@/api/chatModel';

const table = makeTable('T', [field('id', 'long'), field('ids', 'list<long>'), field('map', 'map<Key,I>')], {pk: ['id']});
const key = makeStruct('Key', [field('id', 'long')]);
const impl = makeStruct('Item', [field('id', 'long')]);
const iface = makeInterface('I', [impl]);
const raw = () => makeRawSchema([table, key, iface]);
const exactSchema = () => new Schema({...raw(), editorValueEncoding: 'long-decimal-string'});
const value = (): JSONObject => ({$type: 'T', id: '9007199254740993', ids: ['-9223372036854775808', '9223372036854775807', '-1', '0'], map: [{$type: '$entry', key: {$type: 'Key', id: '9007199254740993'}, value: {$type: 'I.Item', id: '9223372036854775807'}}]});

describe('editor long encoding', () => {
    it('preserves long strings in primitives, lists, map keys, struct keys and interfaces without mutating input', () => {
        const object = value();
        const before = structuredClone(object);
        expect(encodeEditorRecord(exactSchema(), table, object)).toEqual(object);
        expect(object).toEqual(before);
    });

    it('validates fractions, range and already unsafe numbers', () => {
        for (const id of ['1.5', '1e3', '9223372036854775808', '-9223372036854775809', 9007199254740992, 1.5]) {
            expect(() => encodeEditorRecord(exactSchema(), table, {$type: 'T', id})).toThrow();
        }
    });

    it('supports safe legacy numbers and blocks exact strings on legacy servers', () => {
        expect(encodeEditorRecord(exactSchema(), table, {$type: 'T', id: 42}).id).toBe('42');
        const legacy = new Schema(raw());
        expect(encodeEditorRecord(legacy, table, {$type: 'T', id: '42'}).id).toBe(42);
        expect(() => encodeEditorRecord(legacy, table, value())).toThrow('update the server');
        expect(() => encodeEditorRecord(legacy, table, {$type: 'T', id: 9007199254740992})).toThrow('unsafe long');
    });

    it('generates the next long key without Number rounding', () => {
        expect(getNextId({...table, recordIds: [{id: '9007199254740994'}]}, '9007199254740993')).toBe('9007199254740995');
        expect(getNextId(table, '9223372036854775807')).toBeNull();
    });

    it('leaves malformed structural values to the existing server schema validation', () => {
        const schema = exactSchema();
        const input = {$type: 'T', map: [null, {$type: '$entry', key: null, value: {}}, {$type: '$entry', key: null, value: {$type: 1}}]} as unknown as JSONObject;
        expect(encodeEditorRecord(schema, table, input)).toEqual(input);
    });
});

describe('server-checked editor JSON', () => {
    const checked = (jsonResult: string, table = 'T'): CheckJsonResult => ({resultCode: 'ok', table, jsonResult});

    it('rejects unsafe numbers returned by a legacy service', () => {
        for (const schema of [new Schema(raw()), exactSchema()]) {
            expect(() => parseCheckedEditorRecord(schema, 'T', checked('{"id":9007199254740993}'))).toThrow('unsafe long');
        }
    });

    it('normalizes safe long text for legacy servers throughout nested values', () => {
        const text = JSON.stringify({id: '42', ids: ['-7'], map: [{key: {id: '8'}, value: {$type: 'I.Item', id: '9'}}]});
        expect(parseCheckedEditorRecord(new Schema(raw()), 'T', checked(text))).toEqual({
            id: 42, ids: [-7], map: [{key: {id: 8}, value: {$type: 'I.Item', id: 9}}],
        });
    });

    it('keeps exact decimal strings and encodes safe numbers for updated servers', () => {
        expect(parseCheckedEditorRecord(exactSchema(), 'T', checked(JSON.stringify(value())))).toEqual(value());
        expect(parseCheckedEditorRecord(exactSchema(), 'T', checked('{"id":42}'))).toEqual({id: '42'});
    });

    it('requires an existing table and a JSON record object', () => {
        for (const text of ['null', '[]', '42', 'true', '"record"']) {
            expect(() => parseCheckedEditorRecord(exactSchema(), 'T', checked(text))).toThrow('record requires a JSON object');
        }
        expect(() => parseCheckedEditorRecord(exactSchema(), 'T', checked('{'))).toThrow();
        expect(() => parseCheckedEditorRecord(exactSchema(), 'missing', checked('{}', 'missing'))).toThrow('table missing not found');
    });

    it('propagates source-token validation errors instead of interpreting their text as data', () => {
        for (const token of ['9007199254740991.1', '42.00000000000000000001', '1e-400']) {
            expect(() => parseCheckedEditorRecord(exactSchema(), 'T', {
                resultCode: 'ParseJsonError', table: 'T', jsonResult: `T.id: invalid long ${token}`,
            })).toThrow(`invalid long ${token}`);
        }
        expect(() => parseCheckedEditorRecord(exactSchema(), 'T', checked('{}', 'other'))).toThrow('different table');
    });
});

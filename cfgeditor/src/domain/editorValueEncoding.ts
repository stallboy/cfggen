import {JSONObject, JSONValue} from '@/api/recordModel';
import {parseFieldTypeId, SStruct, STable} from '@/api/schemaModel';
import {getImpl, Schema} from './schema';
import {CheckJsonResult} from '@/api/chatModel';

/** Parse only server-checked JSON: raw number tokens must be validated before JavaScript rounds them. */
export function parseCheckedEditorRecord(schema: Schema, tableName: string, result: CheckJsonResult): JSONObject {
    if (result.resultCode !== 'ok') throw new Error(result.jsonResult || result.resultCode);
    if (result.table !== tableName) throw new Error('checked JSON belongs to a different table');
    const table = schema.getSTable(tableName);
    if (!table) throw new Error(`table ${tableName} not found`);
    const value: unknown = JSON.parse(result.jsonResult);
    if (value == null || typeof value !== 'object' || Array.isArray(value)) throw new Error('record requires a JSON object');
    return encodeEditorRecord(schema, table, value as JSONObject);
}

/** Validate before HTTP serialization; never coerce an already rounded number to text. */
export function encodeEditorRecord(schema: Schema, table: STable, object: JSONObject): JSONObject {
    const encoded = structuredClone(object);
    const exact = schema.rawSchema.editorValueEncoding === 'long-decimal-string';

    function structural(item: SStruct | STable, value: JSONObject, path: string) {
        for (const field of item.fields) {
            if (value[field.name] !== undefined) value[field.name] = fieldValue(field.type, value[field.name], `${path}.${field.name}`);
        }
    }

    function fieldValue(type: string, value: JSONValue, path: string): JSONValue {
        const parsed = parseFieldTypeId(type);
        if (parsed.kind === 'primitive' && parsed.name === 'long') {
            if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error(`${path}: unsafe long number; reload with an updated server`);
            if ((typeof value !== 'string' && typeof value !== 'number') || !/^-?[0-9]+$/.test(String(value))) throw new Error(`${path}: long requires a decimal integer`);
            const integer = BigInt(value);
            if (integer < -9223372036854775808n || integer > 9223372036854775807n) throw new Error(`${path}: long is outside the 64-bit range`);
            if (exact) return integer.toString();
            if (integer < -9007199254740991n || integer > 9007199254740991n) throw new Error(`${path}: update the server to save exact long values`);
            return Number(integer);
        }
        if (parsed.kind === 'list' && Array.isArray(value)) return value.map((entry, i) => fieldValue(parsed.item, entry, `${path}[${i}]`));
        if (parsed.kind === 'map' && Array.isArray(value)) {
            return value.map((entry, i) => {
                if (entry == null || typeof entry !== 'object' || Array.isArray(entry)) return entry;
                entry.key = fieldValue(parsed.key, entry.key, `${path}[${i}].key`);
                entry.value = fieldValue(parsed.value, entry.value, `${path}[${i}].value`);
                return entry;
            });
        }
        if (parsed.kind === 'ref' && value != null && typeof value === 'object' && !Array.isArray(value)) {
            const item = schema.getStructOrInterface(parsed.name);
            if (item?.type === 'struct') structural(item, value, path);
            if (item?.type === 'interface' && typeof value.$type === 'string') {
                const impl = getImpl(item, value.$type.replace(`${item.name}.`, ''));
                if (impl) structural(impl, value, path);
            }
        }
        return value;
    }

    structural(table, encoded, table.name);
    return encoded;
}

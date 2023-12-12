export type JSONValue =
    | string
    | number
    | boolean
    | JSONObject & Refs
    | JSONArray;

export interface JSONObject {
    [x: string]: JSONValue;

    "$type": string;
}

export interface Refs {
    "$refs"?: RefIdMap;
}

export interface RefIdMap {
    [refName: string]: RefId[];
}

export interface RefId {
    table: string;
    id: string;
}

export interface JSONArray extends Array<JSONValue> {
}

export interface TableMap {
    [table: string]: RecordMap;
}

export interface RecordMap {
    [id: string]: Refs;
}

export interface RecordResult {
    resultCode: 'ok' | 'tableNotSet' | 'idNotSet' | 'tableNotFound' | 'idFormatErr' | 'idNotFound' | 'paramErr';
    table?: string;
    id?: string;
    depth: number;
    in: boolean;
    maxObjs: number;
    object: JSONObject & Refs;
    refs: TableMap;
}
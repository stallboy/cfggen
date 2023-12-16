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

export interface BriefRecord extends Refs {
    value: string;
    depth: number;
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
    [id: string]: BriefRecord;
}

export type ResultCode =
    'ok'
    | 'tableNotSet'
    | 'idNotSet'
    | 'tableNotFound'
    | 'idParseErr'
    | 'idNotFound'
    | 'paramErr';

export interface RecordResult {
    resultCode: ResultCode;
    table?: string;
    id?: string;
    maxObjs: number;
    object: JSONObject & Refs;
    refs: TableMap;
}

export interface RecordRefsResult {
    resultCode: ResultCode;
    table?: string;
    id?: string;
    depth: number;
    in: boolean;
    maxObjs: number;
    refs: TableMap;
}

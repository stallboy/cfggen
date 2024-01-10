import {RecordId} from "./schemaModel.ts";

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

export interface BriefDescription {
    field: string;
    value: string;
    comment: string;
}

export interface BriefRecord extends Refs {
    table: string;
    id: string;

    img?: string;
    title?: string;
    descriptions?: BriefDescription[];

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
    refs: BriefRecord[];
}

export interface RecordRefsResult {
    resultCode: ResultCode;
    table?: string;
    id?: string;
    depth: number;
    in: boolean;
    maxObjs: number;
    refs: BriefRecord[];
}

export type EditResultCode =
    'addOk'
    | 'updateOk'
    | 'deleteOk'
    | 'serverNotEditable'
    | 'tableNotSet'
    | 'idNotSet'
    | 'tableNotFound'
    | 'tableNotEditable'
    | 'idParseErr'
    | 'idNotFound'
    | 'jsonParseErr'
    | 'jsonStoreErr';


export interface RecordEditResult {
    resultCode: EditResultCode;
    table: string;
    id: string;
    valueErrs: string[];
    recordIds: RecordId[];
}

import {RecordId} from "../table/schemaModel.ts";

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

export type JSONArray = Array<JSONValue>

export interface Refs {
    "$refs"?: FieldRef[];
}

export interface BriefDescription {
    field: string;
    value: string;
    comment: string;
}

export interface BriefRecord extends Refs {
    table: string;
    id: string;

    title?: string;
    descriptions?: BriefDescription[];

    value: string;
    depth: number;  // 若ref in为-1，自身为0，ref出去的1,2...
}

export interface FieldRef {
    firstField: string;
    label?: string;
    toTable: string;
    toId: string;
}

export interface RefId {
    table: string;
    id: string;
}


export interface RecordRefId {
    table: string;
    id: string;
    title: string;
    depth: number;
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
    table: string;
    id: string;
    maxObjs: number;
    object: JSONObject & Refs;  //自身详细信息
    refs: BriefRecord[];
}

export interface RecordRefsResult {
    resultCode: ResultCode;
    table: string;
    id: string;
    depth: number;
    in: boolean;
    maxObjs: number;
    refs: BriefRecord[];
}

export interface RecordRefIdsResult {
    resultCode: ResultCode;
    table: string;
    id: string;
    inDepth: number;
    outDepth: number;
    maxRefIds: number;
    recordRefIds: RecordRefId[];
}


export type EditResultCode =
    'addOk'
    | 'updateOk'
    | 'deleteOk'
    | 'serverNotEditable'
    | 'tableNotSet'
    | 'idNotSet'
    | 'tableNotFound'
    | 'idParseErr'
    | 'idNotFound'
    | 'jsonParseErr'
    | 'jsonStoreErr';

export interface RecordEditResult {
    resultCode: EditResultCode;
    table: string;
    id: string;
    valueErrs: string[];
    recordIds: RecordId[];  // 并没有用上，而是返回后直接invalid所有的cache
}

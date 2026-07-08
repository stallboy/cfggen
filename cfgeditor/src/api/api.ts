import axios from 'axios';
import { RawSchema } from "./schemaModel.ts";
import {
    JSONObject,
    RecordEditResult,
    RecordRefIdsResult,
    RecordRefsResult,
    RecordResult,
    UnreferencedRecordsResult
} from "./recordModel.ts";
import { NoteEditResult, Notes } from "./noteModel.ts";
import { CheckJsonResult, PromptResult } from "./chatModel.ts";

const DEFAULT_TIMEOUT_MS = 15000;

/**
 * 规范化 server：用户可能填入 `http://host:port`、`https://host:port`、`host:port` 或带尾随斜杠，
 * 统一剥成 `host[:port]`，避免拼出 `http://https://host` 这种非法 URL。
 * 注意：cfggen 的 `-gen server` 后端只支持 http，故这里固定用 http。
 */
function normalizeServer(server: string): string {
    return server.trim().replace(/^https?:\/\//i, '').replace(/\/+$/, '');
}

/**
 * 为给定 server 创建带默认超时与 header 的 axios 实例。
 * server 由用户动态配置，故按调用创建（axios.create 开销可忽略）。
 */
function httpClient(server: string) {
    return axios.create({
        baseURL: `http://${normalizeServer(server)}`,
        timeout: DEFAULT_TIMEOUT_MS,
        headers: { 'Content-Type': 'application/json' },
    });
}

export async function fetchSchema(server: string, signal: AbortSignal): Promise<RawSchema> {
    const { data } = await httpClient(server).get<RawSchema>('/schemas', { signal });
    return data;
}

export async function fetchRecordRefIds(server: string, tableId: string, id: string,
    refInDepth: number, refOutDepth: number, maxIds: number,
    signal: AbortSignal): Promise<RecordRefIdsResult> {
    const { data } = await httpClient(server).get<RecordRefIdsResult>('/recordRefIds', {
        params: { table: tableId, id, in: refInDepth, out: refOutDepth, maxIds },
        signal,
    });
    return data;
}

export async function fetchRecord(server: string, tableId: string, id: string, signal: AbortSignal): Promise<RecordResult> {
    const { data } = await httpClient(server).get<RecordResult>('/record', {
        params: { table: tableId, id, depth: 1 },
        signal,
    });
    return data;
}

export async function fetchRecordRefs(
    server: string,
    tableId: string,
    id: string,
    refOutDepth: number,
    maxNode: number,
    refIn: boolean,
    signal: AbortSignal
): Promise<RecordRefsResult> {
    // 后端 EditorServer.queryToMap 对 refs / in 做存在性判断（!= null），
    // 故用空串占位即可，等价于原先无值的 `&refs`、`&in`。
    const { data } = await httpClient(server).get<RecordRefsResult>('/record', {
        params: {
            table: tableId,
            id,
            depth: refOutDepth,
            maxObjs: maxNode,
            refs: '',
            ...(refIn ? { in: '' } : {}),
        },
        signal,
    });
    return data;
}

// 获取某个table下所有未被引用的记录
export async function fetchUnreferencedRecords(
    server: string,
    tableId: string,
    maxNode: number,
    signal: AbortSignal
): Promise<UnreferencedRecordsResult> {
    const { data } = await httpClient(server).get<UnreferencedRecordsResult>('/record', {
        params: { table: tableId, maxObjs: maxNode, noRefIn: '' },
        signal,
    });
    return data;
}

export async function addOrUpdateRecord(server: string, tableId: string, editingObject: JSONObject, signal?: AbortSignal): Promise<RecordEditResult> {
    const { data } = await httpClient(server).post<RecordEditResult>(
        '/recordAddOrUpdate', editingObject, { params: { table: tableId }, signal });
    return data;
}


export async function deleteRecord(server: string, tableId: string, id: string, signal?: AbortSignal): Promise<RecordEditResult> {
    const { data } = await httpClient(server).post<RecordEditResult>(
        '/recordDelete', null, { params: { table: tableId, id }, signal });
    return data;
}

export async function fetchNotes(server: string, signal: AbortSignal): Promise<Notes> {
    const { data } = await httpClient(server).get<Notes>('/notes', { signal });
    return data;
}

export async function updateNote(server: string, key: string, note: string, signal?: AbortSignal): Promise<NoteEditResult> {
    const { data } = await httpClient(server).post<NoteEditResult>(
        '/noteUpdate', note, { params: { key }, headers: { 'Content-Type': 'text/plain' }, signal });
    return data;
}

export async function getPrompt(server: string, table: string, signal: AbortSignal): Promise<PromptResult> {
    const { data } = await httpClient(server).get<PromptResult>('/prompt', {
        params: { table },
        signal,
    });
    return data;
}


export async function checkJson(server: string, tableId: string, raw: string, signal?: AbortSignal): Promise<CheckJsonResult> {
    const { data } = await httpClient(server).post<CheckJsonResult>(
        '/checkJson', raw, { params: { table: tableId }, headers: { 'Content-Type': 'text/plain' }, signal });
    return data;
}

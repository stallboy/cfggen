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


export async function fetchSchema(server: string, signal: AbortSignal): Promise<RawSchema> {
    const response = await axios.get<RawSchema>(`http://${server}/schemas`, { signal });
    return response.data;
}

export async function fetchRecordRefIds(server: string, tableId: string, id: string,
    refInDepth: number, refOutDepth: number, maxIds: number,
    signal: AbortSignal): Promise<RecordRefIdsResult> {
    const url = `http://${server}/recordRefIds?table=${tableId}&id=${id}&in=${refInDepth}&out=${refOutDepth}&maxIds=${maxIds}`;
    const response = await axios.get<RecordRefIdsResult>(url, { signal });
    return response.data;
}

export async function fetchRecord(server: string, tableId: string, id: string, signal: AbortSignal): Promise<RecordResult> {
    const url = `http://${server}/record?table=${tableId}&id=${id}&depth=1`;
    const response = await axios.get<RecordResult>(url, { signal });
    return response.data;
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
    const url = `http://${server}/record?table=${tableId}&id=${id}&depth=${refOutDepth}&maxObjs=${maxNode}&refs${refIn ? '&in' : ''}`;
    // console.log('fetch refs', tableId, id);
    const response = await axios.get<RecordRefsResult>(url, { signal });
    // console.log('fetched refs', tableId, id, response.data);
    return response.data;
}

// 获取某个table下所有未被引用的记录
export async function fetchUnreferencedRecords(
    server: string,
    tableId: string,
    maxNode: number,
    signal: AbortSignal
): Promise<UnreferencedRecordsResult> {
    const url = `http://${server}/record?table=${tableId}&maxObjs=${maxNode}&noRefIn`;
    const response = await axios.get<UnreferencedRecordsResult>(url, { signal });
    return response.data;
}

export async function addOrUpdateRecord(server: string, tableId: string, editingObject: JSONObject): Promise<RecordEditResult> {
    const url = `http://${server}/recordAddOrUpdate?table=${tableId}`;
    // console.log('add or update', tableId, editingObject);
    const response = await axios.post<RecordEditResult>(url, editingObject, {
        method: 'POST',
        headers: {
            cache: "no-cache",
            mode: "cors",
            credentials: "same-origin",
            redirect: "follow",
            referrerPolicy: "no-referrer",
            "Content-Type": "application/json",
        },
    });
    return response.data;
}


export async function deleteRecord(server: string, tableId: string, id: string): Promise<RecordEditResult> {
    const url = `http://${server}/recordDelete?table=${tableId}&id=${id}`;
    const response = await axios.post<RecordEditResult>(url, null, {
        method: 'POST',
        headers: {
            cache: "no-cache",
            mode: "cors",
            credentials: "same-origin",
            redirect: "follow",
            referrerPolicy: "no-referrer"
        }
    });
    return response.data;
}

export async function fetchNotes(server: string, signal: AbortSignal): Promise<Notes> {
    const response = await axios.get<Notes>(`http://${server}/notes`, { signal });
    return response.data;
}

export async function updateNote(server: string, key: string, note: string): Promise<NoteEditResult> {
    const url = `http://${server}/noteUpdate?key=${key}`;
    // console.log('add or update note', key, note);
    const response = await axios.post<NoteEditResult>(url, note, {
        method: 'POST',
        headers: {
            cache: "no-cache",
            mode: "cors",
            credentials: "same-origin",
            redirect: "follow",
            referrerPolicy: "no-referrer",
            "Content-Type": "text/plain",
        },
    });
    return response.data;
}

export async function getPrompt(server: string, table: string, signal: AbortSignal): Promise<PromptResult> {
    const url = `http://${server}/prompt?table=${table}`;
    const response = await axios.get<PromptResult>(url, { signal });
    return response.data;
}


export async function checkJson(server: string, tableId: string, raw: string): Promise<CheckJsonResult> {
    const url = `http://${server}/checkJson?table=${tableId}`;
    // console.log('check json', tableId, raw);
    const response = await axios.post<CheckJsonResult>(url, raw, {
        method: 'POST',
        headers: {
            cache: "no-cache",
            mode: "cors",
            credentials: "same-origin",
            redirect: "follow",
            referrerPolicy: "no-referrer",
            "Content-Type": "text/plain",
        },
    });
    return response.data;
}

import {RawSchema} from "./table/schemaModel.ts";
import {JSONObject, RecordEditResult, RecordRefsResult, RecordResult} from "./record/recordModel.ts";
import axios from 'axios';
import {Schema} from "./table/schemaUtil.ts";
import {clearLayoutCache} from "./setting/store.ts";
import {NoteEditResult, Notes, notesToMap} from "./record/noteModel.ts";
import {PromptRequest, PromptResult} from "./search/chatModel.ts";


export async function fetchSchema(server: string, signal: AbortSignal) {
    const response = await axios.get<RawSchema>(`http://${server}/schemas`, {signal});
    console.log('fetched schema');
    clearLayoutCache();
    return new Schema(response.data);
}

export async function fetchRecord(server: string, tableId: string, id: string, signal: AbortSignal) {
    const url = `http://${server}/record?table=${tableId}&id=${id}&depth=1`;
    const response = await axios.get<RecordResult>(url, {signal});
    return response.data;
}

export async function fetchRecordRefs(server: string, tableId: string, id: string,
                                      refOutDepth: number, maxNode: number, refIn: boolean,
                                      signal: AbortSignal) {
    const url = `http://${server}/record?table=${tableId}&id=${id}&depth=${refOutDepth}&maxObjs=${maxNode}&refs${refIn ? '&in' : ''}`;
    // console.log('fetch refs', tableId, id);
    const response = await axios.get<RecordRefsResult>(url, {signal});
    // console.log('fetched refs', tableId, id, response.data);
    return response.data;
}

export async function addOrUpdateRecord(server: string, tableId: string, editingObject: JSONObject) {
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


export async function deleteRecord(server: string, tableId: string, id: string) {
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

export async function fetchNotes(server: string, signal: AbortSignal) {
    const response = await axios.get<Notes>(`http://${server}/notes`, {signal});
    console.log('fetched notes');
    clearLayoutCache();
    return notesToMap(response.data);
}

export async function updateNote(server: string, key: string, note: string) {
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

export async function generatePrompt(server: string, req: PromptRequest) {
    const url = `http://${server}/prompt`;
    const response = await axios.post<PromptResult>(url, req, {
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

import {RawSchema} from "../routes/table/schemaModel.ts";
import {JSONObject, RecordEditResult, RecordRefsResult, RecordResult} from "../routes/record/recordModel.ts";
import axios from 'axios';
import {Schema} from "../routes/table/schemaUtil.ts";


export async function fetchSchema(server: string) {
    const response = await axios.get<RawSchema>(`http://${server}/schemas`);
    return new Schema(response.data);
}

export async function fetchRecord(server: string, tableId: string, id: string) {
    const url = `http://${server}/record?table=${tableId}&id=${id}&depth=1`;
    const response = await axios.get<RecordResult>(url);
    return response.data;
}

export async function fetchRecordRefs(server: string, tableId: string, id: string,
                                      refOutDepth: number, maxNode: number, refIn: boolean) {
    let url = `http://${server}/record?table=${tableId}&id=${id}&depth=${refOutDepth}&maxObjs=${maxNode}&refs${refIn ? '&in' : ''}`;
    const response = await axios.get<RecordRefsResult>(url);
    return response.data;
}

export async function addOrUpdateRecord(server: string, tableId: string, editingObject: JSONObject) {
    let url = `http://${server}/recordAddOrUpdate?table=${tableId}`;
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
    let url = `http://${server}/recordDelete?table=${tableId}&id=${id}`;
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


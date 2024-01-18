import {store} from "./store.ts";
import {RawSchema} from "./schemaModel.ts";
import {JSONObject, RecordEditResult, RecordRefsResult, RecordResult} from "./recordModel.ts";
import axios from 'axios';
import {Schema} from "./schemaUtil.ts";


export async function fetchSchema(server: string) {
    // axios.get()
    const response = await axios.get<RawSchema>(`http://${server}/schemas`);
    return new Schema(response.data);
}

export async function fetchRecord(tableId: string, id: string) {
    const {server} = store;
    const url = `http://${server}/record?table=${tableId}&id=${id}&depth=1`;
    const response = await fetch(url);
    return await response.json() as RecordResult;
}

export async function fetchRecordRefs(tableId: string, id: string,
                                      refOutDepth: number, maxNode: number, refIn: boolean) {
    const {server} = store;
    let url = `http://${server}/record?table=${tableId}&id=${id}&depth=${refOutDepth}&maxObjs=${maxNode}&refs${refIn ? '&in' : ''}`;
    const response = await fetch(url);
    return await response.json() as RecordRefsResult;
}

export async function addOrUpdateRecord(tableId: string, editingObject: JSONObject) {
    const {server} = store;

    let url = `http://${server}/recordAddOrUpdate?table=${tableId}`;
    const response = await fetch(url, {
        method: 'POST',
        cache: "no-cache",
        mode: "cors",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
        },
        redirect: "follow",
        referrerPolicy: "no-referrer",
        body: JSON.stringify(editingObject)
    });
    return await response.json() as RecordEditResult;
}

export async function deleteRecord(tableId: string, id: string) {
    const {server} = store;

    let url = `http://${server}/recordDelete?table=${tableId}&id=${id}`;
    const response = await fetch(url, {
        method: 'POST',
        cache: "no-cache",
        mode: "cors",
        credentials: "same-origin",
        redirect: "follow",
        referrerPolicy: "no-referrer"
    });
    return await response.json() as RecordEditResult;
}


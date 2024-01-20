import resso from "resso";
import {Convert, FixedPage, NodeShowType} from "../../io/localStoreJson.ts";
import {getBool, getEnumStr, getInt, getJson, getJsonNullable, getStr} from "../../io/localStore.ts";
import {History} from "../headerbar/historyModel.ts";
import {Schema} from "../table/schemaUtil.ts";
import {useLocation} from "react-router-dom";

export type DragPanelType = 'recordRef' | 'fix' | 'none';
const dragPanelEnums = ['recordRef', 'fix', 'none']

export type PageType = 'table' | 'tableRef' | 'record' | 'recordRef' | 'fix';
export const pageEnums = ['table', 'tableRef', 'record', 'recordRef', 'fix'];

export type StoreState = {
    server: string;
    maxImpl: number;

    refIn: boolean;
    refOutDepth: number;
    maxNode: number;

    recordRefIn: boolean;
    recordRefOutDepth: number;
    recordMaxNode: number;
    nodeShow: NodeShowType

    query: string;
    searchMax: number;

    dragPanel: DragPanelType;
    fix: FixedPage | null;
    imageSizeScale: number;

    history: History;
}

const defaultNodeShow: NodeShowType = {
    showHead: 'show',
    showDescription: 'show',
    containEnum: true,
    nodePlacementStrategy: 'SIMPLE',
    keywordColors: [],
    tableColors: [],
}

function readStoreState(): StoreState {
    return {
        server: getStr('server', 'localhost:3456'),
        maxImpl: getInt('maxImpl', 10),

        refIn: getBool('refIn', true),
        refOutDepth: getInt('refOutDepth', 5),
        maxNode: getInt('maxNode', 30),

        recordRefIn: getBool('recordRefIn', true),
        recordRefOutDepth: getInt('recordRefOutDepth', 5),
        recordMaxNode: getInt('recordMaxNode', 30),
        nodeShow: getJson<NodeShowType>('nodeShow', Convert.toNodeShowType, defaultNodeShow),

        query: getStr('query', ''),
        searchMax: getInt('searchMax', 50),

        dragPanel: getEnumStr<DragPanelType>('dragPanel', dragPanelEnums, 'none'),
        fix: getJsonNullable<FixedPage>('fix', Convert.toFixedPage),
        imageSizeScale: getInt('imageSizeScale', 16),

        history: new History(),
    };
}

export const store = resso<StoreState>(readStoreState());


export function setQuery(v: string) {
    store.query = v;
    localStorage.setItem('query', v);
}


export function setMaxImpl(value: number | null) {
    if (value) {
        store.maxImpl = value;
        localStorage.setItem('maxImpl', value.toString());
    }
}

export function setRefIn(checked: boolean) {
    store.refIn = checked;
    localStorage.setItem('refIn', checked ? 'true' : 'false');
}

export function setRefOutDepth(value: number | null) {
    if (value) {
        store.refOutDepth = value;
        localStorage.setItem('refOutDepth', value.toString());
    }
}

export function setMaxNode(value: number | null) {
    if (value) {
        store.maxNode = value;
        localStorage.setItem('maxNode', value.toString());
    }
}

export function setRecordRefIn(checked: boolean) {
    store.recordRefIn = checked;
    localStorage.setItem('recordRefIn', checked ? 'true' : 'false');
}

export function setRecordRefOutDepth(value: number | null) {
    if (value) {
        store.recordRefOutDepth = value;
        localStorage.setItem('recordRefOutDepth', value.toString());
    }
}

export function setRecordMaxNode(value: number | null) {
    if (value) {
        store.recordMaxNode = value;
        localStorage.setItem('recordMaxNode', value.toString());
    }
}

export function setSearchMax(value: number | null) {
    if (value) {
        store.searchMax = value;
        localStorage.setItem('searchMax', value.toString());
    }
}

export function setImageSizeScale(value: number | null) {
    if (value) {
        store.imageSizeScale = value;
        localStorage.setItem('imageSizeScale', value.toString());
    }
}

export function setDragPanel(value: DragPanelType) {
    if (dragPanelEnums.includes(value)) {
        store.dragPanel = value;
        localStorage.setItem('dragPanel', value);
    }
}


export function setFix(curTableId: string, curId: string) {
    const {recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow} = store;
    let fp: FixedPage = {
        table: curTableId,
        id: curId,
        refIn: recordRefIn,
        refOutDepth: recordRefOutDepth,
        maxNode: recordMaxNode,
        nodeShow: nodeShow,
    };
    store.fix = fp;
    localStorage.setItem('fix', Convert.fixedPageToJson(fp));
}

export function setFixNull() {
    store.fix = null;
    localStorage.removeItem('fix');
}

export function setServer(value: string) {
    store.server = value;
    localStorage.setItem('server', value);
}

export function setNodeShow(nodeShow: NodeShowType) {
    store.nodeShow = nodeShow;
    localStorage.setItem('nodeShow', Convert.nodeShowTypeToJson(nodeShow));
}

export function historyPrev(curPage: PageType) {
    const {history} = store;
    const newHistory = history.prev();
    store.history = newHistory;
    const cur = newHistory.cur();
    if (cur) {
        return navTo(curPage, cur.table, cur.id, false, false);
    }
}

export function historyNext(curPage: PageType) {
    const {history} = store;
    const newHistory = history.next();
    store.history = newHistory;
    const cur = newHistory.cur();
    if (cur) {
        return navTo(curPage, cur.table, cur.id, false, false);
    }
}


export function setSchema(schema: Schema, curTableId: string, curId: string) {
    let curTab;
    if (curTableId.length > 0) {
        curTab = schema.getSTable(curTableId);
    }
    if (curTab == null) {
        curTab = schema.getFirstSTable();

    }

    let tableId = curTableId;
    let id = '';
    if (curTab) {
        tableId = curTab.name;

        if (curId.length > 0 && schema.hasId(curTab, curId)) {
            id = curId;
        } else if (curTab.recordIds.length > 0) {
            id = curTab.recordIds[0].id;
        }
    }
    return [tableId, id];
}


export function getFixCurIdByTable(schema: Schema, curTableId: string, curId: string) {
    let id = '';
    let table = schema.getSTable(curTableId);
    if (table) {
        if (schema.hasId(table, curId)) {
            id = curId;
        } else if (table.recordIds.length > 0) {
            id = table.recordIds[0].id;
        }
    }
    return id;
}

export function navTo(curPage: PageType, tableId: string, id: string = '',
                      edit: boolean = false, addHistory: boolean = true) {
    if (addHistory) {
        const {history} = store;
        const cur = history.cur();
        if (cur == null || (cur.table != tableId || cur.id != id)) {
            store.history = history.addItem(tableId, id);
        }
    }

    const url = `/${curPage}/${tableId}/${id}`;
    return edit ? url + '/edit' : url;
}


export function useLocationData() {
    const location = useLocation();
    const pathname = location.pathname;
    const split = pathname.split('/');
    let curPage: PageType = 'table';
    let curTableId = '';
    let curId = '';

    if (split.length > 1) {
        if (pageEnums.includes(split[1])) {
            curPage = split[1] as PageType;
        }
    }
    if (split.length > 2) {
        curTableId = split[2];
    }
    if (split.length > 3) {
        curId = split[3];
    }

    let edit = false;
    if (split.length > 4) {
        edit = split[4] == 'edit';
    }
    return {curPage, curTableId, curId, edit, pathname};
}

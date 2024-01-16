import resso from "resso";
import {Convert, FixedPage, NodeShowType} from "../func/localStoreJson.ts";
import {getBool, getEnumStr, getInt, getJson, getJsonNullable, getStr} from "../func/localStore.ts";
import {History} from "./historyModel.ts";
import {Schema} from "./schemaUtil.ts";

export type DragPanelType = 'recordRef' | 'fix' | 'none';
const dragPanelEnums = ['recordRef', 'fix', 'none']

export type PageType = 'table' | 'tableRef' | 'record' | 'recordRef' | 'fix';
const pageEnums = ['table', 'tableRef', 'record', 'recordRef', 'fix'];

export type StoreState = {
    curTableId: string;
    curId: string;
    curPage: PageType;

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

    //---
    editMode: boolean;
    history: History;
    schema: Schema | null;

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
        curTableId: getStr('curTableId', ''),
        curId: getStr('curId', ''),
        curPage: getEnumStr<PageType>('curPage', pageEnums, 'record'),

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
        editMode: false,
        schema: null,
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

export function setDragPanel(value: string) {
    if (dragPanelEnums.includes(value)) {
        store.dragPanel = (value as DragPanelType);
        localStorage.setItem('dragPanel', value);
    }
}


export function setFix() {
    const {curTableId, curId, recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow} = store;
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

export function setNodeShow(nodeShow: NodeShowType) {
    store.nodeShow = nodeShow;
    localStorage.setItem('nodeShow', Convert.nodeShowTypeToJson(nodeShow));
}

export function setCurPage(page: string) {
    if (pageEnums.includes(page)) {
        store.curPage = page as PageType;
        localStorage.setItem('curPage', page);
    }
}

function setSchemaCurTableAndId(schema: Schema,
                                curTableId: string,
                                curId: string,
                                fromOp: boolean = true) {

    const {history} = store;
    store.schema = schema;

    let curTab;
    if (curTableId.length > 0) {
        curTab = schema.getSTable(curTableId);
    }
    if (curTab == null) {
        curTab = schema.getFirstSTable();
    }

    if (curTab) {
        store.curTableId = curTab.name;

        let id = '';
        if (curId.length > 0 && schema.hasId(curTab, curId)) {
            id = curId;
        } else if (curTab.recordIds.length > 0) {
            id = curTab.recordIds[0].id;
        }

        store.curId = id;
        if (fromOp) { // 如果是从prev，next中来的，就不要再设置history了
            store.history = history.addItem(curTab.name, id);
        }
        localStorage.setItem('curTableId', curTab.name);
        localStorage.setItem('curId', id);
    }
}

export function setCurTable(curTableId: string) {
    const {schema, curId} = store;
    if (schema) {
        setSchemaCurTableAndId(schema, curTableId, curId);
    }
}

export function setCurTableAndId(curTableId: string, curId: string) {
    const {schema} = store;
    if (schema) {
        setSchemaCurTableAndId(schema, curTableId, curId);
    }
}


export function setCurId(curId: string) {
    const {schema, curTableId} = store;
    if (schema) {
        setSchemaCurTableAndId(schema, curTableId, curId);
    }
}

export function historyPrev() {
    const {schema, history} = store;
    const newHistory = history.prev();
    store.history = newHistory;
    const cur = newHistory.cur();
    if (cur && schema) {
        setSchemaCurTableAndId(schema, cur.table, cur.id, false);
    }
}

export function historyNext() {
    const {schema, history} = store;
    const newHistory = history.next();
    store.history = newHistory;
    const cur = newHistory.cur();
    if (cur && schema) {
        setSchemaCurTableAndId(schema, cur.table, cur.id, false);
    }
}

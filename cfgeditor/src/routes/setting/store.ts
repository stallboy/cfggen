import resso from "resso";
import {AIConf, Convert, FixedPage, FixedPagesConf, NodeShowType, TauriConf} from "./storageJson.ts";
import {getPrefBool, getPrefEnumStr, getPrefInt, getPrefJson, getPrefStr, setPref} from "./storage.ts";
import {History} from "../headerbar/historyModel.ts";
import {Schema} from "../table/schemaUtil.ts";
import {useLocation} from "react-router-dom";
import {queryClient} from "../../main.tsx";
import {getId} from "../record/recordRefEntity.ts";
import {ResInfo} from "../../res/resInfo.ts";

export type PageType = 'table' | 'tableRef' | 'record' | 'recordRef';
export const pageEnums = ['table', 'tableRef', 'record', 'recordRef'];

export type StoreState = {
    server: string;
    aiConf: AIConf;

    maxImpl: number;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;

    recordRefIn: boolean;
    recordRefInShowLinkMaxNode: number;
    recordRefOutDepth: number;
    recordMaxNode: number;
    isNextIdShow: boolean;

    nodeShow: NodeShowType
    query: string;
    searchMax: number;
    imageSizeScale: number;

    dragPanel: string;  // 'recordRef', 'none', 'finder', 'chat', 'addJson', page.label（page的label前面的）
    pageConf: FixedPagesConf;
    tauriConf: TauriConf;

    history: History;
    isEditMode: boolean;
    resMap: Map<string, ResInfo[]>;
    resourceDir: string;
}

const storeState: StoreState = {
    server: 'localhost:3456',
    aiConf: {
        baseUrl: '',
        apiKey: '',
        model: '',
    },

    maxImpl: 10,

    refIn: true,
    refOutDepth: 5,
    maxNode: 30,

    recordRefIn: true,
    recordRefInShowLinkMaxNode: 3,
    recordRefOutDepth: 5,
    recordMaxNode: 30,
    isNextIdShow: false,

    nodeShow: {
        showHead: 'show',
        showDescription: 'show',
        containEnum: true,
        nodePlacementStrategy: 'BRANDES_KOEPF',
        keywordColors: [],
        tableHideAndColors: [],
        fieldColors: [],
    },

    query: '',
    searchMax: 50,
    imageSizeScale: 4,

    dragPanel: 'none',
    pageConf: {
        pages: [],
    },
    tauriConf: {
        resDirs: [],
        assetDir: '',
        assetRefTable: '',
    },

    history: new History(),
    isEditMode: false,

    resMap: new Map<string, ResInfo[]>(),
    resourceDir: '',
};

let prefKeySet: Set<string> | undefined;
let prefSelfKeySet: Set<string> = new Set<string>(['curPage', 'curTableId', 'curId', 'query', 'isEditMode',
    'imageSizeScale', 'dragPanel']);

export function getPrefKeySet(): Set<string> {
    if (prefKeySet === undefined) {
        prefKeySet = new Set<string>(Object.keys(storeState));
        prefKeySet.delete('query');

        prefKeySet.delete('imageSizeScale');
        prefKeySet.delete('dragPanel');

        prefKeySet.delete('history');
        prefKeySet.delete('isEditMode');
        prefKeySet.delete('resMap');
        prefKeySet.delete('resourceDir');
    }
    return prefKeySet;
}

export function getPrefSelfKeySet(): Set<string> {
    return prefSelfKeySet;
}

let alreadyRead = false;

export function readStoreStateOnce() {
    if (alreadyRead) {
        return;
    }
    alreadyRead = true;
    console.log('read storage')
    for (const k in storeState) {
        const key = k as keyof StoreState;
        const value = storeState[key]
        switch (key) {
            case 'nodeShow':
                // eslint-disable-next-line no-case-declarations
                const ns = getPrefJson<NodeShowType>('nodeShow', Convert.toNodeShowType);
                if (ns) {
                    store.nodeShow = ns;
                }
                break;
            case 'aiConf':
                const ac = getPrefJson<AIConf>('aiConf', Convert.toAIConf);
                if (ac) {
                    store.aiConf = ac;
                }
                break;
            case 'pageConf':
                // eslint-disable-next-line no-case-declarations
                const pc = getPrefJson<FixedPagesConf>('pageConf', Convert.toFixedPagesConf);
                if (pc) {
                    store.pageConf = pc;
                }
                break;
            case 'tauriConf':
                // eslint-disable-next-line no-case-declarations
                const tc = getPrefJson<TauriConf>('tauriConf', Convert.toTauriConf);
                if (tc) {
                    store.tauriConf = tc;
                }
                break;
            default:
                switch (typeof value) {
                    case "boolean":
                        store(key, () => getPrefBool(key, value));
                        break;
                    case "number":
                        store(key, () => getPrefInt(key, value));
                        break;
                    case "string":
                        store(key, () => getPrefStr(key, value));
                        break;
                    default:
                        break;
                }
                break;
        }
    }
}

export const store = resso<StoreState>(storeState);


export function clearLayoutCache() {
    queryClient.removeQueries({queryKey: ['layout']});
}

export function invalidateAllQueries() {
    queryClient.invalidateQueries({queryKey: [], refetchType: 'all'}).catch((reason: unknown) => {
        console.log(reason);
    });
}

export function setQuery(v: string) {
    store.query = v;
    setPref('query', v);
    clearLayoutCache();
}


export function setMaxImpl(value: number | null) {
    if (value) {
        store.maxImpl = value;
        setPref('maxImpl', value.toString());
        clearLayoutCache();
    }
}

export function setRefIn(checked: boolean) {
    store.refIn = checked;
    setPref('refIn', checked ? 'true' : 'false');
    clearLayoutCache();
}

export function setRefOutDepth(value: number | null) {
    if (value) {
        store.refOutDepth = value;
        setPref('refOutDepth', value.toString());
        clearLayoutCache();
    }
}

export function setMaxNode(value: number | null) {
    if (value) {
        store.maxNode = value;
        setPref('maxNode', value.toString());
        clearLayoutCache();
    }
}

export function setRecordRefIn(checked: boolean) {
    store.recordRefIn = checked;
    setPref('recordRefIn', checked ? 'true' : 'false');
    clearLayoutCache();
}

export function setRecordRefInShowLinkMaxNode(value: number | null) {
    if (value) {
        store.recordRefInShowLinkMaxNode = value;
        setPref('recordRefInShowLinkMaxNode', value.toString());
        clearLayoutCache();
    }
}

export function setRecordRefOutDepth(value: number | null) {
    if (value) {
        store.recordRefOutDepth = value;
        setPref('recordRefOutDepth', value.toString());
        clearLayoutCache();
    }
}

export function setRecordMaxNode(value: number | null) {
    if (value) {
        store.recordMaxNode = value;
        setPref('recordMaxNode', value.toString());
        clearLayoutCache();
    }
}

export function setIsNextIdShow(checked: boolean) {
    store.isNextIdShow = checked;
    setPref('isNextIdShow', checked ? 'true' : 'false');
}

export function setSearchMax(value: number | null) {
    if (value) {
        store.searchMax = value;
        setPref('searchMax', value.toString());
    }
}

export function setImageSizeScale(value: number | null) {
    if (value) {
        store.imageSizeScale = value;
        setPref('imageSizeScale', value.toString());
    }
}

export function setDragPanel(value: string) {
    store.dragPanel = value;
    setPref('dragPanel', value);
}

export function makeFixedPage(curTableId: string, curId: string) {
    const {recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow} = store;
    const fp: FixedPage = {
        label: getId(curTableId, curId),
        table: curTableId,
        id: curId,
        refIn: recordRefIn,
        refOutDepth: recordRefOutDepth,
        maxNode: recordMaxNode,
        nodeShow: nodeShow,
    };
    return fp;
}

export function setFixedPagesConf(pageConf: FixedPagesConf) {
    store.pageConf = pageConf;
    setPref('pageConf', Convert.fixedPagesConfToJson(pageConf));
    clearLayoutCache();
}

export function getFixedPage(pageConf: FixedPagesConf, label: string) {
    for (const page of pageConf.pages) {
        if (page.label == label) {
            return page;
        }
    }
}

export function setServer(value: string) {
    store.server = value;
    setPref('server', value);
}

export function setNodeShow(nodeShow: NodeShowType) {
    store.nodeShow = nodeShow;
    setPref('nodeShow', Convert.nodeShowTypeToJson(nodeShow));
    clearLayoutCache();
}

export function setTauriConf(tauriConf: TauriConf) {
    store.tauriConf = tauriConf;
    setPref('tauriConf', Convert.tauriConfToJson(tauriConf));
    clearLayoutCache();
}

export function setAIConf(aiConf: AIConf) {
    store.aiConf = aiConf;
    setPref('aiConf', Convert.aIConfToJson(aiConf));
}

export function historyPrev(curPage: PageType, history: History, isEditMode: boolean) {
    const newHistory = history.prev();
    store.history = newHistory;
    const cur = newHistory.cur();
    if (cur) {
        return navTo(curPage, cur.table, cur.id, isEditMode, false);
    }
}

export function historyNext(curPage: PageType, history: History, isEditMode: boolean) {
    const newHistory = history.next();
    store.history = newHistory;
    const cur = newHistory.cur();
    if (cur) {
        return navTo(curPage, cur.table, cur.id, isEditMode, false);
    }
}

export function getLastOpenIdByTable(schema: Schema, curTableId: string): string | undefined {
    const {history} = store;
    const lastOpenId = history.findLastOpenId(curTableId)
    const table = schema.getSTable(curTableId);
    let id;
    if (table) {
        if (lastOpenId && schema.hasId(table, lastOpenId)) {
            id = lastOpenId;
        } else if (table.recordIds.length > 0) {
            id = table.recordIds[0].id;
        }
    }
    return id;
}

export function setIsEditMode(isEditMode: boolean) {
    store.isEditMode = isEditMode;
    setPref('isEditMode', isEditMode ? 'true' : 'false');
}

export function navTo(curPage: PageType, tableId: string, id: string,
                      edit: boolean = false, addHistory: boolean = true) {
    const {history} = store;

    if (addHistory) {
        const cur = history.cur();
        if (cur == undefined || (cur.table != tableId || cur.id != id)) {
            store.history = history.addItem(tableId, id);
        }
    }

    setPref('curPage', curPage);
    setPref('curTableId', tableId);
    setPref('curId', id);

    const url = `/${curPage}/${tableId}/${id}`;
    return (curPage == 'record' && edit) ? '/edit' + url : url;
}

export function getLastNavToInLocalStore() {
    const page = getPrefEnumStr<PageType>('curPage', pageEnums);
    const tableId = getPrefStr('curTableId', '');
    const id = getPrefStr('curId', '');
    const isEditMode = getPrefBool('isEditMode', false);
    return navTo(page ?? 'table', tableId, id, isEditMode);
}

export function useLocationData() {
    const location = useLocation();
    const pathname = location.pathname;
    const split = pathname.split('/');
    let curPage: PageType = 'table';
    let curTableId = '';
    let curId = '';
    let edit = false;

    let idx = 2;
    if (split.length > 1) {
        if (split[1] == 'edit') {
            edit = true;
            if (split.length > 2 && split[2] == 'record') {
                curPage = 'record';
                idx = 3;
            }
        } else if (pageEnums.includes(split[1])) {
            curPage = split[1] as PageType;
        }
    }
    if (split.length > idx) {
        curTableId = split[idx];
        idx++;
    }
    if (split.length > idx) {
        curId = split.slice(idx).join("/");
    }
    return {curPage, curTableId, curId, edit, pathname};
}

import resso from "./resso.ts";
import {AIConf, Convert, FixedPage, FixedPagesConf, NodeShowType, TauriConf, ThemeConfig} from "./storageJson.ts";
import {getPrefBool, getPrefEnumStr, getPrefInt, getPrefJson, getPrefStr, setPref} from "./storage.ts";
import {History} from "../headerbar/historyModel.ts";
import {Schema} from "../table/schemaUtil.tsx";
import {useLocation} from "react-router-dom";
import {queryClient} from "../../main.tsx";
import {getId} from "../record/recordRefEntity.ts";
import {ResInfo} from "../../res/resInfo.ts";

export type PageType = 'table' | 'tableRef' | 'record' | 'recordRef';
export const pageEnums = ['table', 'tableRef', 'record', 'recordRef'];

export type StoreState = {
    server: string;
    aiConf: AIConf;
    themeConfig: ThemeConfig;

    maxImpl: number;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;

    recordRefIn: boolean;
    recordRefInShowLinkMaxNode: number;
    recordRefOutDepth: number;
    recordMaxNode: number;

    isNextIdShow: boolean;

    refIdsInDepth: number;
    refIdsOutDepth: number;
    refIdsMaxNode: number;

    nodeShow: NodeShowType
    query: string;
    searchMax: number;
    imageSizeScale: number;

    dragPanel: string;  // 'recordRef', 'none', 'finder', 'chat', 'addJson', page.label（page的label前面的）
    pageConf: FixedPagesConf;
    tauriConf: TauriConf;

    history: History;
    isEditMode: boolean;
    editingCurTable: string;
    editingCurId: string;
    editingIsEdited: boolean;

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
    themeConfig: {
        themeFile: '',
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

    refIdsInDepth: 2,
    refIdsOutDepth: 2,
    refIdsMaxNode: 15,

    nodeShow: {
        recordLayout: 'BRANDES_KOEPF',
        editLayout: 'mrtree',
        refLayout: 'BRANDES_KOEPF',

        tableLayout: 'BRANDES_KOEPF',
        tableRefLayout: 'BRANDES_KOEPF',

        nodeColorsByValue: [],
        nodeColorsByLabel: [],
        fieldColorsByName: [],
        editFoldColor: '#ffd6e7',

        refTableHides: [],
        refIsShowCopyable: false,
        refShowDescription: 'show',
        refContainEnum: true,
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

    isEditMode: false,
    editingCurTable: '',
    editingCurId: '',
    editingIsEdited: false,

    history: new History(),
    resMap: new Map<string, ResInfo[]>(),
    resourceDir: '',
};

let prefKeySet: Set<string> | undefined;
const prefSelfKeySet: Set<string> = new Set<string>(['curPage', 'curTableId', 'curId', 'query', 'isEditMode',
    'imageSizeScale', 'dragPanel']);

const notSaveKeySet = new Set<string>(['history', 'resMap', 'resourceDir',
    'editingCurTable', 'editingCurId', 'editingIsEdited']);

export function getPrefKeySet(): Set<string> {
    if (prefKeySet === undefined) {
        prefKeySet = new Set<string>(Object.keys(storeState));
        for (const sk of prefSelfKeySet) {
            prefKeySet.delete(sk);
        }
        for (const nk of notSaveKeySet) {
            prefKeySet.delete(nk);
        }
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
    // console.log('read storage')
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
            case 'themeConfig':
                // eslint-disable-next-line no-case-declarations
                const theme = getPrefJson<ThemeConfig>('themeConfig', Convert.toThemeConfig);
                if (theme) {
                    store.themeConfig = theme;
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

const store = resso<StoreState>(storeState);


export function useMyStore() {
    return store;
}

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

export function setRefIdsInDepth(value: number | null) {
    if (value) {
        store.refIdsInDepth = value;
        setPref('refIdsInDepth', value.toString());
    }
}

export function setRefIdsOutDepth(value: number | null) {
    if (value) {
        store.refIdsOutDepth = value;
        setPref('refIdsOutDepth', value.toString());
    }
}

export function setRefIdsMaxNode(value: number | null) {
    if (value) {
        store.refIdsMaxNode = value;
        setPref('refIdsMaxNode', value.toString());
    }
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
    const {recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow} = useMyStore();
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

export function setFixedPagesConf(newPageConf: FixedPagesConf) {
    store.pageConf = newPageConf;
    setPref('pageConf', Convert.fixedPagesConfToJson(newPageConf));
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

export function setThemeConfig(themeConfig: ThemeConfig) {
    store.themeConfig = themeConfig;
    setPref('themeConfig', Convert.themeConfigToJson(themeConfig));
}

export function historyCanPrev(curTableId: string, curId: string, history: History): boolean {
    let cur = history.cur();
    if (cur && (cur.table != curTableId || cur.id != curId)) {
        return true;
    }
    return history.canPrev();
}

export function historyPrev(curPage: PageType, curTableId: string, curId: string,
                            history: History, isEditMode: boolean) {
    let cur = history.cur();
    if (cur && (cur.table != curTableId || cur.id != curId)) {
        // 点击<关联数据>，<访问历史>里的链接时，不会修改访问历史。
        // 此时，如果看的页面已经不同于历史中的当前页面，点击回退优先跳回到当前页面。
        return navTo(curPage, cur.table, cur.id, isEditMode, false);
    }

    const newHistory = history.prev();
    store.history = newHistory;
    cur = newHistory.cur();
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

export function setEditingState(editingCurTable: string, editingCurId: string, editingIsEdited: boolean) {
    store.editingCurTable = editingCurTable;
    store.editingCurId = editingCurId;
    store.editingIsEdited = editingIsEdited;
}

export function getLastOpenIdByTable(schema: Schema, curTableId: string): string | undefined {
    const {history} = useMyStore();
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
    const {history} = useMyStore();

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

export function setResourceDir(resourceDir:string) {
    store.resourceDir = resourceDir;
}

export function setResMap(resMap: Map<string, ResInfo[]>) {
    store.resMap = resMap;
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

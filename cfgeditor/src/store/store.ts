import resso from "./resso.ts";
import {
    AIConf,
    Convert,
    FixedPagesConf,
    NodeShowType,
    TauriConf,
    ThemeConfig
} from "@/domain/storageJson";
import {
    getPrefBool,
    getPrefInt,
    getPrefJson,
    getPrefStr,
    registerPrefKeySet,
    setPref
} from "./storage.ts";
import {History} from "@/domain/historyModel";
import {ResInfo} from "@/domain/resInfo";
import {removeAllQueryCache} from "@/services/queryClient.ts";

// 导航/history 簇（navTo/useLocationData/FixedPage 工厂守卫等）拆至 navigation.ts；
// 此处 re-export 保持既有导入方零改动
export * from './navigation.ts';

export const pageEnums = ['table', 'tableRef', 'record', 'recordRef', 'recordUnref'] as const;
export type PageType = typeof pageEnums[number];

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

    dragPanel: string;  // 'recordRef', 'setting', 'none', 'finder', 'add',  page.label（page的label前面的）
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

// 持久化分类按「声明分组」决定（结构强保证）：新增 store key 必须归入下列某一组，强制开发者决定
// 它的持久化语义。三个分类 Set 从分组派生（见下方 prefKeySet / prefSelfKeySet），不再手维护——
// 原本「手维护三个独立 Set」曾导致新 key 默认落进 prefKeySet → 写入共享 cfgeditor.yml，
// 对含 apiKey 的 aiConf 是真实泄漏风险。
//   sharedPrefState → cfgeditor.yml（团队共享）
//   selfPrefState   → cfgeditorSelf.yml（个人/敏感，aiConf 含 apiKey）
//   sessionState    → 不持久化（运行时）
// 另：路由派生的 localStorage key（curPage/curTableId/curId）不在 StoreState，由 navTo 直接 setPref 写、归 self。

// → cfgeditor.yml（团队共享）
const sharedPrefState = {
    server: 'localhost:3456',
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

        // NEW: Flow visualization defaults
        nodeWidth: 240,
        editNodeWidth: 280,
        edgeColor: '#0898b5',
        edgeStrokeWidth: 3,
        mrtreeSpacing: 100,
        layeredSpacing: 60,
        layeredNodeSpacing: 80,
        // NEW: Node color defaults
        nodeColor: '#0898b5',
        nodeRefColor: '#207b4a',
        nodeRef2Color: '#006d75',
        nodeRefInColor: '#003eb3',
    } satisfies NodeShowType,

    searchMax: 50,
    pageConf: {
        pages: [],
    },
    tauriConf: {
        resDirs: [],
        assetDir: '',
        assetRefTable: '',
    },
};

// → cfgeditorSelf.yml（个人/敏感，aiConf 含 apiKey）
// isEditMode 的共享默认值：session 初始态与 getLastNavToInLocalStore 的恢复默认都取它，保持一致
export const DEFAULT_IS_EDIT_MODE = true;
const selfPrefState = {
    aiConf: {
        baseUrl: 'https://api.deepseek.com/chat/completions',
        apiKey: '',
        model: 'deepseek-v4-flash',
    },
    query: '',
    isEditMode: DEFAULT_IS_EDIT_MODE,
    imageSizeScale: 4,
    dragPanel: 'none',
};

// 不持久化（运行时）
const sessionState = {
    history: new History(),
    resMap: new Map<string, ResInfo[]>(),
    resourceDir: '',
    editingCurTable: '',
    editingCurId: '',
    editingIsEdited: false,
};

// 三组声明合并为完整 storeState；TS 在此强制：StoreState 的每个 key 都必须由某组提供
// （漏归组 = 编译报错），分类则由「在哪个组」决定。
const storeState: StoreState = {...sharedPrefState, ...selfPrefState, ...sessionState};

// 三个分类 Set 从上方声明分组派生（结构强保证：分组即分类）。
const prefKeySet = new Set<string>(Object.keys(sharedPrefState));
// prefSelfKeySet = 个人 store keys + 路由派生的 localStorage key（curPage/curTableId/curId 不在 StoreState，由 navTo 直接 setPref 写）
const prefSelfKeySet = new Set<string>([...Object.keys(selfPrefState), 'curPage', 'curTableId', 'curId']);

export function getPrefKeySet(): Set<string> {
    return prefKeySet;
}

export function getPrefSelfKeySet(): Set<string> {
    return prefSelfKeySet;
}

// 把持久化键集注册给 storage，消除 storage→store 反向依赖（模块加载时 storeState 已就绪）
registerPrefKeySet(getPrefKeySet(), getPrefSelfKeySet());

let alreadyRead = false;

// JSON 型 pref key → quicktype Convert 解析表：五个 key 同一形状（getPrefJson 成功才覆盖 store），
// 表驱动后新增 JSON 型 key 只需加一行表项，其余 key 走下方 typeof 分支
type JsonPrefKey = 'nodeShow' | 'aiConf' | 'pageConf' | 'tauriConf' | 'themeConfig';
const jsonPrefParsers: Record<JsonPrefKey, (jsonStr: string) => StoreState[JsonPrefKey]> = {
    nodeShow: Convert.toNodeShowType,
    aiConf: Convert.toAIConf,
    pageConf: Convert.toFixedPagesConf,
    tauriConf: Convert.toTauriConf,
    themeConfig: Convert.toThemeConfig,
};

function isJsonPrefKey(key: string): key is JsonPrefKey {
    return key in jsonPrefParsers;
}

export function readStoreStateOnce() {
    if (alreadyRead) {
        return;
    }
    alreadyRead = true;
    // console.log('read storage')
    for (const k in storeState) {
        const key = k as keyof StoreState;
        const value = storeState[key]
        if (isJsonPrefKey(key)) {
            const parsed = getPrefJson(key, jsonPrefParsers[key]);
            if (parsed) {
                store(key, () => parsed);
            }
            continue;
        }
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
    }
}

const store = resso<StoreState>(storeState);


export function useMyStore() {
    return store;
}

export function getMyStore() {
    return store;
}

export function setQuery(v: string) {
    store.query = v;
    setPref('query', v);
    // query 仅用于节点高亮（FlowNode/EntityProperties/EntityCard），不进入 elk 布局输入，
    // 无需清 layout 缓存——否则每次搜索都会让所有可见图在 worker 里重跑布局并得到相同结果。
}


// 拓扑相关 setter：不再 clearLayoutCache——这些 setting 已纳入 useEntityToGraph 的 layout
// queryKey（topologyKeys），改值时缓存自然失效重布局。store 重新变纯状态容器（Query Key Factory）。
type NumPrefKey = {[K in keyof StoreState]: StoreState[K] extends number ? K : never}[keyof StoreState];
type BoolPrefKey = {[K in keyof StoreState]: StoreState[K] extends boolean ? K : never}[keyof StoreState];

// 数字 pref setter 工厂：value 为 null（输入框清空）时不动作，否则写 store + 持久化
function numPrefSetter(key: NumPrefKey) {
    return (value: number | null): void => {
        if (value !== null) {
            store[key] = value;
            setPref(key, value.toString());
        }
    };
}

// 布尔 pref setter 工厂：写 store + 持久化为 'true'/'false'
function boolPrefSetter(key: BoolPrefKey) {
    return (checked: boolean): void => {
        store[key] = checked;
        setPref(key, checked ? 'true' : 'false');
    };
}

export const setMaxImpl = numPrefSetter('maxImpl');
export const setRefIn = boolPrefSetter('refIn');
export const setRefOutDepth = numPrefSetter('refOutDepth');
export const setMaxNode = numPrefSetter('maxNode');
export const setRecordRefIn = boolPrefSetter('recordRefIn');
export const setRecordRefInShowLinkMaxNode = numPrefSetter('recordRefInShowLinkMaxNode');
export const setRecordRefOutDepth = numPrefSetter('recordRefOutDepth');
export const setRecordMaxNode = numPrefSetter('recordMaxNode');
export const setIsNextIdShow = boolPrefSetter('isNextIdShow');
export const setRefIdsInDepth = numPrefSetter('refIdsInDepth');
export const setRefIdsOutDepth = numPrefSetter('refIdsOutDepth');
export const setRefIdsMaxNode = numPrefSetter('refIdsMaxNode');
export const setSearchMax = numPrefSetter('searchMax');
export const setImageSizeScale = numPrefSetter('imageSizeScale');

export function setDragPanel(value: string) {
    store.dragPanel = value;
    setPref('dragPanel', value);
}

// 内置面板（非用户自定义 fixed page）：切换 pageConf 时不参与"引用了已删除页面"的校验。
// 与 dragPanel 注释及 HeaderBar 面板菜单保持一致（'add' = AddPanel，聚合 AI/JSON）
const BUILTIN_PANELS: readonly string[] = ['none', 'recordRef', 'finder', 'add', 'setting'];

export function setFixedPagesConf(newPageConf: FixedPagesConf) {
    // 若当前 dragPanel 指向已被删除的用户自定义页面，则回退到 'none'
    const currentDragPanel = store.dragPanel;
    if (currentDragPanel && !BUILTIN_PANELS.includes(currentDragPanel)) {
        const pageExists = newPageConf.pages.some(page => page.label === currentDragPanel);
        if (!pageExists) {
            store.dragPanel = 'none';
            setPref('dragPanel', 'none');
        }
    }

    store.pageConf = newPageConf;
    setPref('pageConf', Convert.fixedPagesConfToJson(newPageConf));
    // pageConf 不改当前路由的 layout 输入（固定页各自有独立 pathname → 独立 layout query），无需清缓存。
}

export function setServer(value: string) {
    store.server = value;
    setPref('server', value);
    // server 改了（含「重连当前 server」「换库」）：清空全部缓存强制重取。
    // 所有 queryKey 都不含 server（queryFn 直接闭包捕获 store.server），不清的话换库后旧库数据
    // 会赖在缓存里直到 staleTime（schema 5min / record 30s）过期才刷新——期间显示错库数据。
    removeAllQueryCache();
}

export function setNodeShow(nodeShow: NodeShowType) {
    store.nodeShow = nodeShow;
    setPref('nodeShow', Convert.nodeShowTypeToJson(nodeShow));
    // 不清 layout 缓存：布局相关字段已由 useEntityToGraph 的 pickLayoutKeys 进 queryKey——改这些字段时
    // queryKey 变 → 缓存自然失效；改纯颜色字段 queryKey 不变 → 命中缓存不重跑 ELK。
}

export function setTauriConf(tauriConf: TauriConf) {
    store.tauriConf = tauriConf;
    setPref('tauriConf', Convert.tauriConfToJson(tauriConf));
    // tauriConf 已纳入 layout queryKey（topologyKeys），改值时缓存自然失效，无需手动清。
}

export function setAIConf(aiConf: AIConf) {
    store.aiConf = aiConf;
    setPref('aiConf', Convert.aIConfToJson(aiConf));
}

export function setThemeConfig(themeConfig: ThemeConfig) {
    store.themeConfig = themeConfig;
    setPref('themeConfig', Convert.themeConfigToJson(themeConfig));
}

export function setEditingState(editingCurTable: string, editingCurId: string, editingIsEdited: boolean) {
    store.editingCurTable = editingCurTable;
    store.editingCurId = editingCurId;
    store.editingIsEdited = editingIsEdited;
}

export const setIsEditMode = boolPrefSetter('isEditMode');

export function setResourceDir(resourceDir: string) {
    store.resourceDir = resourceDir;
}

export function setResMap(resMap: Map<string, ResInfo[]>) {
    store.resMap = resMap;
}

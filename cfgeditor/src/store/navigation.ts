import {useLocation} from "react-router";
import {FixedPage, FixedPagesConf, FixedRefPage, FixedUnrefPage} from "@/domain/storageJson";
import {History} from "@/domain/historyModel";
import {NEW_RECORD_ID, Schema} from "@/domain/schema";
import {getPrefBool, getPrefEnumStr, getPrefStr, setPref} from "./storage.ts";
import {DEFAULT_IS_EDIT_MODE, getMyStore, pageEnums, PageType} from "./store.ts";

// 导航/history 簇（从 store.ts 拆出）：路由解析、页面跳转、访问历史、FixedPage 工厂与守卫。
// 只依赖 store 实例（getMyStore，函数体内取用，避免模块初始化顺序问题）+ storage + domain。
// store.ts 顶部 `export * from './navigation.ts'` 保持既有导入方不变。

export type PageRecordOrRecordRef = 'record' | 'recordRef';

// 类型守卫函数
export function isFixedRefPage(page: FixedPage): page is FixedRefPage {
    return 'id' in page;
}

export function isFixedUnrefPage(page: FixedPage): page is FixedUnrefPage {
    return !('id' in page);
}

export function makeFixedPage(curTableId: string, curId: string): FixedRefPage {
    const { recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow } = getMyStore();
    return {
        label: `${curTableId}_${curId}`,
        table: curTableId,
        id: curId,
        refIn: recordRefIn,
        refOutDepth: recordRefOutDepth,
        maxNode: recordMaxNode,
        nodeShow: nodeShow,
    };
}

export function makeUnrefPage(curTableId: string): FixedUnrefPage {
    const { recordRefOutDepth, recordMaxNode, nodeShow } = getMyStore();
    return {
        label: `unref:${curTableId}`,
        table: curTableId,
        refOutDepth: recordRefOutDepth,
        maxNode: recordMaxNode,
        nodeShow: nodeShow,
    };
}

export function getFixedPage(pageConf: FixedPagesConf, label: string) {
    for (const page of pageConf.pages) {
        if (page.label == label) {
            return page;
        }
    }
}

export function historyCanPrev(curTableId: string, curId: string, history: History): boolean {
    const cur = history.cur();
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
    getMyStore().history = newHistory;
    cur = newHistory.cur();
    if (cur) {
        return navTo(curPage, cur.table, cur.id, isEditMode, false);
    }
}

export function historyNext(curPage: PageType, history: History, isEditMode: boolean) {
    const newHistory = history.next();
    getMyStore().history = newHistory;
    const cur = newHistory.cur();
    if (cur) {
        return navTo(curPage, cur.table, cur.id, isEditMode, false);
    }
}

export function getLastOpenIdByTable(schema: Schema, curTableId: string): string | undefined {
    const { history } = getMyStore();
    const lastOpenId = history.findLastOpenId(curTableId)
    const table = schema.getSTable(curTableId);
    let id;
    if (table) {
        if (lastOpenId && schema.hasId(table, lastOpenId)) {
            id = lastOpenId;
        } else if (table.recordIds.length > 0) {
            id = table.recordIds[0].id;
        } else {
            id = NEW_RECORD_ID;
        }
    }
    return id;
}

export function navTo(curPage: PageType, tableId: string, id: string,
    edit: boolean = false, addHistory: boolean = true) {
    const { history } = getMyStore();

    if (addHistory) {
        const cur = history.cur();
        if (cur == undefined || (cur.table != tableId || cur.id != id)) {
            getMyStore().history = history.addItem(tableId, id);
        }
    }

    setPref('curPage', curPage);
    setPref('curTableId', tableId);
    setPref('curId', id);

    const url = `/${curPage}/${tableId}/${id}`;
    return (curPage == 'record' && edit) ? '/edit' + url : url;
}

export function getLastNavToInLocalStore(): string | undefined {
    const page = getPrefEnumStr<PageType>('curPage', pageEnums);
    const tableId = getPrefStr('curTableId', '');
    // 无有效历史（全新 localStorage）时返回 undefined：否则得到 /record// 空表地址，
    // 路由不匹配落 * → PathNotFound，且"返回首页"又会被导回形成死循环；
    // 调用方应不跳转，留在首页让用户选表
    if (tableId.length == 0) {
        return undefined;
    }
    const id = getPrefStr('curId', '');
    // isEditMode 缺失时的默认与 selfPrefState 的 session 默认一致（true：编辑是产品核心路径）；
    // 全新首装 tableId 缺失已在上方提前返回 undefined，走不到这里，所以 true 不影响首装行为
    const isEditMode = getPrefBool('isEditMode', DEFAULT_IS_EDIT_MODE);
    return navTo(page ?? 'record', tableId, id, isEditMode);
}

export function useLocationData() {
    const location = useLocation();
    const pathname = location.pathname;
    const split = pathname.split('/');
    let curPage: PageType = 'record';
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
        } else {
            // as const 元组的 includes 不收窄类型，用 find 收窄以消掉 as PageType
            const page = pageEnums.find((p) => p === split[1]);
            if (page) {
                curPage = page;
            }
        }
    }
    if (split.length > idx) {
        curTableId = split[idx];
        idx++;
    }
    if (split.length > idx) {
        curId = split.slice(idx).join("/");
    }
    return { curPage, curTableId, curId, edit, pathname };
}


export function useCurPageRecordOrRecordRef(): { curPage: PageRecordOrRecordRef } {
    // 由 useLocationData 一行派生：只有 recordRef 页返回 recordRef，其余（含 table/tableRef/edit）都归 record
    const { curPage } = useLocationData();
    return { curPage: curPage === 'recordRef' ? 'recordRef' : 'record' };
}

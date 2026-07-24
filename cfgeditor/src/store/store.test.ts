import {beforeEach, describe, expect, it} from 'vitest';
import {History} from '@/domain/historyModel';
import {getLastNavToInLocalStore, getMyStore, getPrefKeySet, getPrefSelfKeySet, navTo} from './store';

// 锁住持久化键集分类（结构强保证：分类从 sharedPrefState/selfPrefState/sessionState 三组声明派生）。
// 核心不变式：aiConf（含 apiKey）必须归个人文件 cfgeditorSelf.yml，绝不能进共享 cfgeditor.yml——
// 这正是键集「按声明分组」重构要防的泄漏。新增 store key 若错归组，此处立即失败。
describe('store 持久化键集分类', () => {
    it('aiConf 归个人 self（不得进共享 yml，防 apiKey 泄漏）', () => {
        expect(getPrefSelfKeySet().has('aiConf')).toBe(true);
        expect(getPrefKeySet().has('aiConf')).toBe(false);
    });

    it('团队配置（server/nodeShow/pageConf/tauriConf 等）归共享', () => {
        for (const k of ['server', 'nodeShow', 'pageConf', 'tauriConf', 'maxImpl', 'themeConfig']) {
            expect(getPrefKeySet().has(k)).toBe(true);
            expect(getPrefSelfKeySet().has(k)).toBe(false);
        }
    });

    it('session 态不持久化（既不共享也不个人）', () => {
        for (const k of ['history', 'resMap', 'resourceDir',
            'editingCurTable', 'editingCurId', 'editingIsEdited']) {
            expect(getPrefKeySet().has(k)).toBe(false);
            expect(getPrefSelfKeySet().has(k)).toBe(false);
        }
    });

    it('路由派生 key（curPage/curTableId/curId）归个人 self', () => {
        for (const k of ['curPage', 'curTableId', 'curId']) {
            expect(getPrefSelfKeySet().has(k)).toBe(true);
        }
    });

    it('prefKeySet 与 prefSelfKeySet 不相交（一个 key 不会同时进两份 yml）', () => {
        const shared = getPrefKeySet();
        for (const k of getPrefSelfKeySet()) {
            expect(shared.has(k)).toBe(false);
        }
    });
});

// 路由纯逻辑：navTo（URL 生成 + 写 localStorage）与 getLastNavToInLocalStore（从 localStorage 重建）。
// 二者是「localStorage 三 key ↔ URL 串」的互逆映射，覆盖 record/table/recordRef/recordUnref 各 page、
// /edit 前缀触发条件（仅 record 且 isEditMode=true）、含 / 的 id 透传、缺省/非法值回退。
// 注：useLocationData 是 react-router hook（依赖组件树），且 devDependencies 无 @testing-library/react，
// 无法在测试环境直接 renderHook，故不覆盖——见任务报告。
describe('store 路由纯逻辑', () => {
    beforeEach(() => {
        localStorage.clear();
        getMyStore().history = new History();
    });

    describe('navTo', () => {
        it('record 页返回 /record/:table/:id', () => {
            expect(navTo('record', 'Item', '1001')).toBe('/record/Item/1001');
        });

        it('edit=true 且 page=record 时加 /edit 前缀', () => {
            expect(navTo('record', 'Item', '1001', true)).toBe('/edit/record/Item/1001');
        });

        it('edit=true 但 page 非 record 不加 /edit 前缀（仅 record 支持编辑）', () => {
            expect(navTo('recordRef', 'Item', '1001', true)).toBe('/recordRef/Item/1001');
            expect(navTo('table', 'Item', '', true)).toBe('/table/Item/');
            expect(navTo('tableRef', 'Item', '', true)).toBe('/tableRef/Item/');
            expect(navTo('recordUnref', 'Item', '1001', true)).toBe('/recordUnref/Item/1001');
        });

        it('edit=false 时不加 /edit 前缀', () => {
            expect(navTo('record', 'Item', '1001', false)).toBe('/record/Item/1001');
        });

        it('写入 localStorage 的 curPage/curTableId/curId', () => {
            navTo('record', 'Item', '1001');
            expect(localStorage.getItem('curPage')).toBe('record');
            expect(localStorage.getItem('curTableId')).toBe('Item');
            expect(localStorage.getItem('curId')).toBe('1001');
        });

        it('addHistory 默认 true：追加到 store.history', () => {
            navTo('record', 'Item', '1001');
            const cur = getMyStore().history.cur();
            expect(cur?.table).toBe('Item');
            expect(cur?.id).toBe('1001');
        });

        it('addHistory=false：不追加到 store.history', () => {
            navTo('record', 'Item', '1001', false, false);
            expect(getMyStore().history.cur()).toBeUndefined();
        });

        it('重复调同 table/id：history addItem 视为无操作（同一实例）', () => {
            navTo('record', 'Item', '1001');
            const h1 = getMyStore().history;
            navTo('record', 'Item', '1001');
            expect(getMyStore().history).toBe(h1);
        });

        it('id 含 / 时透传到 URL（不做 encoding，与 useLocationData 的 slice(idx).join("/") 互逆）', () => {
            expect(navTo('record', 'Item', 'with/slash')).toBe('/record/Item/with/slash');
            expect(navTo('recordUnref', 'Item', 'with/slash')).toBe('/recordUnref/Item/with/slash');
        });
    });

    describe('getLastNavToInLocalStore', () => {
        it('从 localStorage 三 key 重建路由', () => {
            localStorage.setItem('curPage', 'record');
            localStorage.setItem('curTableId', 'Item');
            localStorage.setItem('curId', '1001');
            expect(getLastNavToInLocalStore()).toBe('/record/Item/1001');
        });

        it('isEditMode=true 时 record 带 /edit 前缀', () => {
            localStorage.setItem('curPage', 'record');
            localStorage.setItem('curTableId', 'Item');
            localStorage.setItem('curId', '1001');
            localStorage.setItem('isEditMode', 'true');
            expect(getLastNavToInLocalStore()).toBe('/edit/record/Item/1001');
        });

        it('isEditMode=false 或缺失时不加 /edit', () => {
            localStorage.setItem('curPage', 'record');
            localStorage.setItem('curTableId', 'Item');
            localStorage.setItem('curId', '1001');
            // 缺失 isEditMode → getPrefBool 默认 false
            expect(getLastNavToInLocalStore()).toBe('/record/Item/1001');

            localStorage.setItem('isEditMode', 'false');
            expect(getLastNavToInLocalStore()).toBe('/record/Item/1001');
        });

        it('table 页不加 /edit（即使 isEditMode=true）', () => {
            localStorage.setItem('curPage', 'table');
            localStorage.setItem('curTableId', 'Item');
            localStorage.setItem('curId', '');
            localStorage.setItem('isEditMode', 'true');
            expect(getLastNavToInLocalStore()).toBe('/table/Item/');
        });

        it('recordRef/recordUnref 页也能重建', () => {
            localStorage.setItem('curPage', 'recordRef');
            localStorage.setItem('curTableId', 'Item');
            localStorage.setItem('curId', '1001');
            expect(getLastNavToInLocalStore()).toBe('/recordRef/Item/1001');

            localStorage.setItem('curPage', 'recordUnref');
            expect(getLastNavToInLocalStore()).toBe('/recordUnref/Item/1001');
        });

        it('curPage 非法时 getPrefEnumStr 返回 undefined → 回退 record', () => {
            localStorage.setItem('curPage', 'invalidPage');
            localStorage.setItem('curTableId', 'Item');
            localStorage.setItem('curId', '1001');
            expect(getLastNavToInLocalStore()).toBe('/record/Item/1001');
        });

        it('curPage 缺失时同样回退 record', () => {
            localStorage.setItem('curTableId', 'Item');
            localStorage.setItem('curId', '1001');
            expect(getLastNavToInLocalStore()).toBe('/record/Item/1001');
        });

        it('tableId/curId 缺失时 getPrefStr 回退空串', () => {
            expect(getLastNavToInLocalStore()).toBe('/record//');
        });

        it('id 含 / 时透传（与 navTo 一致）', () => {
            localStorage.setItem('curPage', 'record');
            localStorage.setItem('curTableId', 'Item');
            localStorage.setItem('curId', 'with/slash');
            expect(getLastNavToInLocalStore()).toBe('/record/Item/with/slash');
        });
    });
});

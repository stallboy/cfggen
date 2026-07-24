import {describe, expect, it} from 'vitest';
import {getPrefKeySet, getPrefSelfKeySet} from './store';

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

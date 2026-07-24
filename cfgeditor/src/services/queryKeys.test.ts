import {afterEach, describe, expect, it, vi} from 'vitest';
import {queryClient} from './queryClient';
import {
    invalidateLayoutCache,
    queryKeys,
    refetchResInfoCache,
    removeEditLayoutCache,
    setNotesCache
} from './queryKeys';

// 行为测试：seed 缓存 → 调动词 → 断言缓存状态。测的是「前缀真的命中该命中的、不误伤邻居」，
// 而非抄写实现的字面量（spy 断言调用参数无法验证前缀是否真能命中缓存项）。
describe('缓存写动词（行为契约）', () => {
    afterEach(() => {
        queryClient.clear();   // 单例跨用例清理
        vi.restoreAllMocks();
    });

    it('removeEditLayoutCache：编辑态分桶被删，浏览态分桶与其他域不受影响', () => {
        queryClient.setQueryData(['layout', '/p', 'e', 'K'], 'edit-layout');
        queryClient.setQueryData(['layout', '/p', 'K'], 'view-layout');
        queryClient.setQueryData(['record', 't', '1'], 'rec');

        removeEditLayoutCache('/p');

        expect(queryClient.getQueryData(['layout', '/p', 'e', 'K'])).toBeUndefined(); // 编辑态已删
        expect(queryClient.getQueryData(['layout', '/p', 'K'])).toBe('view-layout');  // 浏览态保留
        expect(queryClient.getQueryData(['record', 't', '1'])).toBe('rec');           // 其他域不误伤
    });

    it('invalidateLayoutCache：两种分桶都标 stale（数据保留），其他 pathname 不受影响', () => {
        queryClient.setQueryData(['layout', '/p', 'e', 'K'], 1);
        queryClient.setQueryData(['layout', '/p', 'K'], 2);
        queryClient.setQueryData(['layout', '/other', 'K'], 3);

        invalidateLayoutCache('/p');

        expect(queryClient.getQueryState(['layout', '/p', 'e', 'K'])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(['layout', '/p', 'K'])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(['layout', '/other', 'K'])?.isInvalidated).toBe(false);
    });

    it('setNotesCache：notes key 下读出 notesToMap 变换结果', () => {
        setNotesCache({notes: [{key: 'k1', note: 'n1'}]});

        expect(queryClient.getQueryData(queryKeys.notes())).toEqual(new Map([['k1', 'n1']]));
    });

    it('removeEditLayoutCache 前缀 = factory 编辑态 key 前缀（跨 artifact 一致性）', () => {
        // factory 的 'e' 段位置漂移时此断言先红（行为测试 seed 的是手写字面量，兜不住 factory 侧改动）
        const editKey = queryKeys.layout('/p', {}, {}, true);
        expect(editKey.slice(0, 3)).toEqual(['layout', '/p', 'e']);
    });

    it('refetchResInfoCache：invalidate resInfo 且 refetchType=all（强制重扫，不等 active）', () => {
        // 保留 spy：钉住 refetchType:'all' 这个易被「顺手简化」掉的配置细节，行为化反而绕
        const spy = vi.spyOn(queryClient, 'invalidateQueries');

        refetchResInfoCache();

        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith({queryKey: queryKeys.resInfo(), refetchType: 'all'});
    });
});

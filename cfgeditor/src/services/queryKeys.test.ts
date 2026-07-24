import {afterEach, describe, expect, it, vi} from 'vitest';
import {queryClient} from './queryClient';
import {
    invalidateLayoutCache,
    queryKeys,
    refetchResInfoCache,
    removeEditLayoutCache,
    setNotesCache
} from './queryKeys';
import {notesToMap} from '@/api/noteModel';

describe('缓存写动词（收口于 queryKeys/queryClient 单例，调用方不碰 client）', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('removeEditLayoutCache：removeQueries 前缀 [layout, pathname, e]，且是 factory 编辑态 key 的前缀', () => {
        const spy = vi.spyOn(queryClient, 'removeQueries');

        removeEditLayoutCache('/record/Item/1001');

        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith({queryKey: ['layout', '/record/Item/1001', 'e']});
        // 一致性：前缀必须命中 factory 构造的编辑态 key（'e' 段位置漂移时此断言先红）
        const editKey = queryKeys.layout('/record/Item/1001', {}, {}, true);
        expect(editKey.slice(0, 3)).toEqual(['layout', '/record/Item/1001', 'e']);
    });

    it('invalidateLayoutCache：invalidateQueries 前缀 [layout, pathname]（不带 e，两种分桶一并命中）', () => {
        const spy = vi.spyOn(queryClient, 'invalidateQueries');

        invalidateLayoutCache('/record/Item/1001');

        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith({queryKey: ['layout', '/record/Item/1001']});
    });

    it('refetchResInfoCache：invalidate resInfo 且 refetchType=all（强制重扫，不等 active）', () => {
        const spy = vi.spyOn(queryClient, 'invalidateQueries');

        refetchResInfoCache();

        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith({queryKey: queryKeys.resInfo(), refetchType: 'all'});
    });

    it('setNotesCache：setQueryData 写 notes key，内容经 notesToMap 变换', () => {
        const spy = vi.spyOn(queryClient, 'setQueryData');
        const notes = {notes: [{key: 'k1', note: 'n1'}]};

        setNotesCache(notes);

        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith(queryKeys.notes(), notesToMap(notes));
    });
});

import {QueryClient} from '@tanstack/react-query';

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 1000 * 30,
        },
    },
});

export function invalidateAllQueries() {
    // queryKey: [] 匹配所有查询并标记 stale；去掉 refetchType:'all' 改用默认 'active'，
    // 只立即重请求当前挂载的查询，未挂载的查询在下次 mount 时按 stale 自然刷新（正确性不变）。
    queryClient.invalidateQueries({queryKey: []}).catch((reason: unknown) => {
        console.log(reason);
    });
}

/** 写操作（record save / addJson）后失效所有数据查询，但**排除 layout**。
 *  layout 不在此 invalidate：invalidate 会立即用**重渲前的旧 queryFn 闭包** refetch（保存发起时 list 仍收起 →
 *  算出收起态布局），这份陈旧布局随后被服到 record 回来展开后的新节点集 → applyRectToNodes not found +
 *  新节点跳到默认位 (100,100) 全重叠（"删 list 项→embed→更新→子项重叠"复现）。layout 改由
 *  EditingSession.onStructureChange→removeEditLayoutCache 在结构变更时同步清、重渲后用新闭包自然重取
 *  （同 removeLayoutCache/removeEditLayoutCache 的 remove-不-invalidate 契约）。
 *  ToolsSetting 改 schema 仍走 invalidateAllQueries（schema 变需刷各视图 layout，性质不同）。 */
export function invalidateAllExceptLayout() {
    queryClient.invalidateQueries({
        queryKey: [],
        predicate: (query) => query.queryKey[0] !== 'layout',
    }).catch((reason: unknown) => {
        console.log(reason);
    });
}

/** 换库 / 重连时清空全部缓存（setServer 用）。与 invalidateAllQueries 不同：remove 直接删除
 *  不主动 fetch，等下次 mount 自然重取——旧库数据即刻不再可读，不留 stale 窗口。 */
export function removeAllQueryCache() {
    queryClient.removeQueries({queryKey: []});
}

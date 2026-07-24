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

/** 换库 / 重连时清空全部缓存（setServer 用）。与 invalidateAllQueries 不同：remove 直接删除
 *  不主动 fetch，等下次 mount 自然重取——旧库数据即刻不再可读，不留 stale 窗口。 */
export function removeAllQueryCache() {
    queryClient.removeQueries({queryKey: []});
}

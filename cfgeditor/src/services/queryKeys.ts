// Query Key Factory：集中构造所有 queryKey，避免字面量散落各处导致 invalidate 时拼错。
//
// 约定：每条 key 第一段是「资源域」字符串（schema / record / recordRef / layout ...），
// 其余段是该查询的全部入参——漏任一入参 = 不同参数共享同一缓存 = 脏数据。
//
// 用法：
//   useQuery({ queryKey: queryKeys.record(curTableId, curId), ... })
//   queryClient.invalidateQueries({ queryKey: queryKeys.notes() })
//
// layout 的「按前缀批量失效」走下方 removeEditLayoutCache / invalidateLayoutCache 两个 helper——
// 前缀（含 'e' 分桶段的位置）必须与 queryKeys.layout 的构造保持一致，集中在本文件即保证同步改。
//
// 本文件同时收口所有「key 相关」的缓存写动词（remove/invalidate/setQueryData），调用方不碰
// queryClient 实例；key 无关的全量操作（invalidateAllQueries / removeAllQueryCache）在 queryClient.ts。
import {queryClient} from "./queryClient.ts";
import {Notes, notesToMap} from "@/api/noteModel.ts";

export const queryKeys = {
    // 启动期一次性（AppLoader）
    setting: () => ['setting'],
    resInfo: () => ['setting', 'resInfo'],   // 挂在 setting 域下，enabled 依赖 setting 完成

    // 全局低频
    schema: () => ['schema'],
    notes: () => ['notes'],

    // 单条记录
    record: (tableId: string, id: string) => ['record', tableId, id],

    // 引用图
    recordRef: (tableId: string, id: string, refOutDepth: number, maxNode: number, refIn: boolean) =>
        ['recordRef', tableId, id, refOutDepth, maxNode, refIn],
    unreferenced: (tableId: string, maxNode: number) => ['unreferenced', tableId, maxNode],

    // 布局（ELK 结果缓存）；编辑路由态插入 'e' 段隔离——编辑/浏览 entityMap 构建方式不同（节点集合不同），
    // 必须分桶，否则 nodes 与 rectMap 错配（applyRectToNodes not found）。结构变更时
    // removeQueries(['layout', pathname, 'e']) 只清编辑态。分桶标志是「路由态」(type==='edit') 而非脏标记：
    // 提交后 isEdited 翻 false 但 entityMap 仍是编辑态构建；脏标记只驱动 staleTime（见 useEntityToGraph）。
    layout: (pathname: string, layoutKeys: object, topologyKeys: object, isEditRoute: boolean) =>
        isEditRoute
            ? ['layout', pathname, 'e', layoutKeys, topologyKeys]
            : ['layout', pathname, layoutKeys, topologyKeys],

    // AI
    prompt: (tableId: string) => ['prompt', tableId],
};

/** 结构编辑后清该 pathname 的编辑态 layout 缓存（前缀失效：['layout', pathname, 'e']）。
 *  前缀必须与 queryKeys.layout 的编辑态分桶（'e' 段位置）一致——集中在本文件，改分桶约定时同步改。
 *  契约：由 EditingSession.onStructureChange 在事件期同步调用，不能挪 effect——否则重渲那一帧
 *  useQuery 读到未删的旧缓存，旧布局多渲一帧。用 remove 不用 invalidate：invalidate 会立即用
 *  重渲前的旧 queryFn 闭包 refetch → 旧布局；remove 只删缓存，等重渲后用新闭包自然重取。 */
export function removeEditLayoutCache(pathname: string): void {
    queryClient.removeQueries({queryKey: ['layout', pathname, 'e']});
}

/** 该 pathname 的全部 layout 缓存标记失效（前缀失效：['layout', pathname]），布局失败 retry 用。
 *  不带 'e' 段，编辑态/浏览态分桶一并命中。 */
export function invalidateLayoutCache(pathname: string): void {
    queryClient.invalidateQueries({queryKey: ['layout', pathname]}).catch((reason: unknown) => {
        console.log(reason);
    })
}

/** 资源信息（resInfo）强制重扫：refetchType:'all' 连未挂载的查询也立即重取（资源目录变了要立刻反映，
 *  不能用默认 'active' 等自然刷新）。调用方：res/readResInfosAsync 的 invalidateResInfos
 *  （它还重置自己的 alreadyRead 守卫——那是 res 层职责，不在本动词内）。 */
export function refetchResInfoCache(): void {
    queryClient.invalidateQueries({queryKey: queryKeys.resInfo(), refetchType: 'all'}).catch((reason: unknown) => {
        console.log(reason);
    });
}

/** updateNote 成功后精确写 notes 缓存：后端返回全量 notes，直接替它回答（setQueryData），
 *  省一次 GET /notes refetch。 */
export function setNotesCache(notes: Notes): void {
    queryClient.setQueryData(queryKeys.notes(), notesToMap(notes));
}

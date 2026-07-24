// Query Key Factory：集中构造所有 queryKey，避免字面量散落各处导致 invalidate 时拼错。
//
// 约定：每条 key 第一段是「资源域」字符串（schema / record / recordRef / layout ...），
// 其余段是该查询的全部入参——漏任一入参 = 不同参数共享同一缓存 = 脏数据。
//
// 用法：
//   useQuery({ queryKey: queryKeys.record(curTableId, curId), ... })
//   queryClient.invalidateQueries({ queryKey: queryKeys.notes() })
//
// 例外：layout 的「按前缀批量失效」仍手写——
//   - removeQueries({queryKey: ['layout', pathname, 'e']})   // 清该 pathname 编辑态
//   - invalidateQueries({queryKey: ['layout', pathname]})    // 该 pathname 全部 layout
// 那是批量语义，与这里「构造单条完整 key」不同；第一段 'layout' 与本 factory 保持一致即可。
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

import {useContext, useEffect, useMemo, useRef} from "react";
import {useMyStore} from "@/store/store";
import {Rect, useReactFlow, useStore} from "@xyflow/react";
import {convertNodeAndEdges} from "./layout/entityToNodeAndEdge.ts";
import {useQuery} from "@tanstack/react-query";
import {queryClient} from "@/services/queryClient.ts";
import {queryKeys} from "@/services/queryKeys.ts";
import {layoutAsync} from "./layout/layoutAsync.ts";
import {EntityNode, NodeDoubleClickFunc, NodeMenuFunc} from "./FlowGraph.tsx";
import {Entity, EditingObjectRes} from "@/domain/entityModel";
import {MenuItem} from "./FlowContextMenu.tsx";
import {NodePlacementStrategyType, NodeShowType} from "@/domain/storageJson";
import {FlowGraphContext} from "./FlowGraphContext.ts";
import {pickViewportAction} from "./layout/viewportMath.ts";
import {devLog} from "./devLog.ts";
import {pickLayoutKeys} from "@/domain/nodeShowLayoutKeys";


type FlowGraphType = 'record' | 'edit' | 'ref' | 'table' | 'tableRef';

interface FlowGraphInput {
    type: FlowGraphType;
    pathname: string;
    entityMap: Map<string, Entity>;
    notes?: Map<string, string>;
    nodeMenuFunc: NodeMenuFunc;
    paneMenu: MenuItem[];
    nodeDoubleClickFunc?: NodeDoubleClickFunc;
    editingObjectRes?: EditingObjectRes;

    setFitViewForPathname?: (pathname: string) => void;
    nodeShow?: NodeShowType;
}

function getLayoutStrategy(nodeShow: NodeShowType, type: FlowGraphType): NodePlacementStrategyType {
    switch (type) {
        case 'record':
            return nodeShow.recordLayout;
        case 'edit':
            return nodeShow.editLayout;
        case 'ref':
            return nodeShow.refLayout;
        case 'table':
            return nodeShow.tableLayout;
        case 'tableRef':
            return nodeShow.tableRefLayout;
    }
}

// 一次遍历同时写 position 与 width/height（原 applyPositionToNodes + applyWidthHeightToNodes 各遍历一次同一份 rectMap，合并省一趟 map）。
function applyRectToNodes(nodes: EntityNode[], rectMap?: Map<string, Rect>) {
    if (!rectMap) {
        return nodes;
    }
    return nodes.map(n => {
        const r = rectMap.get(n.id);
        if (r) {
            return {...n, position: {x: r.x, y: r.y}, width: r.width, height: r.height};
        }
        // 节点无对应布局 rect：数据异常（节点未进 ELK 结果），诊断仅 dev 打印。
        devLog('not found', n, rectMap);
        return n;
    });
}

// 布局失败兜底：ELK 失败时把节点按网格铺开，避免全部塌叠在默认 (100,100) 成一摞不可读。
// 非精确布局——只求"看得见、不重叠成一摞"，retry 成功后由正常 applyRectToNodes 路径校正。
// 网格单元略大于最大节点（edit 280 宽 + padding），保证横向不贴边；纵向高度按典型行数留余量。
const FALLBACK_CELL_W = 320;
const FALLBACK_CELL_H = 260;
const FALLBACK_COLS = 5;
const FALLBACK_ORIGIN = 80;
function spreadFallbackNodes(nodes: EntityNode[]): EntityNode[] {
    return nodes.map((n, i) => ({
        ...n,
        position: {
            x: FALLBACK_ORIGIN + (i % FALLBACK_COLS) * FALLBACK_CELL_W,
            y: FALLBACK_ORIGIN + Math.floor(i / FALLBACK_COLS) * FALLBACK_CELL_H,
        },
    }));
}


export function useEntityToGraph({
                                     type, pathname, entityMap, notes,
                                     nodeMenuFunc, paneMenu, nodeDoubleClickFunc,
                                     editingObjectRes,
                                     setFitViewForPathname, nodeShow,
                                 }: FlowGraphInput) {
    const flowGraph = useContext(FlowGraphContext);
    if (!flowGraph) {
        // FlowGraphContext 默认 undefined（见 FlowGraphContext.ts）：FlowGraph 外误用即显式报错，避免旧 dummy noop 的静默失败。
        throw new Error('FlowGraphContext missing: useEntityToGraph must run inside <FlowGraph>');
    }
    const {
        nodeShow: currentNodeShow,
        // 拓扑 setting（影响 entityMap 节点集合）纳入 layout queryKey → 改这些缓存自然失效，
        // 替代旧 store setter 的 clearLayoutCache 命令式清缓存。新增拓扑 setting 在此登记即可，无需 clearLayoutCache。
        maxImpl, refIn, refOutDepth, maxNode,
        recordRefIn, recordRefInShowLinkMaxNode, recordRefOutDepth, recordMaxNode, tauriConf,
    } = useMyStore();
    const nodeShowSetting = nodeShow ?? currentNodeShow;
    // 命令面收口到公开稳定的 useReactFlow（setNodes/setEdges/setViewport/getViewport/fitView），
    // 不再调用内部 panZoom 的方法（非公开契约，升级易变）。详见 doc §5。
    // 唯一保留的 useStore 只读切片：viewport 是否就绪——本版本无 viewportInitialized 字段，
    // 就绪信号即 panZoom 非空；只做 null 判断不调用其方法。
    const viewportReady = useStore((state) => state.panZoom !== null);
    const {setNodes, setEdges, setViewport, getViewport, fitView} = useReactFlow();

    // 上一帧布局结果：供 KeepStable（undo/redo）算 anchorOld（undo 发起时锚点坐标）。
    // 只在 Effect 2 主体内更新；loading 期（id2RectMap=undefined）guard 不满足、不更新 → undo 前布局留存到补偿那帧。
    const prevRectMapRef = useRef<Map<string, Rect> | undefined>(undefined);

    // nodeShow/notes 下发到 node.data；query 不在此下发——它无 per-graph override，
    // 渲染组件（FlowNode/EntityProperties/EntityCard）各自 useMyStore() 订阅（resso per-key），
    // 故 query 不进 nodes 重建、不进 layout，搜索时不重跑 ELK（与 store.ts setQuery 注释一致）。
    const {nodes, edges} = useMemo(() => convertNodeAndEdges({
        entityMap,
        nodeShow: nodeShowSetting,
        notes,
    }), [entityMap, notes, nodeShowSetting]);

    // layout 缓存：queryKey 与 staleTime 是两个独立维度。
    //   - queryKey 分桶（isEditRoute）：按 entityMap 构建方式分。编辑路由（RecordEditEntityCreator）与浏览
    //     路由（RecordEntityCreator）产出不同节点集合（编辑态按 $fold/$embed 收起子结构，浏览态全展开 + 外部
    //     ref），混用同一 key 会让 nodes 与 rectMap 错配（applyRectToNodes not found + 节点跳位）。用
    //     type==='edit' 分桶而非 isEdited——提交后 isEdited 翻 false 但 entityMap 仍是编辑态构建，若按 isEdited
    //     切到浏览态 key 会命中陈旧浏览态缓存（节点集合错配）。
    //   - staleTime（isEdited）：跟脏标记走。干净（进入编辑未改 / 刚提交）→ 5min 复用；脏（有未提交修改）→ 0，
    //     trigger 时重取。值类编辑不 bump、不刷新 isEdited（quirk）→ 继续走干净态缓存正确。勿当 bug 修。
    // queryKey 还含布局相关字段（pickLayoutKeys）+ 拓扑 setting（topologyKeys）：
    //   - 改纯颜色字段 → queryKey 不变 → 命中缓存不重跑 ELK；
    //   - 改拓扑 setting（maxImpl/refOutDepth/recordRef*/tauriConf…）→ topologyKeys 变 → 缓存自然失效重布局。
    // 'e' 段保持在 pathname 之后同一层级，保 Record.tsx 的 ['layout', pathname, 'e'] prefix 失效契约。
    const layoutKeys = pickLayoutKeys(nodeShowSetting);
    const topologyKeys = {
        maxImpl, refIn, refOutDepth, maxNode,
        recordRefIn, recordRefInShowLinkMaxNode, recordRefOutDepth, recordMaxNode, tauriConf,
    };
    const isEditRoute = type === 'edit';
    const isEdited = !!editingObjectRes?.isEdited;
    const queryKey = queryKeys.layout(pathname, layoutKeys, topologyKeys, isEditRoute)
    const staleTime = isEdited ? 0 : 1000 * 60 * 5;
    const {data: id2RectMap, error: layoutError} = useQuery({
        queryKey: queryKey,
        // 透传 react-query 的 AbortSignal：query 变 stale/inactive 时 react-query abort，
        // layoutAsync 据此放弃过期的 ELK 结果。失败一律 throw（绝不 resolve undefined）。
        queryFn: async (ctx) => await layoutAsync(nodes, edges, getLayoutStrategy(nodeShowSetting, type), nodeShowSetting, ctx.signal),
        staleTime: staleTime,
    })

    const newNodes: EntityNode[] | undefined = useMemo(() => {
        if (id2RectMap) {
            return applyRectToNodes(nodes, id2RectMap);
        }
        return undefined;
    }, [nodes, id2RectMap]);

    // Effect 1：节点/边/菜单回调下发。随 nodes/edges/菜单回调变化重跑。
    useEffect(() => {
        if (newNodes) {
            flowGraph.setNodeMenuFunc(nodeMenuFunc);
            flowGraph.setPaneMenu(paneMenu);
            if (nodeDoubleClickFunc) {
                flowGraph.setNodeDoubleClickFunc(nodeDoubleClickFunc)
            }
            setNodes(newNodes);
            setEdges(edges);
        } else if (layoutError) {
            // 布局失败兜底（layoutAsync throw → react-query retry 耗尽后 error 态）：
            // 节点按网格铺开（spreadFallbackNodes）保证画布非空且可读 + console.error 反馈；
            // 视觉反馈（Result 覆盖层 + retry）由 Effect 3 经 FlowGraphContext 透传到 FlowGraph。
            // retry 成功后由 newNodes 分支校正。仅 error 态触发，不污染正常路径（无闪烁）。
            console.error('[layout] failed, falling back to unpositioned nodes', layoutError);
            setNodes(spreadFallbackNodes(nodes));
            setEdges(edges);
        }
    }, [newNodes, edges, nodeMenuFunc, paneMenu, nodeDoubleClickFunc, flowGraph,
        setNodes, setEdges, layoutError, nodes]);

    // Effect 3：布局失败反馈。把 error 透传给 FlowGraph（渲染 Result 覆盖层 + retry 按钮），
    // retry = invalidate 该 pathname 的 layout 缓存 → react-query 重新跑 ELK。
    // 拆独立 effect：只随 layoutError/pathname 变化，不沾 Effect 1 的菜单依赖；flowGraph 引用稳定（ctx memoized）。
    useEffect(() => {
        flowGraph.setLayoutError(layoutError ?? undefined);
        flowGraph.setRetryLayout(() => queryClient.invalidateQueries({queryKey: ['layout', pathname]}));
    }, [layoutError, flowGraph, pathname]);

    // Effect 2：视口动作。刻意与 Effect 1 拆开——视口只随「视口指令(editingObjectRes) / layout 结果
    // (id2RectMap) / 就绪信号(viewportReady)」变化，不含 paneMenu/nodeMenuFunc 等菜单回调。
    // 历史背景：值类编辑 coalescing flush 曾让 Record 订阅的 canUndo 翻转 → paneMenu 新引用 → 视口被
    // 连带重置（EntityForm 输入 primitive 后过一会偶发 fitFull）。现 Record 不再订阅 canUndo（hotkey 回调
    // 实时判）+ paneMenu disabled 惰性化，paneMenu 引用稳定，此诱因消除；拆分仍作视口语义独立边界保留。
    useEffect(() => {
        if (viewportReady && id2RectMap && newNodes) {
            const action = pickViewportAction(editingObjectRes, id2RectMap, getViewport(), {
                prevId2RectMap: prevRectMapRef.current,
            });
            if (action.kind === 'fitFull') {
                // FitFull：原 getNodesBounds+getViewportForBounds+setViewport 三步等价于一次公开 fitView。
                // padding/minZoom/maxZoom 与原 getViewportForBounds(bounds,w,h,0.3,1,0.2) 对齐。
                void fitView({padding: 0.2, minZoom: 0.3, maxZoom: 1});
                if (setFitViewForPathname) {
                    setFitViewForPathname(pathname);
                }
            } else if (action.kind === 'fitId') {
                // FitId / KeepStable：relayout 后让锚点节点屏幕坐标不变。newVp 已由 pickViewportAction
                // 经 computeStableViewport 算好（FitId 用 position.x/y，KeepStable 用 prevRectMap 坐标）。
                void setViewport(action.viewport);
            }
            // noop（NoChange / FitId 但 id 不存在 / KeepStable 锚点缺失）：不调 fitView/setViewport，保持当前视口。
            prevRectMapRef.current = id2RectMap;   // 记录本帧布局，供下次 KeepStable 作 anchorOld
        }
    }, [editingObjectRes, id2RectMap, viewportReady, newNodes, fitView, setViewport, getViewport,
        setFitViewForPathname, pathname]);

}

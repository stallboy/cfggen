import {useContext, useEffect, useMemo} from "react";
import {useMyStore} from "@/store/store";
import {Rect, useReactFlow, useStore} from "@xyflow/react";
import {convertNodeAndEdges} from "./entityToNodeAndEdge.ts";
import {useQuery} from "@tanstack/react-query";
import {layoutAsync} from "./layoutAsync.ts";
import {EntityNode, NodeDoubleClickFunc, NodeMenuFunc} from "./FlowGraph.tsx";
import {Entity, EditingObjectRes, EFitView} from "@/domain/entityModel";
import {MenuItem} from "./FlowContextMenu.tsx";
import {NodePlacementStrategyType, NodeShowType} from "@/domain/storageJson";
import {FlowGraphContext} from "./FlowGraphContext.ts";
import {computeStableViewport} from "./viewportMath.ts";
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

function applyPositionToNodes(nodes: EntityNode[], id2RectMap: Map<string, Rect>) {
    return nodes.map(n => {
        const newPos = id2RectMap.get(n.id);
        if (newPos) {
            const {x, y} = newPos;
            return {
                ...n,
                position: {x, y},
            }
        } else {
            console.log('not found', n, id2RectMap)
            return n;
        }
    })
}


function applyWidthHeightToNodes(nodes: EntityNode[], id2RectMap?: Map<string, Rect>) {
    if (!id2RectMap) {
        return nodes;
    }
    return nodes.map(n => {
        const newPos = id2RectMap.get(n.id);
        if (newPos) {
            const {width, height} = newPos;
            return {...n, width, height};
        } else {
            return n;
        }
    })
}


export function useEntityToGraph({
                                     type, pathname, entityMap, notes,
                                     nodeMenuFunc, paneMenu, nodeDoubleClickFunc,
                                     editingObjectRes,
                                     setFitViewForPathname, nodeShow,
                                 }: FlowGraphInput) {
    const flowGraph = useContext(FlowGraphContext);
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

    // nodeShow/notes 下发到 node.data；query 不在此下发——它无 per-graph override，
    // 渲染组件（FlowNode/EntityProperties/EntityCard）各自 useMyStore() 订阅（resso per-key），
    // 故 query 不进 nodes 重建、不进 layout，搜索时不重跑 ELK（与 store.ts setQuery 注释一致）。
    const {nodes, edges} = useMemo(() => convertNodeAndEdges({
        entityMap,
        nodeShow: nodeShowSetting,
        notes,
    }), [entityMap, notes, nodeShowSetting]);

    // layout 缓存策略：'e' 是编辑态（isEdited）layout 缓存的隔离标记——结构变更时 onStructureChange 调
    // removeQueries(['layout', pathname, 'e']) 只清编辑态缓存，浏览态（无 'e'）的 5min 缓存不受影响。
    // staleTime 脏=0（每次重取，编辑可能改了拓扑）/ 干净=5min（拓扑稳定复用缓存）。
    // quirk：纯值类编辑（键入 primitive）期间 isEdited 不刷新——entityMap 不重算、editingObjectRes 不
    // 重建（性能契约1）。安全：值类不改拓扑、布局不变，继续走干净态 5min 缓存正确。勿当 bug 修。
    // queryKey 含布局相关字段（pickLayoutKeys）+ 拓扑 setting（topologyKeys）：
    //   - 改纯颜色字段 → queryKey 不变 → 命中缓存不重跑 ELK；
    //   - 改拓扑 setting（maxImpl/refOutDepth/recordRef*/tauriConf…）→ topologyKeys 变 → 缓存自然失效重布局。
    // 'e' 标记保持在 pathname 之后同一层级，保 Record.tsx 的 ['layout', pathname, 'e'] prefix 失效契约。
    const layoutKeys = pickLayoutKeys(nodeShowSetting);
    const topologyKeys = {
        maxImpl, refIn, refOutDepth, maxNode,
        recordRefIn, recordRefInShowLinkMaxNode, recordRefOutDepth, recordMaxNode, tauriConf,
    };
    const queryKey = editingObjectRes?.isEdited ?
        ['layout', pathname, 'e', layoutKeys, topologyKeys] : ['layout', pathname, layoutKeys, topologyKeys]
    const staleTime = editingObjectRes?.isEdited ? 0 : 1000 * 60 * 5;
    const {data: id2RectMap, error: layoutError} = useQuery({
        queryKey: queryKey,
        // 透传 react-query 的 AbortSignal：query 变 stale/inactive 时 react-query abort，
        // layoutAsync 据此放弃过期的 ELK 结果。失败一律 throw（绝不 resolve undefined）。
        queryFn: async (ctx) => await layoutAsync(nodes, edges, getLayoutStrategy(nodeShowSetting, type), nodeShowSetting, ctx.signal),
        staleTime: staleTime,
    })

    const newNodes: EntityNode[] | undefined = useMemo(() => {
        if (id2RectMap) {
            const positionedNodes = applyPositionToNodes(nodes, id2RectMap);
            return applyWidthHeightToNodes(positionedNodes, id2RectMap);
        }
        return undefined;
    }, [nodes, id2RectMap]);

    useEffect(() => {
        if (newNodes) {
            flowGraph.setNodeMenuFunc(nodeMenuFunc);
            flowGraph.setPaneMenu(paneMenu);
            if (nodeDoubleClickFunc) {
                flowGraph.setNodeDoubleClickFunc(nodeDoubleClickFunc)
            }
            setNodes(newNodes);
            setEdges(edges);
            if (viewportReady && id2RectMap) {
                if (editingObjectRes === undefined || editingObjectRes.fitView === EFitView.FitFull) {
                    // FitFull：原 getNodesBounds+getViewportForBounds+setViewport 三步等价于一次公开 fitView。
                    // padding/minZoom/maxZoom 与原 getViewportForBounds(bounds,w,h,0.3,1,0.2) 对齐。
                    void fitView({padding: 0.2, minZoom: 0.3, maxZoom: 1});
                    if (setFitViewForPathname) {
                        setFitViewForPathname(pathname);
                    }
                } else if (editingObjectRes?.fitView === EFitView.FitId
                    && editingObjectRes.fitViewToIdPosition) {
                    const {id, x, y} = editingObjectRes.fitViewToIdPosition;
                    const nowXy = id2RectMap.get(id);
                    if (nowXy !== undefined) { // onDeleteItemFromArray时，是会遇到undefined情况的
                        // FitId：relayout 后让该节点屏幕坐标不变。fitView 做不到，必须自算（见 viewportMath）。
                        // anchorOld=旧世界坐标(x,y) anchorNew=新世界坐标(nowXy)；求保持锚点屏幕不动的 newVp。
                        const newVp = computeStableViewport(
                            {x, y},
                            {x: nowXy.x, y: nowXy.y},
                            getViewport(),
                        );
                        void setViewport(newVp);
                    }
                }
            }
        } else if (layoutError) {
            // 布局失败兜底（layoutAsync throw → react-query retry 耗尽后 error 态）：
            // 把未布局节点（默认位置 100,100）推入保证画布非空 + console.error 反馈，
            // 等 retry 成功后由 newNodes 分支校正。仅 error 态触发，不污染正常路径（无闪烁）。
            console.error('[layout] failed, falling back to unpositioned nodes', layoutError);
            setNodes(nodes);
            setEdges(edges);
        }
    }, [newNodes, edges, nodeMenuFunc, paneMenu, editingObjectRes, flowGraph, nodeDoubleClickFunc,
        setNodes, setEdges, id2RectMap, viewportReady, fitView, setViewport, getViewport,
        setFitViewForPathname, pathname, layoutError, nodes]);

}

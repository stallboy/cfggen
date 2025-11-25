import {useContext, useEffect, useMemo} from "react";
import {useMyStore} from "../store/store.ts";
import {getViewportForBounds, Rect, useReactFlow, useStore} from "@xyflow/react";
import {convertNodeAndEdges} from "./entityToNodeAndEdge.ts";
import {useQuery} from "@tanstack/react-query";
import {layoutAsync} from "./layoutAsync.ts";
import {EntityNode, NodeDoubleClickFunc, NodeMenuFunc} from "./FlowGraph.tsx";
import {Entity} from "./entityModel.ts";
import {MenuItem} from "./FlowContextMenu.tsx";
import {NodePlacementStrategyType, NodeShowType} from "../store/storageJson.ts";
import {EditingObjectRes, EFitView} from "../routes/record/editingObject.ts";
import {FlowGraphContext} from "./FlowGraphContext.ts";


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
    const {query, nodeShow: currentNodeShow} = useMyStore();
    const width = useStore((state) => state.width);
    const height = useStore((state) => state.height);
    const setNodes = useStore((state) => state.setNodes);
    const setEdges = useStore((state) => state.setEdges);
    const panZoom = useStore((state) => state.panZoom);
    const nodeShowSetting = nodeShow ?? currentNodeShow;
    const {getNodesBounds} = useReactFlow();

    const {nodes, edges} = useMemo(() => convertNodeAndEdges({
        entityMap,
        sharedSetting: {notes, query, nodeShow: nodeShowSetting}
    }), [entityMap, notes, query, nodeShowSetting]);

    const queryKey = editingObjectRes?.isEdited ? ['layout', pathname, 'e', nodeShowSetting] : ['layout', pathname, nodeShowSetting]
    const staleTime = editingObjectRes?.isEdited ? 0 : 1000 * 60 * 5;
    const {data: id2RectMap} = useQuery({
        queryKey: queryKey,
        queryFn: async () => await layoutAsync(nodes, edges, getLayoutStrategy(nodeShowSetting, type), nodeShowSetting),
        staleTime: staleTime,
    })

    const newNodes:EntityNode[] | undefined = useMemo(() => {
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
            // console.log("set nodes", newNodes, edges);
            if (panZoom && id2RectMap) {
                if (editingObjectRes === undefined || editingObjectRes.fitView === EFitView.FitFull) {
                    const bounds = getNodesBounds(newNodes); //  {useRelativePosition: true}
                    const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1, 0.2);
                    panZoom.setViewport(viewportForBounds);
                    // console.log(pathname, bounds, width, height, viewportForBounds)
                    if (setFitViewForPathname) {
                        setFitViewForPathname(pathname);
                    }
                } else if (editingObjectRes?.fitView === EFitView.FitId
                    && editingObjectRes.fitViewToIdPosition) {
                    const {id, x, y} = editingObjectRes.fitViewToIdPosition;
                    const nowXy = id2RectMap.get(id);
                    if (nowXy !== undefined) { // onDeleteItemFromArray时，是会遇到undefined情况的
                        const {x: nowX, y: nowY} = nowXy;
                        const {x: tx, y: ty, zoom} = panZoom.getViewport();
                        // xyflow的viewport的含义如下：
                        // screenX = x*zoom + tx
                        // screenY = y*zoom + ty
                        // 这里要保证screenX，screenY前后一致
                        // nowX*zoom + nowTx = x*zoom + tx
                        const nowTx = x * zoom + tx - nowX * zoom;
                        const nowTy = y * zoom + ty - nowY * zoom;
                        panZoom.setViewport({x: nowTx, y: nowTy, zoom});
                    }
                }
            }


        }
    }, [newNodes, edges, nodeMenuFunc, paneMenu, editingObjectRes, flowGraph, nodeDoubleClickFunc,
        setNodes, setEdges, id2RectMap, width, height, panZoom, setFitViewForPathname, pathname, getNodesBounds]);

}

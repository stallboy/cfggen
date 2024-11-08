import {useContext, useEffect, useMemo} from "react";
import {store} from "../routes/setting/store.ts";
import {getViewportForBounds, Rect, useReactFlow, useStore} from "@xyflow/react";
import {convertNodeAndEdges} from "./entityToNodeAndEdge.ts";
import {useQuery} from "@tanstack/react-query";
import {layoutAsync} from "./layoutAsync.ts";
import {EntityNode, FlowGraphContext, NodeDoubleClickFunc, NodeMenuFunc} from "./FlowGraph.tsx";
import {Entity} from "./entityModel.ts";
import {MenuItem} from "./FlowContextMenu.tsx";
import {NodeShowType} from "../routes/setting/storageJson.ts";
import {EFitView} from "../routes/record/editingObject.ts";

interface FlowGraphInput {
    pathname: string;
    entityMap: Map<string, Entity>;
    notes?: Map<string, string>;
    nodeMenuFunc: NodeMenuFunc;
    paneMenu: MenuItem[];
    nodeDoubleClickFunc?: NodeDoubleClickFunc;

    fitView: EFitView;
    fitViewToId?: string;
    isEdited?: boolean;
    setFitViewForPathname?: (pathname: string) => void;
    nodeShow?: NodeShowType;
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

interface QueryRes {
    idXy?: Rect;
    id2RectMap?: Map<string, Rect>;
}

export function useEntityToGraph({
                                     pathname, entityMap, notes, nodeMenuFunc, paneMenu, nodeDoubleClickFunc,
                                     fitView, fitViewToId, isEdited,
                                     setFitViewForPathname, nodeShow,
                                 }: FlowGraphInput) {
    const flowGraph = useContext(FlowGraphContext);
    const {query, nodeShow: currentNodeShow} = store;
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

    const queryKey = isEdited ? ['layout', pathname, 'e'] : ['layout', pathname]
    const staleTime = isEdited ? 0 : 1000 * 60 * 5;
    const {data: idAndRectMap} = useQuery({
        queryKey: queryKey,
        queryFn: async () => {
            let idXy;
            if (idAndRectMap && idAndRectMap.id2RectMap && fitView == EFitView.FitId && fitViewToId) {
                idXy = idAndRectMap.id2RectMap.get(fitViewToId);
            }
            let id2RectMap = await layoutAsync(nodes, edges, nodeShowSetting)
            let res: QueryRes = {
                idXy: idXy,
                id2RectMap: id2RectMap
            };
            return res;
        },
        staleTime: staleTime,
    })

    const newNodes: EntityNode[] | null = idAndRectMap && idAndRectMap.id2RectMap ?
        applyPositionToNodes(nodes, idAndRectMap.id2RectMap) : null;
    // console.log('new nodes', pathname, fitView, newNodes);

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
            if (panZoom && idAndRectMap && idAndRectMap.id2RectMap){
                if (fitView == EFitView.FitFull) {
                    const appliedWHNodes = applyWidthHeightToNodes(newNodes, idAndRectMap.id2RectMap);
                    const bounds = getNodesBounds(appliedWHNodes); //  {useRelativePosition: true}
                    const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1, 0.2);
                    panZoom.setViewport(viewportForBounds);
                    // console.log(pathname, bounds, width, height, viewportForBounds)
                    if (setFitViewForPathname) {
                        setFitViewForPathname(pathname);
                    }
                } else if (fitView == EFitView.FitId && fitViewToId  && idAndRectMap.idXy) {
                    const nowXy = idAndRectMap.id2RectMap.get(fitViewToId);
                    if (nowXy){
                        const {x, y} = nowXy;
                        const {x: oldX, y: oldY} = idAndRectMap.idXy;
                        const {x:viewX, y:viewY, zoom} = panZoom.getViewport();

                        panZoom.setViewport({x: viewX + oldX - x, y: viewY + oldY - y, zoom});

                    }


                }
            }


        }
    }, [newNodes, edges, nodeMenuFunc, paneMenu, fitView, flowGraph, nodeDoubleClickFunc,
        setNodes, setEdges, idAndRectMap, width, height, panZoom, setFitViewForPathname, pathname]);

}

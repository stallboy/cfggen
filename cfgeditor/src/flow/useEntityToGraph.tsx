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

interface FlowGraphInput {
    pathname: string;
    entityMap: Map<string, Entity>;
    notes?: Map<string, string>;
    nodeMenuFunc: NodeMenuFunc;
    paneMenu: MenuItem[];
    nodeDoubleClickFunc?: NodeDoubleClickFunc;

    fitView: boolean;
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

export function useEntityToGraph({
                                     pathname, entityMap, notes, nodeMenuFunc, paneMenu, nodeDoubleClickFunc,
                                     fitView, isEdited, setFitViewForPathname, nodeShow,
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
    const {data: id2RectMap} = useQuery({
        queryKey: queryKey,
        queryFn: () => layoutAsync(nodes, edges, nodeShowSetting),
        staleTime: staleTime,
    })

    const newNodes: EntityNode[] | null = id2RectMap ? applyPositionToNodes(nodes, id2RectMap) : null;
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

            if (fitView) {
                const appliedWHNodes = applyWidthHeightToNodes(newNodes, id2RectMap);
                const bounds = getNodesBounds(appliedWHNodes); //  {useRelativePosition: true}
                const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1, 0.2);
                panZoom?.setViewport(viewportForBounds);
                // console.log(pathname, bounds, width, height, viewportForBounds)
                if (setFitViewForPathname) {
                    setFitViewForPathname(pathname);
                }
            }

        }
    }, [newNodes, edges, nodeMenuFunc, paneMenu, fitView, flowGraph, nodeDoubleClickFunc,
        setNodes, setEdges, id2RectMap, width, height, panZoom, setFitViewForPathname, pathname]);

}

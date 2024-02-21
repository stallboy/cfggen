import {
    ReactFlow,
    Background,
    Controls,
    Edge,
    Node,
    NodeTypes,
    ReactFlowProvider,
    useStore, getNodesBounds, getViewportForBounds, Rect,
} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {
    createContext,
    MouseEvent,
    ReactNode,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState
} from "react";
import {layoutAsync} from "./layoutAsync.ts";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";
import {store} from "../routes/setting/store.ts";
import {convertNodeAndEdges} from "./entityToNodeAndEdge.ts";
import {useQuery} from "@tanstack/react-query";
import {NodeShowType} from "../routes/setting/storageJson.ts";


export type EntityNode = Node<Entity, string | undefined>;
export type EntityEdge = Edge;
export type NodeMenuFunc = (entity: Entity) => MenuItem[];
export type NodeDoubleClickFunc = (entity: Entity) => void;

export interface FlowGraphContextType {
    setPaneMenu: (menu: MenuItem[]) => void;
    setNodeMenuFunc: (func: NodeMenuFunc) => void;
    setNodeDoubleClickFunc: (func: NodeDoubleClickFunc) => void;
}

function dummy() {
}

export const FlowGraphContext = createContext<FlowGraphContextType>({
    setPaneMenu: dummy,
    setNodeMenuFunc: dummy,
    setNodeDoubleClickFunc: dummy,
});

const nodeTypes: NodeTypes = {
    node: FlowNode,
};

const defaultNodes: EntityNode[] = [];
const defaultEdges: EntityEdge[] = [];

const proOptions = {hideAttribution: true};

export function FlowGraph({children}: {
    children: ReactNode
}) {
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);

    const [paneMenu, setPaneMenu] = useState<MenuItem[]>([]);
    const [nodeMenuFunc, setNodeMenuFunc] = useState<NodeMenuFunc>();
    const [nodeDoubleClickFunc, setNodeDoubleClickFunc] = useState<NodeDoubleClickFunc>();

    const onPaneContextMenu = useCallback((event: any) => {
            event.preventDefault();
            setMenuStyle({top: event.clientY - 30, left: event.clientX - 30,});
            setMenuItems(paneMenu);
        },
        [paneMenu, setMenuStyle, setMenuItems],
    );
    const onNodeContextMenu = useCallback((event: MouseEvent, flowNode: EntityNode) => {
            event.preventDefault();           // Prevent native context menu from showing
            setMenuStyle({top: event.clientY - 30, left: event.clientX - 30,});
            setMenuItems(nodeMenuFunc ? nodeMenuFunc(flowNode.data) : undefined);
        },
        [nodeMenuFunc, setMenuStyle, setMenuItems],
    );
    const onNodeDoubleClick = useCallback((_event: MouseEvent, flowNode: EntityNode) => {
            nodeDoubleClickFunc?.(flowNode.data);
        },
        [nodeDoubleClickFunc],
    );

    const closeMenu = useCallback(() => {
        setMenuStyle(undefined)
    }, [setMenuStyle]);

    const thisSetNodeMenuFunc = useCallback(function (func: NodeMenuFunc) {
        setNodeMenuFunc(() => func);
    }, [setNodeMenuFunc]);

    const thisSetNodeDoubleClickFunc = useCallback(function (func: NodeDoubleClickFunc) {
        setNodeDoubleClickFunc(() => func);
    }, [setNodeMenuFunc]);

    const ctx = useMemo(() => {
        return {
            setPaneMenu,
            setNodeMenuFunc: thisSetNodeMenuFunc,
            setNodeDoubleClickFunc: thisSetNodeDoubleClickFunc,
        }
    }, [setPaneMenu, thisSetNodeMenuFunc, setNodeDoubleClickFunc]);


    return <ReactFlowProvider>
        <ReactFlow
            defaultNodes={defaultNodes}
            defaultEdges={defaultEdges}
            nodeTypes={nodeTypes}
            minZoom={0.1}
            maxZoom={2}
            // fitView
            onNodeDoubleClick={onNodeDoubleClick}
            onNodeContextMenu={onNodeContextMenu}
            onPaneClick={closeMenu}
            onNodeClick={closeMenu}
            onMoveStart={closeMenu}
            onNodeDragStart={closeMenu}
            onPaneContextMenu={onPaneContextMenu}
            proOptions={proOptions}>
            <Background/>
            <Controls showZoom={false}/>
        </ReactFlow>
        {(menuStyle && menuItems && menuItems.length > 0) &&
            <FlowContextMenu menuStyle={menuStyle} menuItems={menuItems} closeMenu={closeMenu}/>}

        <FlowGraphContext.Provider value={ctx}>
            {children}
        </FlowGraphContext.Provider>
    </ReactFlowProvider>;

}

interface FlowGraphInput {
    pathname: string;
    entityMap: Map<string, Entity>;
    notes?: Map<string, string>;
    nodeMenuFunc: NodeMenuFunc;
    paneMenu: MenuItem[];
    nodeDoubleClickFunc?: NodeDoubleClickFunc;

    fitView: boolean;
    setFitViewForPathname?: (pathname: string) => void;
    nodeShow?: NodeShowType;
}

export function useEntityToGraph({
                                     pathname, entityMap, notes, nodeMenuFunc, paneMenu,
                                     fitView, setFitViewForPathname, nodeDoubleClickFunc, nodeShow
                                 }: FlowGraphInput) {
    const flowGraph = useContext(FlowGraphContext);
    const {query, nodeShow: currentNodeShow} = store;
    const width = useStore((state) => state.width);
    const height = useStore((state) => state.height);
    const setNodes = useStore((state) => state.setNodes);
    const setEdges = useStore((state) => state.setEdges);
    const panZoom = useStore((state) => state.panZoom);
    const nodeShowSetting = nodeShow ?? currentNodeShow;

    const {nodes, edges} = useMemo(() => convertNodeAndEdges({
        entityMap,
        sharedSetting: {notes, query, nodeShow: nodeShowSetting}
    }), [entityMap, notes, query, nodeShowSetting]);

    const {data: id2RectMap} = useQuery({
        queryKey: ['layout', pathname],
        queryFn: () => layoutAsync(nodes, edges, nodeShowSetting),
        staleTime: 1000 * 60 * 5,
    })

    let newNodes: EntityNode[] | null = id2RectMap ? applyPositionToNodes(nodes, id2RectMap) : null;
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
                const bounds = getNodesBounds(appliedWHNodes, {useRelativePosition: true}); //
                const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1, 0.2);
                panZoom?.setViewport(viewportForBounds);
                // console.log(pathname, bounds, width, height, viewportForBounds)
                if (setFitViewForPathname) {
                    setFitViewForPathname(pathname);
                }
            }

        }
    }, [newNodes, edges, nodeMenuFunc, paneMenu, fitView]);

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

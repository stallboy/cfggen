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
import {asyncLayout} from "./layout.ts";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";
import {store} from "../routes/setting/store.ts";
import {convertNodeAndEdges} from "./entityToNodeAndEdge.ts";
import {useQuery} from "@tanstack/react-query";
// import {syncLayout} from "./syncLayout.ts";


export type EntityNode = Node<Entity, string | undefined>;
export type EntityEdge = Edge;
export type NodeMenuFunc = (entity: Entity) => MenuItem[];

export interface FlowGraphContextType {
    setPaneMenu: (menu: MenuItem[]) => void;
    setNodeMenuFunc: (func: NodeMenuFunc) => void;
}

function dummy() {
}

export const FlowGraphContext = createContext<FlowGraphContextType>({
    setPaneMenu: dummy,
    setNodeMenuFunc: dummy,
});

const nodeTypes: NodeTypes = {
    node: FlowNode,
};

const defaultNodes: EntityNode[] = [];
const defaultEdges: EntityEdge[] = [];

export function FlowGraph({children}: {
    children: ReactNode
}) {
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);

    const [paneMenu, setPaneMenu] = useState<MenuItem[]>([]);
    const [nodeMenuFunc, setNodeMenuFunc] = useState<NodeMenuFunc>();

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
    const closeMenu = useCallback(() => {
        setMenuStyle(undefined)
    }, [setMenuStyle]);

    const thisSetNodeMenuFunc = useCallback(function (func: NodeMenuFunc) {
        setNodeMenuFunc(() => func);
    }, [setNodeMenuFunc]);

    const ctx = useMemo(() => {
        return {
            setPaneMenu,
            setNodeMenuFunc: thisSetNodeMenuFunc,
        }
    }, [setPaneMenu, thisSetNodeMenuFunc]);


    return <ReactFlowProvider>
        <ReactFlow
            defaultNodes={defaultNodes}
            defaultEdges={defaultEdges}
            nodeTypes={nodeTypes}
            minZoom={0.1}
            maxZoom={2}
            // fitView
            onNodeContextMenu={onNodeContextMenu}
            onPaneClick={closeMenu}
            onNodeClick={closeMenu}
            onMoveStart={closeMenu}
            onNodeDragStart={closeMenu}
            onPaneContextMenu={onPaneContextMenu}>
            <Background/>
            <Controls/>
        </ReactFlow>
        {(menuStyle && menuItems && menuItems.length > 0) &&
            <FlowContextMenu menuStyle={menuStyle} menuItems={menuItems} closeMenu={closeMenu}/>}

        <FlowGraphContext.Provider value={ctx}>
            {children}
        </FlowGraphContext.Provider>
    </ReactFlowProvider>;

}


export function useEntityToGraph(pathname: string,
                                 entityMap: Map<string, Entity>,
                                 nodeMenuFunc: NodeMenuFunc,
                                 paneMenu: MenuItem[],
                                 fitView: boolean = true,
) {
    const flowGraph = useContext(FlowGraphContext);
    const {query, nodeShow} = store;
    const width = useStore((state) => state.width);
    const height = useStore((state) => state.height);
    const setNodes = useStore((state) => state.setNodes);
    const setEdges = useStore((state) => state.setEdges);
    const panZoom = useStore((state) => state.panZoom);

    const {nodes, edges} = convertNodeAndEdges({entityMap, query, nodeShow});

    // console.log('new nodes', nodes, edges);
    const {data: id2RectMap} = useQuery({
        queryKey: ['layout', pathname],
        queryFn: () => asyncLayout(nodes, edges, nodeShow),
        staleTime: 1000 * 60 * 5,
    })

    let newNodes: EntityNode[] | null = id2RectMap ? applyPositionToNodes(nodes, id2RectMap) : null;

    useEffect(() => {
        if (newNodes) {
            flowGraph.setNodeMenuFunc(nodeMenuFunc);
            flowGraph.setPaneMenu(paneMenu);

            setNodes(newNodes);
            setEdges(edges);
            // console.log("set nodes", newNodes, edges);

            if (fitView) {
                const appliedWHNodes = applyWidthHeightToNodes(newNodes, id2RectMap);
                const bounds = getNodesBounds(appliedWHNodes, {useRelativePosition: true}); //
                const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1, 0.2);
                panZoom?.setViewport(viewportForBounds);
                // console.log(bounds, width, height, viewportForBounds)
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

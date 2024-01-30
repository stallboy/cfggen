import ReactFlow, {
    Background,
    Controls,
    Edge,
    Node,
    NodeTypes,
    ReactFlowProvider,
    useEdgesState, useNodesInitialized,
    useNodesState, useReactFlow, useStore
} from "reactflow";
import {Entity} from "./entityModel.ts";
import {
    createContext,
    MouseEvent,
    ReactNode,
    useCallback,
    useContext,
    useEffect,
    useMemo, useReducer,
    useRef,
    useState
} from "react";
import {layout} from "./layout.ts";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";
import {store} from "../routes/setting/store.ts";
import {convertNodeAndEdges} from "./entityToNodeAndEdge.ts";
// import {syncLayout} from "./syncLayout.ts";


export type EntityNode = Node<Entity, string>;
export type EntityEdge = Edge;
export type NodeMenuFunc = (entity: Entity) => MenuItem[];

export interface FlowGraphContextType {
    setPaneMenu: (menu: MenuItem[]) => void;
    setNodeMenuFunc: (func: NodeMenuFunc) => void;
    setPathname: (pathname: string) => void;
    setNodes: (nodes: Node[]) => void;
    setEdges: (edges: any[]) => void;
    forceLayout: ()=>void;
}

function dummy() {
}

export const FlowGraphContext = createContext<FlowGraphContextType>({
    setPaneMenu: dummy,
    setNodeMenuFunc: dummy,
    setPathname: dummy,
    setNodes: dummy,
    setEdges: dummy,
    forceLayout:dummy,
});

const nodeTypes: NodeTypes = {
    node: FlowNode,
};

export function FlowGraphInner({children}: { children: ReactNode }) {
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);

    const [nodes, _setNodes, onNodesChange] = useNodesState<Entity>([]);
    const [edges, _setEdges, onEdgesChange] = useEdgesState<any>([]);
    const [paneMenu, setPaneMenu] = useState<MenuItem[]>([]);
    const [nodeMenuFunc, setNodeMenuFunc] = useState<NodeMenuFunc>();
    const [pathname, setPathname] = useState<string>('');


    const onPaneContextMenu = useCallback((event: MouseEvent) => {
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

    const width = useStore((state) => state.width);
    const height = useStore((state) => state.height);
    const flowInstance = useReactFlow();
    const nodesInitialized = useNodesInitialized();
    const oldPathnameRef = useRef<string | null>(null);
    const [forceLayoutState, forceLayout] = useReducer(v => v + 1, 0);

    useEffect(() => {
        if (nodesInitialized) {
            layout(flowInstance, oldPathnameRef, width, height, pathname)
        }
    }, [flowInstance, nodesInitialized, forceLayoutState, pathname, width, height]);

    const thisSetNodeMenuFunc = useCallback(function (func: NodeMenuFunc) {
        setNodeMenuFunc(() => func);
    }, [setNodeMenuFunc]);

    const ctx = useMemo(() => {
        return {
            setPaneMenu, setNodeMenuFunc: thisSetNodeMenuFunc, setPathname,
            setNodes: _setNodes, setEdges: _setEdges, forceLayout
        }
    }, [setPaneMenu, thisSetNodeMenuFunc, setPathname, _setNodes, _setEdges, forceLayout]);

    // const onNodesChanged = useCallback((changes: NodeChange[]) => {
    //     onNodesChange(changes);
    //     console.log('node changed', changes);
    //     // layout(flowInstance, oldPathnameRef, width, height, pathname);
    // },  [onNodesChange])

    return <>
        <ReactFlow key={pathname}
                   nodes={nodes}
                   edges={edges}
                   onNodesChange={onNodesChange}
                   onEdgesChange={onEdgesChange}
                   nodeTypes={nodeTypes}
            // onInit={onInit}
                   minZoom={0.1}
                   maxZoom={2}
                   fitView
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
    </>;

}

export function FlowGraph({children}: { children: ReactNode }) {
    return <ReactFlowProvider>
        <FlowGraphInner children={children}/>
    </ReactFlowProvider>;
}

export function useEntityToGraph(pathname: string,
                                 entityMap: Map<string, Entity>,
                                 nodeMenuFunc: NodeMenuFunc,
                                 paneMenu: MenuItem[]
) {
    const flowGraph = useContext(FlowGraphContext);
    // const flowInstance = useReactFlow();
    const {query, nodeShow} = store;
    const {nodes, edges} = convertNodeAndEdges({entityMap, query, nodeShow});
    // console.log('new nodes', nodes, edges);
    useEffect(() => {
        flowGraph.setPathname(pathname);
        flowGraph.setNodeMenuFunc(nodeMenuFunc);
        flowGraph.setPaneMenu(paneMenu);
        flowGraph.setNodes(nodes);
        flowGraph.setEdges(edges);
        flowGraph.forceLayout();
        // console.log("set nodes, edges");
        // console.log(nodes);
        // console.log(edges);
    }, [pathname, nodes, edges]);
}

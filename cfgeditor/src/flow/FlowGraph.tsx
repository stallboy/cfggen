import {
    ReactFlow,
    Background,
    Controls,
    Edge,
    Node,
    NodeTypes,
    ReactFlowProvider,
    useNodesInitialized,
    useReactFlow, useStore
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
    useRef,
    useState
} from "react";
import {layout} from "./layout.ts";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";
import {store} from "../routes/setting/store.ts";
import {convertNodeAndEdges} from "./entityToNodeAndEdge.ts";
// import {syncLayout} from "./syncLayout.ts";


export type EntityNode = Node<Entity, string | undefined>;
export type EntityEdge = Edge;
export type NodeMenuFunc = (entity: Entity) => MenuItem[];

export interface FlowGraphContextType {
    setPaneMenu: (menu: MenuItem[]) => void;
    setNodeMenuFunc: (func: NodeMenuFunc) => void;
    setPathname: (pathname: string) => void;
    // setNodes: (nodes: Node[]) => void;
    // setEdges: (edges: any[]) => void;
    forceLayout: () => void;
}

function dummy() {
}

export const FlowGraphContext = createContext<FlowGraphContextType>({
    setPaneMenu: dummy,
    setNodeMenuFunc: dummy,
    setPathname: dummy,
    // setNodes: dummy,
    // setEdges: dummy,
    forceLayout: dummy,
});

const nodeTypes: NodeTypes = {
    node: FlowNode,
};

export function FlowGraphInner({pathname, setPathname, children}: {
    pathname: string;
    setPathname: (p: string) => void;
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

    const width = useStore((state) => state.width);
    const height = useStore((state) => state.height);
    const flowInstance = useReactFlow();
    const nodesInitialized = useNodesInitialized();
    const oldPathnameRef = useRef<string | null>(null);
    const needLayout = useRef<boolean>(false);

    useEffect(() => {
        if (needLayout.current) {
            layout(flowInstance, oldPathnameRef, width, height, pathname)
            needLayout.current = false;
        }
    }, [flowInstance, nodesInitialized, needLayout, pathname, width, height]);

    const forceLayout = useCallback(() => {
        needLayout.current = true;
    }, [needLayout]);

    const thisSetNodeMenuFunc = useCallback(function (func: NodeMenuFunc) {
        setNodeMenuFunc(() => func);
    }, [setNodeMenuFunc]);

    const ctx = useMemo(() => {
        return {
            setPaneMenu, setNodeMenuFunc: thisSetNodeMenuFunc, setPathname,
            forceLayout
        }
    }, [setPaneMenu, thisSetNodeMenuFunc, setPathname, forceLayout]);

    // const onNodesChange = useCallback(
    //     (changes: NodeChange<EntityNode>[]) => setNodes((nds) => applyNodeChanges(changes, nds)),
    //     []
    // );

    // const onNodesChanged = useCallback((changes: NodeChange[]) => {
    //     onNodesChange(changes);
    //     console.log('node changed', changes);
    //     // layout(flowInstance, oldPathnameRef, width, height, pathname);
    // },  [onNodesChange])

    // console.log('flow graph', pathname, nodes);
    return <>
        <ReactFlow
            defaultNodes={[]}
            defaultEdges={[]}
            nodeTypes={nodeTypes}
            // onNodesChange={onNodesChange}
            // onEdgesChange={onEdgesChange}

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
    const [pathname, setPathname] = useState<string>('');

    return <ReactFlowProvider>
        <FlowGraphInner pathname={pathname} setPathname={setPathname} children={children}/>
    </ReactFlowProvider>;
}

export function useEntityToGraph(pathname: string,
                                 entityMap: Map<string, Entity>,
                                 nodeMenuFunc: NodeMenuFunc,
                                 paneMenu: MenuItem[]
) {
    const flowGraph = useContext(FlowGraphContext);
    const flowInstance = useReactFlow();
    const {query, nodeShow} = store;
    const {nodes, edges} = convertNodeAndEdges({entityMap, query, nodeShow});
    // console.log('new nodes', nodes, edges);
    useEffect(() => {
        flowGraph.setPathname(pathname);
        flowGraph.setNodeMenuFunc(nodeMenuFunc);
        flowGraph.setPaneMenu(paneMenu);
        flowInstance.setNodes(nodes);
        flowInstance.setEdges(edges);
        flowGraph.forceLayout();
        console.log("set nodes", nodes, edges);
        // console.log(nodes);
        // console.log(edges);
    }, [pathname, nodes, edges]);
}

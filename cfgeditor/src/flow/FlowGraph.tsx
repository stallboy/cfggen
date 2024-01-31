import {
    ReactFlow,
    Background,
    Controls,
    Edge,
    Node,
    NodeTypes,
    ReactFlowProvider,
    useStore, getNodesBounds, getViewportForBounds
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
import {asyncLayout } from "./layout.ts";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";
import {store} from "../routes/setting/store.ts";
import {convertNodeAndEdges} from "./entityToNodeAndEdge.ts";
import {useQuery} from "@tanstack/react-query";
import {useStoreApi} from "@ant-design/pro-editor/es/DataFill/store";
// import {syncLayout} from "./syncLayout.ts";


export type EntityNode = Node<Entity, string | undefined>;
export type EntityEdge = Edge;
export type NodeMenuFunc = (entity: Entity) => MenuItem[];

export interface FlowGraphContextType {
    setPaneMenu: (menu: MenuItem[]) => void;
    setNodeMenuFunc: (func: NodeMenuFunc) => void;
    setPathname: (pathname: string) => void;
}

function dummy() {
}

export const FlowGraphContext = createContext<FlowGraphContextType>({
    setPaneMenu: dummy,
    setNodeMenuFunc: dummy,
    setPathname: dummy,
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

    const thisSetNodeMenuFunc = useCallback(function (func: NodeMenuFunc) {
        setNodeMenuFunc(() => func);
    }, [setNodeMenuFunc]);

    const ctx = useMemo(() => {
        return {
            setPaneMenu, setNodeMenuFunc: thisSetNodeMenuFunc, setPathname,
        }
    }, [setPaneMenu, thisSetNodeMenuFunc, setPathname]);


    return <>
        <ReactFlow
            defaultNodes={[]}
            defaultEdges={[]}
            nodeTypes={nodeTypes}
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
    // const flowInstance = useReactFlow();
    const {query, nodeShow} = store;
    const width = useStore((state) => state.width);
    const height = useStore((state) => state.height);
    const setNodes = useStore((state) => state.setNodes);
    const setEdges = useStore((state) => state.setEdges);
    const panZoom = useStore((state) => state.panZoom);
    // const storeApi = useStoreApi();


    const {nodes, edges} = convertNodeAndEdges({entityMap, query, nodeShow});
    // console.log('new nodes', nodes, edges);
    const {isLoading, isError, error, data: newNodes} = useQuery({
        queryKey:  ['layout', pathname],
        queryFn: () => asyncLayout(nodes, edges, pathname),
    })

    useEffect(() => {
        if (newNodes ){

            flowGraph.setPathname(pathname);
            flowGraph.setNodeMenuFunc(nodeMenuFunc);
            flowGraph.setPaneMenu(paneMenu);
            setNodes(newNodes );
            setEdges(edges);
            // storeApi.setState({nodeLookup: new Map()})

            const bounds = getNodesBounds(newNodes);
            const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1, 0);
            panZoom?.setViewport(viewportForBounds);

            console.log("set nodes", nodes, edges);
        }
    }, [newNodes, nodes, edges]);

}

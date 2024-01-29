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
import {createContext, MouseEvent, ReactNode, useCallback, useContext, useEffect, useState} from "react";
import {layout} from "./layout.ts";
import {useQueryClient} from "@tanstack/react-query";
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
}

function dummy() {
}

export const FlowGraphContext = createContext<FlowGraphContextType>({
    setPaneMenu: dummy,
    setNodeMenuFunc: dummy,
    setPathname: dummy,
    setNodes: dummy,
    setEdges: dummy,
});

const nodeTypes: NodeTypes = {
    node: FlowNode,
};

export function FlowGraphInner({children}: { children: ReactNode }) {
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);
    const queryClient = useQueryClient();

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

    // const onInit = useCallback((instance: ReactFlowInstance) => {
    //     console.log('onInit', pathname);
    //     layout(instance, pathname, queryClient)
    // }, [pathname, queryClient]);

    // const onInit = useCallback((instance: ReactFlowInstance) => {
    //     syncLayout(instance);
    // }, []);

    const width = useStore((state) => state.width);
    const height = useStore((state) => state.height);


    const thisSetNodeMenuFunc = function (func: NodeMenuFunc) {
        setNodeMenuFunc(() => func);
    }

    const flowInstance = useReactFlow();
    const nodesInitialized = useNodesInitialized();
    useEffect(() => {
        if (nodesInitialized) {
            layout(flowInstance, width, height, pathname, queryClient)
        }
    }, [flowInstance, nodesInitialized, pathname, width, height, queryClient]);


    const ctx: FlowGraphContextType = {
        setPaneMenu, setNodeMenuFunc: thisSetNodeMenuFunc, setPathname,
        setNodes: _setNodes, setEdges: _setEdges
    };
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
    // console.log(entityMap.values(), nodes, edges);
    useEffect(() => {
        flowGraph.setPathname(pathname);
        flowGraph.setNodeMenuFunc(nodeMenuFunc);
        flowGraph.setPaneMenu(paneMenu);
        flowGraph.setNodes(nodes);
        flowGraph.setEdges(edges);
        // console.log("set nodes, edges");
        // console.log(nodes);
        // console.log(edges);
    }, [pathname]);
}

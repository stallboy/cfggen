import {Background, Controls, Edge, Node, NodeTypes, ReactFlow, ReactFlowProvider} from "@xyflow/react";
import {Entity} from "./entityModel.ts";
import {memo, MouseEvent as ReactMouseEvent, ReactNode, useCallback, useMemo, useState} from "react";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";
import {FlowStyleManager} from "./FlowStyleManager.tsx";
import {FlowGraphContext as FlowGraphContext1} from "./FlowGraphContext.ts";

export type EntityNode = Node<{ entity: Entity }, "node">;
export type EntityEdge = Edge;
export type NodeMenuFunc = (entityNode: EntityNode) => MenuItem[];
export type NodeDoubleClickFunc = (entityNode: EntityNode) => void;

const nodeTypes: NodeTypes = {
    node: FlowNode,
};

const defaultNodes: EntityNode[] = [];
const defaultEdges: EntityEdge[] = [];

const proOptions = {hideAttribution: true};

export const FlowGraph = memo(function FlowGraph({children}: {
    children: ReactNode
}) {
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);

    const [paneMenu, setPaneMenu] = useState<MenuItem[]>([]);
    const [nodeMenuFunc, setNodeMenuFunc] = useState<NodeMenuFunc>();
    const [nodeDoubleClickFunc, setNodeDoubleClickFunc] = useState<NodeDoubleClickFunc>();

    const onPaneContextMenu = useCallback((event: ReactMouseEvent<Element, MouseEvent> | MouseEvent) => {
            event.stopPropagation();
            event.preventDefault();
            setMenuStyle({top: event.clientY - 30, left: event.clientX - 30,});
            setMenuItems(paneMenu);
        },
        [paneMenu, setMenuStyle, setMenuItems],
    );
    const onNodeContextMenu = useCallback((event: ReactMouseEvent, flowNode: EntityNode) => {
            event.stopPropagation();
            event.preventDefault();           // Prevent native context menu from showing
            setMenuStyle({top: event.clientY - 30, left: event.clientX - 30,});
            setMenuItems(nodeMenuFunc ? nodeMenuFunc(flowNode) : undefined);
        },
        [nodeMenuFunc, setMenuStyle, setMenuItems],
    );
    const onNodeDoubleClick = useCallback((_event: ReactMouseEvent, flowNode: EntityNode) => {
            nodeDoubleClickFunc?.(flowNode);
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
    }, [setNodeDoubleClickFunc]);

    const ctx = useMemo(() => {
        return {
            setPaneMenu,
            setNodeMenuFunc: thisSetNodeMenuFunc,
            setNodeDoubleClickFunc: thisSetNodeDoubleClickFunc,
        }
    }, [setPaneMenu, thisSetNodeMenuFunc, thisSetNodeDoubleClickFunc]);


    return <ReactFlowProvider>
        <FlowStyleManager />
        <ReactFlow
            defaultNodes={defaultNodes}
            defaultEdges={defaultEdges}
            nodeTypes={nodeTypes}
            minZoom={0.1}
            maxZoom={2}
            deleteKeyCode={null}
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

        <FlowGraphContext1 value={ctx}>
            {children}
        </FlowGraphContext1>
    </ReactFlowProvider>;

});



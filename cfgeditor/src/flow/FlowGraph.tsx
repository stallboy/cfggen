import ReactFlow, {Background, Controls, Edge, Node, NodeTypes, ReactFlowInstance} from "reactflow";
import {Entity} from "./entityModel.ts";
import {MouseEvent, useCallback, useState} from "react";
import {layout} from "./layout.ts";
import {useQueryClient} from "@tanstack/react-query";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";


export type EntityNode = Node<Entity, string>;
export type EntityEdge = Edge;

const nodeTypes: NodeTypes = {
    node: FlowNode,
};

export function FlowGraph({pathname, initialNodes, initialEdges, paneMenu, nodeMenuFunc}: {
    pathname: string;
    initialNodes: EntityNode[],
    initialEdges: EntityEdge[],
    paneMenu?: MenuItem[],
    nodeMenuFunc?: (entity: Entity) => MenuItem[],
}) {
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);
    const queryClient = useQueryClient();

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

    const onInit = useCallback((instance: ReactFlowInstance) => {
        layout(instance, pathname, queryClient)
    }, [pathname, queryClient]);

    return <>
        <ReactFlow defaultNodes={initialNodes}
                   defaultEdges={initialEdges}
                   nodeTypes={nodeTypes}
                   onInit={onInit}
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
        {(menuStyle && menuItems) &&
            <FlowContextMenu menuStyle={menuStyle} menuItems={menuItems} closeMenu={closeMenu}/>}
    </>;

}

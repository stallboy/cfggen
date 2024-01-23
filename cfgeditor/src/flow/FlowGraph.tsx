import ReactFlow, {Background, Controls, Edge, Node, NodeTypes, ReactFlowInstance} from "reactflow";
import {Entity} from "./entityModel.ts";
import {MouseEvent, useCallback, useRef, useState} from "react";
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
    const ref = useRef<HTMLDivElement>(null);

    const onContextMenu = useCallback((event: MouseEvent, flowNode?: EntityNode) => {
            if (ref.current == null) {
                return;
            }
            // Prevent native context menu from showing
            event.preventDefault();

            // Calculate position of the context menu. We want to make sure it
            // doesn't get positioned off-screen.
            const pane = ref.current.getBoundingClientRect();
            const {offsetX, offsetY} = event.nativeEvent;
            setMenuStyle({
                top: offsetY < pane.height - 200 ? offsetY - 30 : undefined,
                left: offsetX < pane.width - 200 ? offsetX - 50 : undefined,
                right: offsetX >= pane.width - 200 ? (pane.width - offsetX - 50) : undefined,
                bottom: offsetY >= pane.height - 200 ? pane.height - offsetY - 30 : undefined,
            });

            if (flowNode) {
                setMenuItems(nodeMenuFunc ? nodeMenuFunc(flowNode.data) : undefined);
            } else {
                setMenuItems(paneMenu);
            }

        },
        [nodeMenuFunc, paneMenu, setMenuStyle, setMenuItems],
    );

    // Close the context menu if it's open whenever the window is clicked.
    const closeMenu = useCallback(() => setMenuStyle(undefined), [setMenuStyle]);

    return <ReactFlow ref={ref}
                      defaultNodes={initialNodes}
                      defaultEdges={initialEdges}
                      nodeTypes={nodeTypes}
                      onInit={(instance: ReactFlowInstance) => layout(instance, pathname, queryClient)}
                      minZoom={0.1}
                      maxZoom={2}
                      fitView
                      onNodeContextMenu={(e: MouseEvent, node: EntityNode) => {
                          onContextMenu(e, node);
                      }}
                      onPaneClick={closeMenu}
                      onNodeClick={closeMenu}
                      onMoveStart={closeMenu}
                      onNodeDragStart={closeMenu}

                      onPaneContextMenu={(e: MouseEvent) => {
                          onContextMenu(e);
                      }}>


        <Background/>
        <Controls/>
        {(menuStyle && menuItems) &&
            <FlowContextMenu menuStyle={menuStyle} menuItems={menuItems}/>}
    </ReactFlow>
        ;

}

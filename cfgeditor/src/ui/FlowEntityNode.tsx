import ReactFlow, {
    Background,
    Controls,
    Edge,
    Handle,
    Node, NodeProps,
    NodeTypes,
    Position, ReactFlowInstance,
    useEdgesState,
    useNodesState,
} from "reactflow";
import {Entity, EntityEdgeType, EntityGraph} from "../model/entityModel.ts";
import {Flex, List, Tooltip, Typography} from "antd";

import {memo, type MouseEvent as ReactMouseEvent, useCallback, useRef, useState} from "react";
import {layout} from "./layout.ts";
import {edgeStorkColor, getNodeBackgroundColor} from "./colors.ts";
import {useLocationData} from "../model/store.ts";
import {useQueryClient} from "@tanstack/react-query";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";

const {Text} = Typography;


function tooltip({comment, name}: { name: string, comment?: string }) {
    return comment ? `${name}: ${comment}` : name;
}

const re = /[（(]/;

function text({comment, name}: { name: string, comment?: string }) {
    if (comment) {
        const c = comment.split(re)[0];

        return `${c.substring(0, 6)} ${name}`
    }
    return name;
}


export const PropertiesNode = memo(function (nodeProps: NodeProps<Entity>) {
    const {fields, handleIn, handleOut, label} = nodeProps.data;
    const color: string = getNodeBackgroundColor(nodeProps.data);
    return <Flex vertical gap={'small'} className='propertiesNode' style={{width: 240, backgroundColor: color}}>
        <Text strong style={{fontSize: 18, color: "#fff"}} ellipsis={{tooltip: true}}>
            {label}
        </Text>
        {fields && fields.length > 0 &&
            <List size='small' style={{backgroundColor: '#ffffff'}} bordered dataSource={(fields)!}
                  renderItem={(item) => {
                      return <List.Item key={item.key} style={{position: 'relative'}}>
                          <Flex justify="space-between" style={{width: '100%'}}>
                              <Tooltip title={tooltip(item)}>
                                  <Text style={{color: color}} ellipsis={{tooltip: true}}>
                                      {text(item)}
                                  </Text>
                              </Tooltip>
                              <Text ellipsis={{tooltip: true}}>
                                  {item.value}
                              </Text>
                          </Flex>

                          {item.handleIn && <Handle type='target' position={Position.Left} id={`@in_${item.name}`}
                                                    style={{
                                                        position: 'absolute',
                                                        left: '-10px',
                                                        backgroundColor: color
                                                    }}/>}
                          {item.handleOut && <Handle type='source' position={Position.Right} id={item.name}
                                                     style={{
                                                         position: 'absolute',
                                                         left: '230px',
                                                         backgroundColor: color
                                                     }}/>}
                      </List.Item>;

                  }}/>}
        {(handleIn && <Handle type='target' position={Position.Left} id='@in'
                              style={{
                                  position: 'absolute',
                                  backgroundColor: color
                              }}/>)}
        {(handleOut && <Handle type='source' position={Position.Right} id='@out'
                               style={{
                                   position: 'absolute',
                                   backgroundColor: color
                               }}/>)}
    </Flex>;

});


export type FlowNode = Node<Entity, string>;
export type FlowEdge = Edge;


export function convertNodeAndEdges(graph: EntityGraph) {
    const nodes: FlowNode[] = []
    const edges: FlowEdge[] = []

    let ei = 1;
    for (let entity of graph.entityMap.values()) {
        entity.query = graph.query;
        entity.nodeShow = graph.nodeShow;

        nodes.push({
            id: entity.id,
            data: entity,
            type: 'props',
            position: {x: 100, y: 100},
            style: {visibility: 'hidden'},
        })
        for (let edge of entity.sourceEdges) {
            let fe: FlowEdge = {
                id: '' + (ei++),
                source: entity.id,
                sourceHandle: edge.sourceHandle,
                target: edge.target,
                targetHandle: edge.targetHandle,

                type: 'simplebezier',
                style: {stroke: edgeStorkColor, visibility: 'hidden'},
            }

            if (edge.type == EntityEdgeType.Ref) {
                fe.animated = true;
            }
            edges.push(fe);
        }
    }
    return {nodes, edges};
}

const nodeTypes: NodeTypes = {
    props: PropertiesNode,
};

export function FlowEntityGraph({initialNodes, initialEdges, paneMenu, nodeMenuFunc}: {
    initialNodes: FlowNode[],
    initialEdges: Edge[],
    paneMenu?: MenuItem[],
    nodeMenuFunc?: (entity: Entity) => MenuItem[],
}) {
    const [nodes, _setNodes, onNodesChange] = useNodesState<Entity>(initialNodes);
    const [edges, _setEdges, onEdgesChange] = useEdgesState(initialEdges);
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);

    const {pathname} = useLocationData();
    const queryClient = useQueryClient();
    const ref = useRef<HTMLDivElement>(null);

    const onContextMenu = useCallback((event: ReactMouseEvent, flowNode?: FlowNode) => {
            if (ref.current == null) {
                return;
            }
            // Prevent native context menu from showing
            event.preventDefault();

            // Calculate position of the context menu. We want to make sure it
            // doesn't get positioned off-screen.
            const pane = ref.current.getBoundingClientRect();
            setMenuStyle({
                top: event.clientY < pane.height - 200 ? event.clientY - 30 : undefined,
                left: event.clientX < pane.width - 200 ? event.clientX - 50 : undefined,
                right: event.clientX >= pane.width - 200 ? (pane.width - event.clientX - 50) : undefined,
                bottom: event.clientY >= pane.height - 200 ? pane.height - event.clientY - 30 : undefined,
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
                      nodes={nodes}
                      onNodesChange={onNodesChange}
                      edges={edges}
                      onEdgesChange={onEdgesChange}
                      nodeTypes={nodeTypes}
                      onInit={(instance: ReactFlowInstance) => layout(instance, pathname, queryClient)}
                      minZoom={0.1}
                      maxZoom={2}
                      fitView
                      onNodeContextMenu={(e: ReactMouseEvent, node: FlowNode) => {
                          onContextMenu(e, node);
                      }}
                      onPaneClick={closeMenu}
                      onNodeClick={closeMenu}
                      onMoveStart={closeMenu}
                      onNodeDragStart={closeMenu}

                      onPaneContextMenu={(e: ReactMouseEvent) => {
                          onContextMenu(e);
                      }}>


        <Background/>
        <Controls/>
        {(menuStyle && menuItems) &&
            <FlowContextMenu menuStyle={menuStyle} menuItems={menuItems}/>}
    </ReactFlow>
        ;

}

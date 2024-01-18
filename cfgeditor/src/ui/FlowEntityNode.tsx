import ReactFlow, {
    Background,
    Controls,
    Edge,
    Handle,
    Node,
    NodeTypes,
    Position,
    useEdgesState,
    useNodesState,
} from "reactflow";
import {Entity, EntityEdgeType, EntityGraph} from "../model/entityModel.ts";
import {Flex, List, Tooltip, Typography} from "antd";

import {memo} from "react";
import {layout} from "./layout.ts";


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


export const PropertiesNode = memo(function ({entity}: { entity: Entity }) {
    return <Flex vertical gap={'small'} className='formNode' style={{width: 240, backgroundColor: '#1677ff'}}>
        <Text strong style={{fontSize: 18, color: "#fff"}} ellipsis={{tooltip: true}}>
            {entity.label}
        </Text>
        {(entity.fields && entity.fields.length > 0 &&
            <List size='small' style={{backgroundColor: '#ffffff'}} bordered dataSource={entity.fields!}
                  renderItem={(item) => {
                      return <List.Item key={item.key} style={{position: 'relative'}}>
                          <Flex justify="space-between" style={{width: '100%'}}>
                              <Tooltip title={tooltip(item)}>
                                  <Text style={{color: '#1677ff'}} ellipsis={{tooltip: true}}>
                                      {text(item)}
                                  </Text>
                              </Tooltip>
                              <Text ellipsis={{tooltip: true}}>
                                  {item.value}
                              </Text>
                          </Flex>

                          {(item.handleIn) && <Handle type='target' position={Position.Left} id={`@in_${item.name}`}
                                                      style={{
                                                          position: 'absolute',
                                                          left: '-10px',
                                                          backgroundColor: '#1677ff'
                                                      }}/>}
                          {(item.handleOut) && <Handle type='source' position={Position.Right} id={item.name}
                                                       style={{
                                                           position: 'absolute',
                                                           left: '230px',
                                                           backgroundColor: '#1677ff'
                                                       }}/>}
                      </List.Item>;

                  }}/>
        )}
        {(entity.handleIn && <Handle type='target' position={Position.Left} id='@in'
                                     style={{
                                         position: 'absolute',
                                         backgroundColor: '#1677ff'
                                     }}/>)}
        {(entity.handleOut && <Handle type='source' position={Position.Right} id='@in'
                                      style={{
                                          position: 'absolute',
                                          backgroundColor: '#1677ff'
                                      }}/>)}
    </Flex>;

});


export type FlowNode = Node<Entity, string>;
export type FlowEdge = Edge;

const nodeTypes: NodeTypes = {
    props: (data) => <PropertiesNode entity={data.data as Entity}/>,
};

export function convertNodeAndEdges(graph: EntityGraph) {
    const nodes: FlowNode[] = []
    const edges: FlowEdge[] = []

    let ei = 1;
    for (let entity of graph.entityMap.values()) {
        nodes.push({id: entity.id, data: entity, type: 'props', position: {x: 100, y: 100}})
        for (let edge of entity.sourceEdges) {
            let fe: FlowEdge = {
                id: '' + (ei++),
                    source: entity.id,
                sourceHandle: edge.sourceHandle,
                target: edge.target,
                targetHandle: edge.targetHandle,

                type: 'simplebezier',
                animated: true,
                style: {stroke: '#1677ff'},
            }

            if (edge.type == EntityEdgeType.Normal){


            }
            edges.push(fe);
        }
    }
    return {nodes, edges};
}


export function FlowEntityGraph({initialNodes, initialEdges}: {
    initialNodes: FlowNode[],
    initialEdges: Edge[]
}) {
    const [nodes, _setNodes, onNodesChange] = useNodesState<Entity>(initialNodes);
    const [edges, _setEdges, onEdgesChange] = useEdgesState(initialEdges);

    return <ReactFlow nodes={nodes}
                      onNodesChange={onNodesChange}
                      edges={edges}
                      onEdgesChange={onEdgesChange}
                      nodeTypes={nodeTypes}
                      onInit={layout}
                      fitView
                      minZoom={0.1}
                      maxZoom={2}>

        <Background/>
        <Controls/>
    </ReactFlow>;

}

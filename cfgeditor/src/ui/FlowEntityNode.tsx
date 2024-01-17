import ReactFlow, {
    Background,
    Controls,
    Edge, Node,
    NodeTypes,
    useEdgesState,
    useNodesState, useReactFlow,
} from "reactflow";
import {Entity, EntityGraph} from "../model/entityModel.ts";
import {Flex, List, Tooltip, Typography} from "antd";

import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
import {memo, useEffect} from "react";


const {Text} = Typography;


function tooltip(field: { name: string, comment?: string }) {
    return field.comment ? `${field.name}: ${field.comment}` : field.name;
}


export const PropertiesNode = memo(function({entity}: { entity: Entity }) {
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
                                      {item.name}
                                  </Text>
                              </Tooltip>
                              <Text ellipsis={{tooltip: true}}>
                                  {item.value}
                              </Text>
                          </Flex>

                          {/*<Handle type={'source'} position={Position.Right} id={item.prop}*/}
                          {/*        style={{position: 'absolute', left: '280px'}}/>*/}
                      </List.Item>;

                  }}/>
        )}
    </Flex>;

});


type FlowNode = Node<Entity, string>;

const nodeTypes: NodeTypes = {
    props: (data) => <PropertiesNode entity={data.data as Entity}/>,
};

export function convertNodeAndEdges(graph: EntityGraph) {
    const nodes: FlowNode[] = []
    const edges: Edge[] = []

    let ei = 1;
    for (let entity of graph.entityMap.values()) {
        nodes.push({id: entity.id, data: entity, type: 'props', position: {x: 100, y: 100}})

        // for (let output of entity.outputs) {
        //     for (let connectToSocket of output.connectToSockets) {
        //         edges.push({
        //             id: '' + (ei++), source: entity.id, sourceHandle: output.output.key,
        //             target: connectToSocket.entityId, targetHandle: connectToSocket.inputKey, type: 'smoothstep'
        //         });
        //     }
        // }
    }
    return {nodes, edges};
}


const elk = new ELK();

function layout(nodes: FlowNode[], edges: Edge[]) {
    const defaultOptions = {
        'elk.algorithm': 'layered',
        'elk.layered.spacing.nodeNodeBetweenLayers': '100',
        'elk.spacing.nodeNode': '80',
        'elk.direction': 'RIGHT',
    };

    const graph: ElkNode = {
        id: 'root',
        layoutOptions: defaultOptions,
        children: nodes as ElkNode[],
        edges: edges as unknown as ElkExtendedEdge[],
    };


    elk.layout(graph).then(({children}) => {
        // By mutating the children in-place we saves ourselves from creating a
        // needless copy of the nodes array.
        if (children) {
            children.forEach((node: ElkNode) => {
                (node as FlowNode).position = {x: node.x!, y: node.y!};

                // console.log(node.id, node.x, node.y, "--", node.width, node.height);
            });

            // setNodes(children as FlowNode[]);
            // window.requestAnimationFrame(() => {
            //     fitView();
            // });
        }
    });
}

export function FlowEntityGraph({initialNodes, initialEdges}: {
    initialNodes: FlowNode[],
    initialEdges: Edge[]
}) {
    const [nodes, _setNodes, onNodesChange] = useNodesState<Entity>(initialNodes);
    const [edges, _setEdges, onEdgesChange] = useEdgesState(initialEdges);
    // const {fitView} = useReactFlow();


    // layout(nodes, edges);

    return <ReactFlow nodes={nodes}
                      onNodesChange={onNodesChange}
                      edges={edges}
                      onEdgesChange={onEdgesChange}
                      nodeTypes={nodeTypes}
                      fitView>
        <Background/>
        <Controls/>
    </ReactFlow>;

}

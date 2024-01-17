import ReactFlow, {
    Background,
    Controls,
    Edge, Node,
    NodeTypes,
    useEdgesState,
    useNodesState,
} from "reactflow";
import {Entity, EntityGraph} from "../model/entityModel.ts";
import {Flex, List, Typography} from "antd";

const {Text} = Typography;


function tooltip(field: { name: string, comment?: string }) {
    return field.comment ? `${field.name}: ${field.comment}` : field.name;
}


export function PropertiesNode({entity}: { entity: Entity }) {
    return <Flex vertical gap={'small'} className='formNode' style={{width: 300, backgroundColor: '#1677ff'}}>
        <Text strong style={{fontSize: 18, color: "#fff"}} ellipsis={{tooltip: true}}>
            {entity.label}
        </Text>
        <List size='small' style={{backgroundColor: '#ffffff'}} bordered dataSource={entity.fields!}
              renderItem={(item) => {
                  return <List.Item key={item.key} style={{position: 'relative'}}>
                      <Flex justify="space-between" style={{width: '100%'}}>
                          <Typography.Text style={{color: '#1677ff'}} ellipsis={{tooltip: tooltip(item)}}>
                              {item.name}
                          </Typography.Text>
                          <Typography.Text ellipsis={{tooltip: true}}>
                              {item.value}
                          </Typography.Text>
                      </Flex>

                      {/*<Handle type={'source'} position={Position.Right} id={item.prop}*/}
                      {/*        style={{position: 'absolute', left: '280px'}}/>*/}
                  </List.Item>;

              }}/>
    </Flex>;

}


const nodeTypes: NodeTypes = {
    props: (data) => <PropertiesNode entity={data.data as Entity}/>,
};

export function convertNodeAndEdges(graph: EntityGraph) {
    const nodes: Node<Entity, string>[] = []
    const edges: Edge[] = []

    let ei = 1;
    for (let entity of graph.entityMap.values()) {
        nodes.push({id: entity.id, data: entity, type: 'props', position: {x: 100, y: 100}})

        for (let output of entity.outputs) {
            for (let connectToSocket of output.connectToSockets) {
                edges.push({
                    id: '' + (ei++), source: entity.id, sourceHandle: output.output.key,
                    target: connectToSocket.entityId, targetHandle: connectToSocket.inputKey, type: 'smoothstep'
                });
            }
        }
    }
    return {initialNodes: nodes, initialEdges: edges};
}


export function FlowEntityGraph({entityGraph}: { entityGraph: EntityGraph }) {
    const {initialNodes, initialEdges} = convertNodeAndEdges(entityGraph);
    const [nodes, _setNodes, onNodesChange] = useNodesState<Entity>(initialNodes);
    const [edges, _setEdges, onEdgesChange] = useEdgesState(initialEdges);

    return <ReactFlow
                nodes={nodes}
                onNodesChange={onNodesChange}
                edges={edges}
                onEdgesChange={onEdgesChange}
                nodeTypes={nodeTypes}
                fitView
            >
                <Background/>
                <Controls/>
            </ReactFlow>    ;
}

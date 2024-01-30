import {
    ReactFlow,
    Controls,
    Background,
    useNodesState, useEdgesState, Edge, Node, NodeTypes, ReactFlowProvider, useNodes, Handle, Position
} from '@xyflow/react';
import 'reactflow/dist/style.css';
import {Button, Checkbox, Flex, Form, Input, List, Space, Typography} from "antd";

const {Text} = Typography;

const onFinish = (values: any) => {
    console.log('Success:', values);
};

const onFinishFailed = (errorInfo: any) => {
    console.log('Failed:', errorInfo);
};

type FieldType = {
    username?: string;
    password?: string;
    remember?: string;
};

const dataSource = [
    {prop: "prop1", value: "value1"},
    {prop: "prop2", value: "value22222222222"},
    {prop: "prop3", value: "value3"},
]

function PropertiesNode() {
    return <Flex vertical gap={'small'} className='formNode' style={{width: 300, backgroundColor: '#1677ff'}}>
        <Text strong style={{fontSize: 18, color: "#fff"}}
              copyable={{text: 'header'}}
              ellipsis={{tooltip: true}}>header</Text>
        <List size='small' style={{backgroundColor: '#ffffff'}} bordered dataSource={dataSource} renderItem={(item) => {
            return <List.Item style={{position: 'relative'}}>
                <Flex justify="space-between" style={{width: '100%'}}>
                    <Typography.Text style={{color: '#1677ff'}} ellipsis={{tooltip: true}}>
                        {item.prop}
                    </Typography.Text>
                    <Typography.Text ellipsis={{tooltip: true}}>
                        {item.value}
                    </Typography.Text>
                </Flex>

                <Handle type={'source'} position={Position.Right} id={item.prop}
                        style={{position: 'absolute', left: '280px'}}/>
            </List.Item>;

        }}/>
    </Flex>;
}


function FormNode() {
    return <div className='formNode' style={{width: 300, backgroundColor: '#13c2c2'}}>
        <Form name="basic"
              labelCol={{span: 8}}
              wrapperCol={{span: 16}}
              style={{maxWidth: 600}}
              initialValues={{remember: true}}
              onFinish={onFinish}
              onFinishFailed={onFinishFailed}
              autoComplete="off"
        >
            <Form.Item<FieldType>
                label="Username"
                name="username"
                rules={[{required: true, message: 'Please input your username!'}]}
            >
                <Input/>
            </Form.Item>


            <Form.Item<FieldType>
                label="Password"
                name="password"
                rules={[{required: true, message: 'Please input your password!'}]}
            >
                <Space>
                    <Input.Password/>

                    <Handle type={'source'} position={Position.Right} id={'password'}
                            style={{position: 'relative', left: '12px'}}/>
                </Space>
            </Form.Item>


            <Form.Item<FieldType>
                name="remember"
                valuePropName="checked"
                wrapperCol={{offset: 8, span: 16}}
            >
                <Checkbox>Remember me</Checkbox>
            </Form.Item>

            <Form.Item wrapperCol={{offset: 8, span: 16}}>
                <Button type="primary" htmlType="submit">
                    Submit
                </Button>
            </Form.Item>
        </Form>
        <Handle type={'source'} position={Position.Right} id={'node'}/>
    </div>;

}

const nodeTypes: NodeTypes = {
    formNode: FormNode,
    propNode: PropertiesNode,
};


const initialNodes: Node[] = [
    {
        id: '1',
        data: {},
        position: {x: 0, y: 0},
        type: 'formNode',
    },
    {
        id: '2',
        data: {label: 'World'},
        position: {x: 400, y: 100},
        type: "propNode",
    },
];

const initialEdges: Edge[] = [
    {id: '1-2', source: '1', sourceHandle: 'password', target: '2', label: 'to the'},
];

function FlowInner() {
    const [nodes, _setNodes, onNodesChange] = useNodesState(initialNodes);
    const [edges, _setEdges, onEdgesChange] = useEdgesState(initialEdges);

    return (
        <div style={{height: 'calc(100vh - 50px)', width: '100%'}}>
            <ReactFlow
                nodes={nodes}
                onNodesChange={onNodesChange}
                edges={edges}
                onEdgesChange={onEdgesChange}
                nodeTypes={nodeTypes}
                fitView
            >
                <Background/>
                <Controls/>
            </ReactFlow>
        </div>
    );
}


export function Test() {
    return (
        <ReactFlowProvider>
            <FlowInner/>
            <Sidebar/>
        </ReactFlowProvider>
    )
}

function Sidebar() {
    // This hook will only work if the component it's used in is a child of a
    // <ReactFlowProvider />.
    const nodes = useNodes()

    return (
        <aside>
            {nodes.map((node) => (
                <div key={node.id}>
                    Node {node.id} -
                    x: {node.position.x.toFixed(2)},
                    y: {node.position.y.toFixed(2)}
                </div>
            ))}
        </aside>
    )
}

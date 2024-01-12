import ReactFlow, {
    Controls,
    Background,
    useNodesState, useEdgesState, Edge, Node, NodeTypes, ReactFlowProvider, useNodes, Handle, Position
} from 'reactflow';
import 'reactflow/dist/style.css';
import {Button, Checkbox, Form, Input, Space} from "antd";

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

function FormNode() {
    return <div className='formNode'>
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
                <Space size={[20,10]} style={{marginRight: 0}}>
                    <Input.Password/>

                    <Handle type={'source'} position={Position.Right} id={'password'}/>
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
        position: {x: 100, y: 100},
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


export function Flow() {
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

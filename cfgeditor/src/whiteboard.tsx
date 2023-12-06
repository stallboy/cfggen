import {createRoot} from "react-dom/client";
import {NodeEditor, GetSchemes, ClassicPreset} from "rete";
import {AreaPlugin, AreaExtensions} from "rete-area-plugin";
import {
    ConnectionPlugin,
    Presets as ConnectionPresets
} from "rete-connection-plugin";
import {ReactPlugin, Presets, ReactArea2D} from "rete-react-plugin";
import {Space, Table} from "antd";
import "antd/dist/reset.css";


class Node extends ClassicPreset.Node<
    { [key in string]: ClassicPreset.Socket },
    { [key in string]: ClassicPreset.Socket },
    {
        [key in string]:
        | TableControl
        | ClassicPreset.Control
        | ClassicPreset.InputControl<"number">
        | ClassicPreset.InputControl<"text">;
    }
> {
    width = 280;
    height = 440;
}

class Connection<
    A extends Node,
    B extends Node
> extends ClassicPreset.Connection<A, B> {
}

type Schemes = GetSchemes<Node, Connection<Node, Node>>;
type AreaExtra = ReactArea2D<never>;

import type {ColumnsType} from 'antd/es/table';
import {AutoArrangePlugin, Presets as ArrangePresets} from "rete-auto-arrange-plugin";

export interface FieldType {
    name: string;
    value: string | number | boolean;
    key: string;
}

export interface ConnectToSocket {
    node: string;
    inputKey: string;
}

export interface SocketInfo {
    key : string;
    label? : string;
}

export interface OutputConnectInfo {
    output: SocketInfo;
    connectToSockets: ConnectToSocket[];
}

export interface NamedNodeType {
    name: string;
    fields: FieldType[];
    inputs: SocketInfo[];
    outputs: OutputConnectInfo[];
}


interface TableType {
    columns: ColumnsType<FieldType>;
    dataSource: FieldType[];
}

class TableControl extends ClassicPreset.Control {
    constructor(public table: TableType) {
        super();
    }
}

function MkTable(props: { data: TableControl }) {
    if (props.data.table.dataSource.length == 0) {
        return <Space/>
    }
    return <Table bordered
                  showHeader={false}
                  columns={props.data.table.columns}
                  dataSource={props.data.table.dataSource}
                  size={"small"}
                  pagination={false}/>;

}

export async function createEditor(container: HTMLElement, data: Map<string, NamedNodeType>) {
    const socket = new ClassicPreset.Socket("socket");

    const editor = new NodeEditor<Schemes>();
    const area = new AreaPlugin<Schemes, AreaExtra>(container);
    const connection = new ConnectionPlugin<Schemes, AreaExtra>();
    const render = new ReactPlugin<Schemes, AreaExtra>({createRoot});

    const arrange = new AutoArrangePlugin<Schemes>();
    arrange.addPreset(ArrangePresets.classic.setup());
    area.use(arrange);

    render.addPreset(
        Presets.classic.setup({
            customize: {
                control(data) {
                    if (data.payload instanceof TableControl) {
                        return MkTable;
                    }

                    if (data.payload instanceof ClassicPreset.InputControl) {
                        return Presets.classic.Control;
                    }
                    return null;
                }
            }
        })
    );
    connection.addPreset(ConnectionPresets.classic.setup());
    editor.use(area);
    area.use(connection);
    area.use(render);

    const columns: ColumnsType<FieldType> = [
        {
            title: 'name',
            dataIndex: 'name',
            align: 'right',
            width: 100,
            key: 'name',
            ellipsis: true,
        },
        {
            title: 'value',
            dataIndex: 'value',
            width: 100,
            key: 'value',
            ellipsis: true,
        },
    ];

    let name2node = new Map<string, Node>();
    for (let nodeData of data.values()) {
        const node = new Node(nodeData.name);
        name2node.set(nodeData.name, node);
        node.height = 40 * nodeData.fields.length + nodeData.inputs.length * 60 + nodeData.outputs.length * 60 + 60;

        const fields = new TableControl({columns: columns, dataSource: nodeData.fields});
        node.addControl("value", fields);

        for (let inputSocket of nodeData.inputs) {
            let input = new ClassicPreset.Input(socket, inputSocket.label);
            node.addInput(inputSocket.key, input);
        }
        for (let outputInfo of nodeData.outputs) {
            let output = new ClassicPreset.Output(socket, outputInfo.output.label);
            node.addOutput(outputInfo.output.key, output);
        }

        await editor.addNode(node);
    }

    for (let nodeData of data.values()) {
        let fromNode = name2node.get(nodeData.name) as Node;
        for (let output of nodeData.outputs) {
            for (let connSocket of output.connectToSockets) {
                let toNode = name2node.get(connSocket.node) as Node;
                let conn = new Connection(fromNode, output.output.key, toNode, connSocket.inputKey);
                await editor.addConnection(conn);
            }
        }
    }

    await arrange.layout();
    AreaExtensions.zoomAt(area, editor.getNodes());


    return {
        destroy: () => area.destroy()
    };
}

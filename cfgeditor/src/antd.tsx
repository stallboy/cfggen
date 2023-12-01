import {createRoot} from "react-dom/client";
import {NodeEditor, GetSchemes, ClassicPreset} from "rete";
import {AreaPlugin, AreaExtensions} from "rete-area-plugin";
import {
    ConnectionPlugin,
    Presets as ConnectionPresets
} from "rete-connection-plugin";
import {ReactPlugin, Presets, ReactArea2D} from "rete-react-plugin";
import {Table} from "antd";
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
    height = 400;
}

class Connection<
    A extends Node,
    B extends Node
> extends ClassicPreset.Connection<A, B> {
}

type Schemes = GetSchemes<Node, Connection<Node, Node>>;
type AreaExtra = ReactArea2D<never>;

import type {ColumnsType} from 'antd/es/table';

interface DataType {
    name: string;
    value: string | number | boolean;
}

interface TableType {
    columns: ColumnsType<DataType>;
    dataSource: DataType[];
}

class TableControl extends ClassicPreset.Control {
    constructor(public table: TableType) {
        super();
    }
}

function MkTable(props: { data: TableControl }) {
    return (
        <Table bordered showHeader={false} columns={props.data.table.columns} dataSource={props.data.table.dataSource} pagination={false}/>
    );
}

export async function createEditor(container: HTMLElement) {
    const socket = new ClassicPreset.Socket("socket");

    const editor = new NodeEditor<Schemes>();
    const area = new AreaPlugin<Schemes, AreaExtra>(container);
    const connection = new ConnectionPlugin<Schemes, AreaExtra>();
    const render = new ReactPlugin<Schemes, AreaExtra>({createRoot});

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

    const a = new Node("A");
    a.addOutput("a", new ClassicPreset.Output(socket));


    const columns: ColumnsType<DataType> = [
        {
            title: 'name',
            dataIndex: 'name',
            align: 'right',
            width: 100,


        },
        {
            title: 'value',
            dataIndex: 'value',
            width: 300,
            // render: (_, { tags }) => (
            //     <>
            //         {tags.map((tag) => {
            //             let color = tag.length > 5 ? 'geekblue' : 'green';
            //             if (tag === 'loser') {
            //                 color = 'volcano';
            //             }
            //             return (
            //                 <Tag color={color} key={tag}>
            //                     {tag.toUpperCase()}
            //                 </Tag>
            //             );
            //         })}
            //     </>
            // ),

        },
    ];

    const data: DataType[] = [
        {
            name: 'Name:',
            value: 'John Brown',
        },
        {
            name: 'Age:',
            value: 32,
        },
        {
            name: 'Address:',
            value: 'New York',
        },
        {
            name: 'Tags:',
            value: true,
        },
    ];


    a.addControl("value", new TableControl({columns: columns, dataSource: data}));

    await editor.addNode(a);

    AreaExtensions.zoomAt(area, editor.getNodes());

    return {
        destroy: () => area.destroy()
    };
}

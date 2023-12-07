import {ClassicPreset} from "rete";
import type {ColumnsType} from "antd/es/table";
import {EntityField} from "../graphModel.ts";
import {Space, Table} from "antd";

const columns: ColumnsType<EntityField> = [
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


export class TableControl extends ClassicPreset.Control {
    constructor(public data: EntityField[]) {
        super();
    }
}

export function TableControlComponent(props: { data: TableControl }) {
    if (props.data.data.length == 0) {
        return <Space/>
    }
    return <Table bordered
                  showHeader={false}
                  columns={columns}
                  dataSource={props.data.data}
                  size={"small"}
                  pagination={false}/>;

}

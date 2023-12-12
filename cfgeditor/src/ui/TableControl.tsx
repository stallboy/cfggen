import {ClassicPreset} from "rete";
import type {ColumnsType} from "antd/es/table";
import {EntityField, FieldsShowType} from "../model/graphModel.ts";
import {Collapse, Space, Table, Tooltip} from "antd";

const columns: ColumnsType<EntityField> = [
    {
        title: 'name',
        dataIndex: 'name',
        align: 'right',
        width: 100,
        key: 'name',
        ellipsis: {
            showTitle: false
        },
        render: (_text: any, record: EntityField, _index: number) => (
            <Tooltip placement="topLeft" title={record.comment ? record.name + ": " + record.comment : record.name}>
                {record.name}
            </Tooltip>
        )
    },
    {
        title: 'value',
        dataIndex: 'value',
        width: 100,
        key: 'value',
        ellipsis: {
            showTitle: false
        },
        render: (_text: any, record: EntityField, _index: number) => (
            <Tooltip placement="topLeft" title={record.value}>
                {record.value}
            </Tooltip>
        )
    },
];


function dummpyOnChange(_key: string | string[]) {
}

export class TableControl extends ClassicPreset.Control {
    onChange: (key: string | string[]) => void = dummpyOnChange;

    constructor(public data: EntityField[], public fieldsShow: FieldsShowType) {
        super();
    }
}

export function TableControlComponent(props: { data: TableControl }) {
    let ctrl = props.data;
    if (ctrl.data.length == 0) {
        return <Space/>
    }
    let tab = <Table bordered
                     showHeader={false}
                     columns={columns}
                     dataSource={props.data.data}
                     size={"small"}
                     pagination={false}/>;


    switch (ctrl.fieldsShow) {
        case FieldsShowType.Direct:
            return tab;
        case FieldsShowType.Expand:
            let items = [{key: '1', label: `${ctrl.data.length} fields`, children: tab}];
            return <Collapse defaultActiveKey={'1'} items={items} onChange={ctrl.onChange}/>
        case FieldsShowType.Fold:
            let items2 = [{key: '1', label: `${ctrl.data.length} fields`, children: tab}];
            return <Collapse items={items2} onChange={ctrl.onChange}/>
    }
}

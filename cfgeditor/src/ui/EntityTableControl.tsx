import {ClassicPreset} from "rete";
import {Entity, EntityField, FieldsShowType} from "../model/graphModel.ts";
import {Collapse, Empty, Form, Space, Table, Tooltip} from "antd";
import {ColumnsType} from "antd/es/table";


function getColumns(): ColumnsType<EntityField> {
    return [
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
            render: (_text: any, record: EntityField, _index: number) => {
                return <Tooltip placement="topLeft" title={record.value}>
                    {record.value}
                </Tooltip>;
            }
        },
    ];
}

function dummpyOnChange(_key: string | string[]) {
}

export class EntityTableControl extends ClassicPreset.Control {
    onChange: (key: string | string[]) => void = dummpyOnChange;

    constructor(public entity: Entity) {
        super();
    }
}

export function EntityTableControlComponent(props: { data: EntityTableControl }) {
    let entity: Entity = props.data.entity;
    if (entity.fieldsShow == FieldsShowType.Edit) {
        let fields = entity.editFields;
        if (!fields || fields.length == 0) {
            return <Space/>
        }

        return <Form labelCol={{span: 4}} wrapperCol={{span: 14}}
                     layout="horizontal" style={{maxWidth: 400}}>
            {fields.map((_field, _index) => {

                return <Empty/>
            })}
        </Form>
    }


    let fields = entity.fields;
    if (fields.length == 0) {
        return <Space/>
    }

    let tab = <Table bordered
                     showHeader={false}
                     columns={getColumns()}
                     dataSource={fields}
                     size={"small"}
                     pagination={false}/>;

    if (entity.fieldsShow == FieldsShowType.Direct) {
        return tab;
    }

    let items = [{key: '1', label: `${fields.length} fields`, children: tab}];


    switch (entity.fieldsShow) {
        case FieldsShowType.Expand:
            return <Collapse defaultActiveKey={'1'} items={items} onChange={props.data.onChange}/>
        case FieldsShowType.Fold:
            return <Collapse items={items} onChange={props.data.onChange}/>
    }
}

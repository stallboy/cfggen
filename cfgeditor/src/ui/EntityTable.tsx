import {ColumnsType} from "antd/es/table";
import {EntityField} from "../model/graphModel.ts";
import {Table, Tooltip} from "antd";

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

export function EntityTable({fields}: {
    fields: EntityField[];
}) {
    return <Table bordered
                  showHeader={false}
                  columns={getColumns()}
                  dataSource={fields}
                  size={"small"}
                  pagination={false}/>;
}

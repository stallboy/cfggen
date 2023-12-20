import {ColumnsType} from "antd/es/table";
import {EntityField} from "../model/entityModel.ts";
import {Table, Tooltip} from "antd";


function tooltip(field: { name: string, comment?: string }) {
    return field.comment ? `${field.name}: ${field.comment}` : field.name;
}

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
            render: (_text: any, field: EntityField, _index: number) => (
                <Tooltip placement="topLeft" title={tooltip(field)}>
                    {field.name}
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
            render: (_text: any, field: EntityField, _index: number) => {
                return <Tooltip placement="topLeft" title={field.value}>
                    {field.value}
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

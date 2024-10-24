import {memo, useMemo} from "react";
import {Schema} from "../table/schemaUtil.ts";
import {navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate} from "react-router-dom";
import {Button, Table} from "antd";
import TimeAgo from 'react-timeago'

class LastModifiedItem {
    constructor(public table: string,
                public id: string,
                public title: string,
                public lastModified: number) {
    }
}

export const LastModified = memo(function LastModified({schema}: {
    schema: Schema | undefined;
}) {
    if (!schema) return <></>;

    const {isEditMode} = store;
    const navigate = useNavigate();
    const {curPage} = useLocationData();

    const orderedItems: LastModifiedItem[] = useMemo(() => {
        if (schema == undefined) {
            return [];
        }
        const ordered: LastModifiedItem[] = [];
        for (let e of schema.lastModifiedMap.entries()) {
            const table: string = e[0]
            const idMap = e[1];
            const sTable = schema.getSTable(table);
            if (sTable == null) {
                continue;
            }
            for (let it of idMap.entries()) {
                const id: string = it[0];
                const lastModified: number = it[1]
                const title: string = sTable.idMap?.get(id)?.title || '';
                ordered.push(new LastModifiedItem(table, id, title, lastModified));
            }
        }
        ordered.sort((a: LastModifiedItem, b: LastModifiedItem) => b.lastModified - a.lastModified);
        return ordered;
    }, [schema]);


    const columns = useMemo(() => {
        return [
            {
                title: 'table',
                dataIndex: 'table',
                width: 60,
                key: 'table',
                ellipsis: true,
            },
            {
                title: 'id',
                // align: 'left',
                width: 160,
                key: 'id',
                ellipsis: {
                    showTitle: false
                },
                render: (_text: any, item: LastModifiedItem, _index: number) => {
                    const label = item.id + '-' + item.title;

                    return <Button type={'link'} onClick={() => {
                        navigate(navTo(curPage, item.table, item.id, isEditMode));
                    }}>
                        {label}
                    </Button>;
                }
            },
            {
                title: 'lastModified',
                // align: 'left',
                width: 80,
                key: 'lastModified',
                ellipsis: {
                    showTitle: false
                },
                render: (_text: any, item: LastModifiedItem, _index: number) => {
                    return <TimeAgo date={item.lastModified}/> ;
                }
            }
        ];
    }, [schema, navigate, curPage, isEditMode]);

    return <Table columns={columns}
                  dataSource={orderedItems}
                  pagination={false}
                  rowKey={rowKey}/>

});


function rowKey(item: LastModifiedItem) {
    return `${item.table}-${item.id}`
}
import {memo, useMemo} from "react";
import {Schema} from "../table/schemaUtil.tsx";
import {navTo, useMyStore, useCurPageRecordOrRecordRef} from "../../store/store.ts";
import {Button, Table} from "antd";
import {useNavigate} from "react-router-dom";

class LastAccessedItem {
    constructor(public table: string,
                public id: string,
                public title: string) {
    }
}

export const LastAccessed = memo(function LastAccessed({schema}: {
    schema: Schema | undefined;
}) {
    const {history, isEditMode} = useMyStore();
    const navigate = useNavigate();
    const {curPage} = useCurPageRecordOrRecordRef();

    const uniqItems: LastAccessedItem[] = useMemo(() => {
        const uniq: LastAccessedItem[] = [];
        for (const item of history.items.toReversed()) {
            if (!uniq.some((it: LastAccessedItem) => it.table === item.table && it.id === item.id)) {
                const title = schema?.getIdTitle(item.table, item.id) || '';
                uniq.push(new LastAccessedItem(item.table, item.id, title));
            }
        }

        return uniq;
    }, [history.items, schema]);

    const columns = useMemo(() => {
        return [
            {
                title: 'table',
                dataIndex: 'table',
                width: 70,
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
                render: (_text: unknown, item: LastAccessedItem) => {
                    const label = item.id + '-' + item.title;
                    return <Button type={'link'} onClick={() => {
                        navigate(navTo(curPage, item.table, item.id, isEditMode, false));
                    }}>
                        {label}
                    </Button>;
                }
            },
        ];
    }, [navigate, curPage, isEditMode]);

    return <Table columns={columns}
                  dataSource={uniqItems}
                  showHeader={false}
                  size={'small'}
                  pagination={false}
                  rowKey={rowKey}/>

});


function rowKey(item: LastAccessedItem) {
    return `${item.table}-${item.id}`
}


import {memo} from "react";
import {navTo, useMyStore, useLocationData, useCurPageRecordOrRecordRef} from "@/store/store";
import {Button, Result, Spin, Table} from "antd";
import {useNavigate} from "react-router";
import {useQuery} from "@tanstack/react-query";
import {fetchRecordRefIds} from "@/api/api";
import {RecordRefId, RecordRefIdsResult, RefId} from "@/api/recordModel";


function rowKey(item: RecordRefId) {
    return `${item.table}-${item.id}`
}

function RefIdListResult({refIdsResult}: {
    refIdsResult: RecordRefIdsResult
}) {

    const {isEditMode} = useMyStore();
    const navigate = useNavigate();
    const {curPage} = useCurPageRecordOrRecordRef();


    const columns = [
        {
            title: 'table',
            dataIndex: 'table',
            width: 70,
            key: 'table',
            ellipsis: true,
            render: (_text: unknown, item: RecordRefId) => {
                return item.depth + ' ' + item.table;
            }
        },
        {
            title: 'id',
            width: 160,
            key: 'id',
            ellipsis: {
                showTitle: false
            },
            render: (_text: unknown, item: RecordRefId) => {
                const label = item.title ? item.id + '-' + item.title : item.id;
                return <Button type={'link'} onClick={() => {
                    navigate(navTo(curPage, item.table, item.id, isEditMode, false));
                }}>
                    {label}
                </Button>;
            }
        },
    ];

    return <Table columns={columns}
                  dataSource={refIdsResult.recordRefIds}
                  showHeader={false}
                  size={'small'}
                  pagination={false}
                  virtual={refIdsResult.recordRefIds.length > 30}
                  scroll={{y: 300}}
                  rowKey={rowKey}/>

}


export const RefIdList = memo(function ({lockedId}: {
    lockedId: RefId | undefined
}) {
    const {server, refIdsInDepth, refIdsOutDepth, refIdsMaxNode} = useMyStore();
    const {curTableId, curId} = useLocationData();

    const thisTable = lockedId ? lockedId.table : curTableId;
    const thisId = lockedId ? lockedId.id : curId;

    const {isLoading, isError, error, data: recordResult} = useQuery({
        queryKey: ['recordRefIds', thisTable, thisId, refIdsInDepth, refIdsOutDepth, refIdsMaxNode],
        queryFn: ({signal}) =>
            fetchRecordRefIds(server, thisTable, thisId,
                refIdsInDepth, refIdsOutDepth, refIdsMaxNode,
                signal),
    })

    if (isLoading) {
        return <Spin/>;
    } else if (isError) {
        return <Result status={'error'} title={error.message}/>;
    } else if (!recordResult) {
        return <Result title={'record result empty'}/>;
    } else if (recordResult.resultCode != 'ok') {
        return <Result status={'error'} title={recordResult.resultCode}/>;
    }
    return <RefIdListResult refIdsResult={recordResult}/>;
});

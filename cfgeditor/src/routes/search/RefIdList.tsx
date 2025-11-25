import {memo, useMemo} from "react";
import {navTo, useMyStore, useLocationData} from "../../store/store.ts";
import {Button, Result, Spin, Table} from "antd";
import {useNavigate} from "react-router-dom";
import {useQuery} from "@tanstack/react-query";
import {fetchRecordRefIds} from "../api.ts";
import {RecordRefId, RecordRefIdsResult, RefId} from "../record/recordModel.ts";


function rowKey(item: RecordRefId) {
    return `${item.table}-${item.id}`
}

function RefIdListResult({refIdsResult}: {
    refIdsResult: RecordRefIdsResult
}) {

    const {isEditMode} = useMyStore();
    const navigate = useNavigate();
    const {curPage} = useLocationData();


    const columns = useMemo(() => {
        return [
            {
                title: 'table',
                dataIndex: 'table',
                width: 70,
                key: 'table',
                ellipsis: true,
                render: (_text: unknown, item: RecordRefId) =>{
                    return item.depth + ' ' + item.table;
                }
            },
            {
                title: 'id',
                // align: 'left',
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
    }, [navigate, curPage, isEditMode]);

    return <Table columns={columns}
                  dataSource={refIdsResult.recordRefIds}
                  showHeader={false}
                  size={'small'}
                  pagination={false}
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
    } else {
        return <RefIdListResult refIdsResult={recordResult}/>
    }
});

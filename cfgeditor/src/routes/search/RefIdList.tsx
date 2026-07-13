import {memo, useState} from "react";
import {navTo, useMyStore, useLocationData, useCurPageRecordOrRecordRef} from "@/store/store";
import {Button, Result, Spin, Table, Tooltip} from "antd";
import {LockOutlined, SyncOutlined, UnlockOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router";
import {useQuery} from "@tanstack/react-query";
import {useTranslation} from "react-i18next";
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


export const RefIdList = memo(function () {
    const {t} = useTranslation();
    const {server, refIdsInDepth, refIdsOutDepth, refIdsMaxNode} = useMyStore();
    const {curTableId, curId} = useLocationData();
    const [lockedId, setLockedId] = useState<RefId | undefined>(undefined);

    const thisTable = lockedId ? lockedId.table : curTableId;
    const thisId = lockedId ? lockedId.id : curId;

    const {isLoading, isError, error, data: recordResult} = useQuery({
        queryKey: ['recordRefIds', thisTable, thisId, refIdsInDepth, refIdsOutDepth, refIdsMaxNode],
        queryFn: ({signal}) =>
            fetchRecordRefIds(server, thisTable, thisId,
                refIdsInDepth, refIdsOutDepth, refIdsMaxNode,
                signal),
    })

    let content;
    if (isLoading) {
        content = <Spin/>;
    } else if (isError) {
        content = <Result status={'error'} title={error.message}/>;
    } else if (!recordResult) {
        content = <Result title={'record result empty'}/>;
    } else if (recordResult.resultCode != 'ok') {
        content = <Result status={'error'} title={recordResult.resultCode}/>;
    } else {
        content = <RefIdListResult refIdsResult={recordResult}/>
    }

    // lock 控件移到面板内部（自管 lockedId），不再挂在 Collapse 公共 extra 误导其它面板。
    // 三态：未锁→锁到当前；锁到当前→解锁；锁到别处→重置到当前。
    const isLockedToCurrent = lockedId?.table == curTableId && lockedId?.id == curId;
    const lockBtn = lockedId
        ? (isLockedToCurrent
            ? {icon: <LockOutlined/>, tip: t('unlock'), act: () => setLockedId(undefined)}
            : {icon: <SyncOutlined/>, tip: t('lock'), act: () => setLockedId({table: curTableId, id: curId})})
        : {icon: <UnlockOutlined/>, tip: t('lock'), act: () => setLockedId({table: curTableId, id: curId})};

    return <>
        <div style={{display: 'flex', justifyContent: 'flex-end'}}>
            <Tooltip title={lockBtn.tip}>
                <Button type="text" size="small" icon={lockBtn.icon} onClick={lockBtn.act}
                        aria-label={lockBtn.tip}/>
            </Tooltip>
        </div>
        {content}
    </>;
});

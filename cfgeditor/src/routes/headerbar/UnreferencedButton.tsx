import {Button, Tag, Skeleton} from "antd";
import {useQuery} from "@tanstack/react-query";
import {fetchUnreferencedRecords} from "../../api/api.ts";
import {useMyStore, useLocationData, navTo} from "../../store/store.ts";
import {STable} from "../../api/schemaModel.ts";
import {memo} from "react";
import {useTranslation} from "react-i18next";
import {useNavigate} from "react-router-dom";

export const UnreferencedButton = memo(function ({curTable}: {
    curTable: STable,
}) {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const {server, recordMaxNode} = useMyStore();
    const {curTableId} = useLocationData();

    // 获取未引用记录数量
    const {isLoading, data} = useQuery({
        queryKey: ['unreferenced', curTable.name, recordMaxNode],
        queryFn: ({signal}) => fetchUnreferencedRecords(
            server,
            curTable.name,
            recordMaxNode,
            signal
        ),
        staleTime: 1000 * 10,
        enabled: curTableId === curTable.name, // 只在当前table时查询
    });

    const count = data?.resultCode === 'ok' ? data.refs.length : 0;

    const handleClick = () => {
        // 使用navigate跳转到recordUnref路由
        navigate(navTo('recordUnref', curTable.name, '', false, false));
    };

    if (isLoading) {
        return <Skeleton.Button size="small" active/>;
    }

    if (count === 0) {
        return null; // 没有未引用记录时不显示按钮
    }

    return (
        <Button
            size="small"
            onClick={handleClick}
            title={t('unreferencedRecords')}
        >
            {t('unreferenced')} <Tag color="default">{count}</Tag>
        </Button>
    );
});

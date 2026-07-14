import {Badge, Button, Skeleton} from "antd";
import {useQuery} from "@tanstack/react-query";
import {fetchUnreferencedRecords} from "@/api/api";
import {useMyStore, useLocationData, navTo} from "@/store/store";
import {STable} from "@/api/schemaModel";
import {memo} from "react";
import {useTranslation} from "react-i18next";
import {useNavigate} from "react-router";

export const UnreferencedButton = memo(function ({curTable}: {
    curTable: STable,
}) {
    const {t} = useTranslation();
    const navigate = useNavigate();
    const {server, recordMaxNode} = useMyStore();
    const {curTableId, curId} = useLocationData();

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
        // 跳转到 recordUnref 路由；带上当前 curId，以便从 unref 切回 record 时保留上下文
        navigate(navTo('recordUnref', curTable.name, curId, false, false));
    };

    if (isLoading) {
        return <Skeleton.Button size="small" active/>;
    }

    if (count === 0) {
        return null; // 没有未引用记录时不显示按钮
    }

    return (
        <Badge count={count} size="small" offset={[-2, 0]}>
            <Button
                size="small"
                onClick={handleClick}
                title={t('unreferencedRecords')}
            >
                {t('unreferenced')}
            </Button>
        </Badge>
    );
});

import {memo, useState} from "react";
import {Schema} from "@/domain/schema";
import {useTranslation} from "react-i18next";
import {Button, CollapseProps, Tooltip} from "antd";
import {Collapse} from "antd/lib";
import {LockOutlined, SyncOutlined, UnlockOutlined} from "@ant-design/icons";
import {SearchValue} from "./SearchValue.tsx";
import {LastAccessed} from "./LastAccessed.tsx";
import {LastModified} from "./LastModified.tsx";
import {RefIdList} from "./RefIdList.tsx";
import {useLocationData} from "@/store/store";
import {RefId} from "@/api/recordModel";

export const Finder = memo(function Finder({schema}: {
    schema: Schema | undefined;
}) {

    const {t} = useTranslation();
    const {curTableId, curId} = useLocationData();
    const [lockedId, setLockedId] = useState<RefId | undefined>(undefined);

    // lock 控件挂在 refIdList 面板 header 右上角（Collapse extra），不占面板内部空间。
    // per-item extra——只挂在 refIdList 上，不波及其它面板。
    // 三态：未锁→锁到当前；锁到当前→解锁；锁到别处→重置到当前。
    const isLockedToCurrent = lockedId?.table == curTableId && lockedId?.id == curId;
    const lockBtn = lockedId
        ? (isLockedToCurrent
            ? {icon: <LockOutlined/>, tip: t('unlock'), act: () => setLockedId(undefined)}
            : {icon: <SyncOutlined/>, tip: t('lock'), act: () => setLockedId({table: curTableId, id: curId})})
        : {icon: <UnlockOutlined/>, tip: t('lock'), act: () => setLockedId({table: curTableId, id: curId})};

    const items: CollapseProps['items'] = [
        {
            key: 'refIdList',
            label: t('refIdList'),
            children: <RefIdList lockedId={lockedId}/>,
            extra: <Tooltip title={lockBtn.tip}>
                <Button type="text" size="small" icon={lockBtn.icon} aria-label={lockBtn.tip}
                        onClick={(e) => {
                            e.stopPropagation();
                            lockBtn.act();
                        }}/>
            </Tooltip>,
        },
        {
            key: 'lastAccessed',
            label: t('lastAccessed'),
            children: <LastAccessed schema={schema}/>,
        },
        {
            key: 'lastModified',
            label: t('lastModified'),
            children: <LastModified schema={schema}/>,
        },
        {
            key: 'search',
            label: t('search'),
            children: <SearchValue/>,
        },
    ];
    return <>
        <Collapse defaultActiveKey="search" items={items} size={"small"}/>
    </>
});

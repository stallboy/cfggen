import {memo, useState} from "react";
import {Schema} from "../table/schemaUtil.tsx";
import {useTranslation} from "react-i18next";
import {CollapseProps, Space} from "antd";
import {SearchValue} from "./SearchValue.tsx";
import {Collapse} from "antd/lib";
import {LastAccessed} from "./LastAccessed.tsx";
import {LastModified} from "./LastModified.tsx";
import {RefIdList} from "./RefIdList.tsx";
import {useLocationData} from "../../store/store.ts";
import {RefId} from "../record/recordModel.ts";
import {LockOutlined, SyncOutlined, UnlockOutlined} from "@ant-design/icons";

export const Finder = memo(function Finder({schema}: {
    schema: Schema | undefined;
}) {

    const {t} = useTranslation();
    const [lockedId, setLockedId] = useState<RefId | undefined>(undefined);
    const {curTableId, curId} = useLocationData();

    const genExtra = () => {
        if (lockedId) {
            return <Space>
                {lockedId.table == curTableId && lockedId.id == curId ? undefined :
                    <SyncOutlined onClick={(event) => {
                        event.stopPropagation();
                        setLockedId({table: curTableId, id: curId});
                    }}/>}
                <LockOutlined onClick={(event) => {
                    event.stopPropagation();
                    setLockedId(undefined);
                }}/>
            </Space>
        } else {
            return <UnlockOutlined onClick={(event) => {
                event.stopPropagation();
                setLockedId({table: curTableId, id: curId});
            }}/>
        }
    };

    const items: CollapseProps['items'] = [
        {
            key: 'refIdList',
            label: t('refIdList'),
            children: <RefIdList lockedId={lockedId}/>,
            extra: genExtra(),
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
        <div style={{height: 32}}/>
        <Collapse defaultActiveKey="lastAccessed" items={items} size={"small"}/>
    </>
});



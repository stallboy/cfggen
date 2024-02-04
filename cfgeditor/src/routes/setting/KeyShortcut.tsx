import {memo} from "react";
import {useTranslation} from "react-i18next";
import {Descriptions} from "antd";
import {LeftOutlined, RightOutlined, SearchOutlined} from "@ant-design/icons";
import {path} from '@tauri-apps/api';
import {useQuery} from "@tanstack/react-query";

async function queryFn() {
    return await path.resourceDir();
}


export const KeyShortCut = memo(function KeyShortcut() {
    const {t} = useTranslation();
    const {data} = useQuery({
        queryKey: ['path'],
        queryFn: queryFn,
    })

    return <>
        {data}
        <Descriptions title="Key Shortcut" bordered column={2} items={[
            {
                key: '1',
                label: <LeftOutlined/>,
                children: 'alt+x',
            },
            {
                key: '2',
                label: <RightOutlined/>,
                children: 'alt+c',
            },
            {
                key: '3',
                label: t('table'),
                children: 'alt+1',
            },
            {
                key: '4',
                label: t('tableRef'),
                children: 'alt+2',
            },
            {
                key: '5',
                label: t('record'),
                children: 'alt+3',
            },
            {
                key: '6',
                label: t('recordRef'),
                children: 'alt+4',
            },

            {
                key: '7',
                label: <SearchOutlined/>,
                children: 'alt+q',
            },
        ]}/>
    </>;


});
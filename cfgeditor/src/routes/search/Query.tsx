import {Tabs, TabsProps} from "antd";
import {useTranslation} from "react-i18next";
import {SearchValue} from "./SearchValue.tsx";
import {Chat} from "./Chat.tsx";
import {Schema} from "../table/schemaUtil.ts";
import {memo} from "react";
import {AddJson} from "./AddJson.tsx";
import {LastAccessed} from "./LastAccessed.tsx";
import {LastModified} from "./LastModified.tsx";


export const Query = memo(function Query({schema}: {
    schema: Schema | undefined;
}) {
    const {t} = useTranslation();

    const items: TabsProps['items'] = [
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
        {
            key: 'addJson',
            label: t('addJson'),
            children: <AddJson schema={schema}/>,
        },
        {
            key: 'chat',
            label: t('chat'),
            children: <Chat schema={schema}/>,
        },

    ];
    return <Tabs defaultActiveKey="search" items={items}/>;

});

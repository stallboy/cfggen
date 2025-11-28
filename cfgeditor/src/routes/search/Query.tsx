import {Tabs, TabsProps} from "antd";
import {useTranslation} from "react-i18next";
import {SearchValue} from "./SearchValue.tsx";
import {Chat} from "../add/Chat.tsx";
import {Schema} from "../table/schemaUtil.tsx";
import {memo} from "react";
import {AddJson} from "../add/AddJson.tsx";
import {LastAccessed} from "./LastAccessed.tsx";
import {LastModified} from "./LastModified.tsx";
import {useLocationData} from "../../store/store.ts";


export const Query = memo(function Query({schema}: {
    schema: Schema | undefined;
}) {
    const {t} = useTranslation();
    const {curTableId} = useLocationData();

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
            key: `chat-${curTableId}`,
            label: t('chat') + "-" + curTableId,
            children: <Chat schema={schema}/>,
        },
        {
            key: `addJson-${curTableId}`,
            label: t('addJson') + "-" + curTableId,
            children: <AddJson schema={schema}/>,
        },
    ];
    return <Tabs defaultActiveKey="lastAccessed" items={items}/>;

});

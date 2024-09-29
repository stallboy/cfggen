import {Tabs, TabsProps} from "antd";
import {useTranslation} from "react-i18next";
import {SearchValue} from "./SearchValue.tsx";
import {Chat} from "./Chat.tsx";
import {Schema} from "../table/schemaUtil.ts";


export function Query({schema}: {
    schema: Schema | undefined;
}) {
    const {t} = useTranslation();

    const items: TabsProps['items'] = [
        {
            key: 'search',
            label: t('search'),
            children: <SearchValue/>,
        },
        {
            key: 'chat',
            label: t('chat'),
            children: <Chat schema={schema}/>,
        },

    ];
    return <Tabs defaultActiveKey="search" items={items}/>;

}
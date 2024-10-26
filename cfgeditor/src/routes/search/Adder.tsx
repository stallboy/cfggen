import {memo} from "react";
import {Schema} from "../table/schemaUtil.ts";
import {useTranslation} from "react-i18next";
import {Tabs, TabsProps} from "antd";
import {AddJson} from "./AddJson.tsx";
import {Chat} from "./Chat.tsx";

export const Adder = memo(function Adder({schema}: {
    schema: Schema | undefined;
}) {

    const {t} = useTranslation();

    const items: TabsProps['items'] = [
        {
            key: 'chat',
            label: t('chat'),
            children: <Chat schema={schema}/>,
        },
        {
            key: 'addJson',
            label: t('addJson'),
            children: <AddJson schema={schema}/>,
        },
    ];
    return <>
        <div style={{height: 32}}/>
        <Tabs defaultActiveKey="chat" items={items}/>
    </>
});



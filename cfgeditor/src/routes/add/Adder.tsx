import {memo} from "react";
import {Schema} from "../table/schemaUtil.tsx";
import {useTranslation} from "react-i18next";
import {Tabs, TabsProps} from "antd";
import {AddJson} from "./AddJson.tsx";
import {Chat} from "./Chat.tsx";
import {useLocationData} from "../../store/store.ts";

export const Adder = memo(function Adder({schema}: {
    schema: Schema | undefined;
}) {

    const {t} = useTranslation();
    const {curTableId} = useLocationData();

    const items: TabsProps['items'] = [
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
    return <>
        <div style={{height: 32}}/>
        <Tabs defaultActiveKey="chat" items={items}/>
    </>
});



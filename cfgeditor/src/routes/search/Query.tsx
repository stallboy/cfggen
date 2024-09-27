import {Tabs, TabsProps} from "antd";
import {useTranslation} from "react-i18next";
import {SearchValue} from "./SearchValue.tsx";
import {Chat} from "./Chat.tsx";


export function Query() {
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
            children: <Chat/>,
        },

    ];
    return <Tabs defaultActiveKey="search" items={items}/>;

}
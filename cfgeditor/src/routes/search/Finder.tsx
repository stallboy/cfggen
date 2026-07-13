import {memo} from "react";
import {Schema} from "@/domain/schema";
import {useTranslation} from "react-i18next";
import {CollapseProps} from "antd";
import {Collapse} from "antd/lib";
import {SearchValue} from "./SearchValue.tsx";
import {LastAccessed} from "./LastAccessed.tsx";
import {LastModified} from "./LastModified.tsx";
import {RefIdList} from "./RefIdList.tsx";

export const Finder = memo(function Finder({schema}: {
    schema: Schema | undefined;
}) {

    const {t} = useTranslation();

    const items: CollapseProps['items'] = [
        {
            key: 'refIdList',
            label: t('refIdList'),
            children: <RefIdList/>,
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

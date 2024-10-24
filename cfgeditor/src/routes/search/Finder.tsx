import {memo} from "react";
import {Schema} from "../table/schemaUtil.ts";
import {useTranslation} from "react-i18next";
import {CollapseProps} from "antd";
import {SearchValue} from "./SearchValue.tsx";
import {Collapse} from "antd/lib";
import {LastAccessed} from "./LastAccessed.tsx";
import {LastModified} from "./LastModified.tsx";

export const Finder = memo(function Finder({schema}: {
    schema: Schema | undefined;
}) {

    const {t} = useTranslation();

    const items: CollapseProps['items'] = [
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
        <Collapse defaultActiveKey="history" items={items}/>
    </>
});



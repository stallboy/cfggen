import {Divider, Tabs} from "antd";

import {NodeShowSetting} from "./NodeShowSetting.tsx";
import {useTranslation} from "react-i18next";
import {STable} from "../table/schemaModel.ts";
import {Schema} from "../table/schemaUtil.tsx";

import {BasicSetting} from "./BasicSetting.tsx";
import {Operations} from "./Operations.tsx";
import {memo, RefObject} from "react";
import {AiAndResource} from "./AiAndResource.tsx";
import {ThemeSetting} from "./ThemeSetting.tsx";


export const Setting = memo(function Setting({schema, curTable, flowRef}: {
    schema: Schema | undefined;
    curTable: STable | null;
    flowRef: RefObject<HTMLDivElement | null>;
}) {

    const {t} = useTranslation();

    const items = [
        {
            key: 'basicSetting',
            label: t('basicSetting'),
            children: <BasicSetting/>,
        },
        {
            key: 'recordShowSetting',
            label: t('recordShowSetting'),
            children: <NodeShowSetting/>,
        },
        {
            key: 'themeSetting',
            label: t('themeSetting'),
            children: <ThemeSetting/>,
        },
        {
            key: 'operations',
            label: t('operations'),
            children: <Operations schema={schema} curTable={curTable} flowRef={flowRef}/>,
        },
        {
            key: 'AiAndResource',
            label: t('AiAndResource'),
            children: <AiAndResource schema={schema}/>,
        },
    ]

    return <>
        <div style={{height: 16}}/>
        <Divider/>
        <Tabs items={items} tabPlacement='start'/>
    </>
});

import {Tabs} from "antd";

import {NodeShowSetting} from "./NodeShowSetting.tsx";
import {useTranslation} from "react-i18next";
import {STable} from "../table/schemaModel.ts";
import {Schema} from "../table/schemaUtil.tsx";

import {KeyShortCut} from "./KeyShortcut.tsx";
import {BasicSetting} from "./BasicSetting.tsx";
import {Operations} from "./Operations.tsx";
import {memo, RefObject} from "react";
import {TauriSetting} from "./TauriSeting.tsx";
import {ServerAndAi} from "./ServerAndAi.tsx";
import {isTauri} from "@tauri-apps/api/core";
import {ThemeSetting} from "./ThemeSetting.tsx";


export const Setting = memo(function Setting({schema, curTable, flowRef}: {
    schema: Schema | undefined;
    curTable: STable | null;
    flowRef: RefObject<HTMLDivElement>;
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
            key: 'serverAndAi',
            label: t('serverAndAi'),
            children: <ServerAndAi schema={schema}/>,
        },


        {
            key: 'operations',
            label: t('operations'),
            children: <Operations schema={schema} curTable={curTable} flowRef={flowRef}/>,
        },
        {
            key: 'keySetting',
            label: t('keySetting'),
            children: <KeyShortCut/>
        },
    ]
    if (isTauri()) {
        items.push({
            key: 'appSetting',
            label: t('appSetting'),
            children: <TauriSetting schema={schema}/>
        });
    }
    return <Tabs items={items} tabPosition='left'/>;
});

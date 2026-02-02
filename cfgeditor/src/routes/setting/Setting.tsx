import {Divider, Tabs} from "antd";

import {NodeShowSetting} from "./NodeShowSetting.tsx";
import {useTranslation} from "react-i18next";
import {STable} from "../../api/schemaModel.ts";
import {Schema} from "../table/schemaUtil.tsx";

import {BasicSetting} from "./BasicSetting.tsx";
import {Operations} from "./Operations.tsx";
import {memo, RefObject} from "react";
import {AiSetting} from "./AiSetting.tsx";
import {ThemeSetting} from "./ThemeSetting.tsx";
import {isTauri} from "@tauri-apps/api/core";
import {TauriSetting} from "./TauriSeting.tsx";
import {AddJson} from "../add/AddJson.tsx";


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
            key: 'addJson',
            label: t('addJson'),
            children: <AddJson schema={schema} key={"addJson-" + (curTable?.name || "$")}/>,
        },

        {
            key: 'ai',
            label: t('aiSetting'),
            children: <AiSetting/>,
        },
    ]


    if (isTauri()) {
        items.push({
            key: 'resource',
            label: t('resourceSetting'),
            children: <TauriSetting schema={schema}/>,
        })
    }

    return <div style={{paddingRight: 24}}>
        <div style={{height: 16}}/>
        <Divider/>
        <Tabs items={items} tabPlacement='start'/>
    </div>
});

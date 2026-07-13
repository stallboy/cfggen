import {Divider, Tabs} from "antd";

import {useTranslation} from "react-i18next";
import {STable} from "@/api/schemaModel";
import {Schema} from "@/domain/schema";

import {BasicSetting} from "./BasicSetting.tsx";
import {DisplaySetting} from "./DisplaySetting.tsx";
import {ConnectionSetting} from "./ConnectionSetting.tsx";
import {TauriSetting} from "./TauriSetting.tsx";
import {ThemeSetting} from "./ThemeSetting.tsx";
import {FixPages} from "./FixPages.tsx";
import {ToolsSetting} from "./ToolsSetting.tsx";
import {memo, RefObject} from "react";
import {isTauri} from "@tauri-apps/api/core";
import {AddJson} from "@/routes/add/AddJson";


export const Setting = memo(function Setting({schema, curTable, flowRef}: {
    schema: Schema | undefined;
    curTable: STable | null;
    flowRef: RefObject<HTMLDivElement | null>;
}) {

    const {t} = useTranslation();

    const items = [
        {
            key: 'display',
            label: t('recordShowSetting'),
            children: <DisplaySetting/>,
        },
        {
            key: 'behavior',
            label: t('basicSetting'),
            children: <BasicSetting/>,
        },
        {
            key: 'connection',
            label: t('connection'),
            children: <ConnectionSetting/>,
        },
        {
            key: 'theme',
            label: t('themeSetting'),
            children: <ThemeSetting/>,
        },
        {
            key: 'fixedPages',
            label: t('pages'),
            children: <FixPages schema={schema} curTable={curTable}/>,
        },
        {
            key: 'tools',
            label: t('tools'),
            children: <ToolsSetting schema={schema} curTable={curTable} flowRef={flowRef}/>,
        },
        {
            key: 'addJson',
            label: t('addJson'),
            children: <AddJson schema={schema} key={"addJson-" + (curTable?.name || "$")}/>,
        },
    ];

    if (isTauri()) {
        // 数据源 tab 仅桌面端，插在「连接」之后
        items.splice(3, 0, {
            key: 'resource',
            label: t('resourceSetting'),
            children: <TauriSetting schema={schema}/>,
        });
    }

    return <div style={{paddingRight: 24}}>
        <Divider/>
        <Tabs items={items} tabPlacement='start'/>
    </div>
});

import {Tabs} from "antd";

import {RecordRefSetting} from "./RecordRefSetting.tsx";
import {useTranslation} from "react-i18next";
import {STable} from "../table/schemaModel.ts";
import {Schema} from "../table/schemaUtil.ts";

import {KeyShortCut} from "./KeyShortcut.tsx";
import {TableSetting} from "./TableSetting.tsx";
import {Operations} from "./Operations.tsx";
import {memo, RefObject} from "react";
import {TauriSetting} from "./TauriSeting.tsx";
import {FixedPagesSetting} from "./FixedPagesSeting.tsx";


export const Setting = memo(function Setting({schema, curTable, flowRef}: {
    schema: Schema | undefined;
    curTable: STable | null;
    flowRef: RefObject<HTMLDivElement>;
}) {

    const {t} = useTranslation();

    const items = [
        {
            key: 'recordSetting',
            label: t('recordSetting'),
            children: <RecordRefSetting/>,
        },
        {
            key: 'fixedPagesSetting',
            label: t('fixedPagesSetting'),
            children: <FixedPagesSetting schema={schema} curTable={curTable}/>,
        },

        {
            key: 'tableSetting',
            label: t('tableSetting'),
            children: <TableSetting/>,
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
    if (window.__TAURI__) {
        items.push({
            key: 'appSetting',
            label: t('appSetting'),
            children: <TauriSetting schema={schema}/>
        });
    }
    return <Tabs items={items} tabPosition='left'/>;
});

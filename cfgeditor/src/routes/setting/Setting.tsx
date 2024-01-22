import {Tabs} from "antd";

import {RecordRefSetting} from "./RecordRefSetting.tsx";
import {useTranslation} from "react-i18next";
import {STable} from "../table/schemaModel.ts";
import {Schema} from "../table/schemaUtil.ts";

import {KeyShortCut} from "./KeyShortcut.tsx";
import {TableSetting} from "./TableSetting.tsx";
import {Operations} from "./Operations.tsx";


export function Setting({schema, curTable, onToPng}: {
    schema: Schema | undefined;
    curTable: STable | null;
    onToPng: () => void;
}) {

    const {t} = useTranslation();
    return <Tabs items={[
        {key: 'recordSetting', label: t('recordSetting'), children: <RecordRefSetting/>,},
        {key: 'tableSetting', label: t('tableSetting'), children: <TableSetting/>,},
        {
            key: 'otherSetting',
            label: t('otherSetting'),
            children: <Operations schema={schema} curTable={curTable} onToPng={onToPng}/>,
        },
        {key: 'keySetting', label: t('keySetting'), children: <KeyShortCut/>,},
    ]} tabPosition='left'/>;
}

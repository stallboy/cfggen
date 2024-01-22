import {Button, Radio, RadioChangeEvent, Select, Skeleton, Space, Typography} from "antd";
import {LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {historyNext, historyPrev, navTo, store, useLocationData} from "../setting/store.ts";
import {getNextId, Schema} from "../table/schemaUtil.ts";
import {useHotkeys} from "react-hotkeys-hook";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {getId} from "../record/recordRefEntity.ts";
import {useTranslation} from "react-i18next";
import {memo} from "react";

const {Text} = Typography;

export const HeaderBar = memo(function ({schema, curTable, setSettingOpen, setSearchOpen}: {
    schema: Schema | undefined;
    curTable: STable | null;
    setSettingOpen: (open: boolean) => void;
    setSearchOpen: (open: boolean) => void;
}) {
    const {curPage, curTableId, curId} = useLocationData();
    const {dragPanel, fix, history, isEditMode} = store;
    const navigate = useNavigate();
    const {t} = useTranslation();
    useHotkeys('alt+1', () => navigate(navTo('table', curTableId, curId)));
    useHotkeys('alt+2', () => navigate(navTo('tableRef', curTableId, curId)));
    useHotkeys('alt+3', () => navigate(navTo('record', curTableId, curId, isEditMode)));
    useHotkeys('alt+4', () => navigate(navTo('recordRef', curTableId, curId)));
    useHotkeys('alt+c', () => prev());
    useHotkeys('alt+v', () => next());



    function prev() {
        const path = historyPrev(curPage);
        if (path) {
            navigate(path);
        }
    }

    function next() {
        const path = historyNext(curPage);
        if (path) {
            navigate(path);
        }
    }

    let nextId;
    if (curTable) {
        let nId = getNextId(curTable, curId);
        if (nId) {
            nextId = <Text>{t('nextSlot')} <Text copyable>{nId}</Text> </Text>
        }
    }

    function onChangeCurPage(e: RadioChangeEvent) {
        const page = e.target.value;
        navigate(navTo(page, curTableId, curId, isEditMode));
    }

    let options = [
        {label: t('table'), value: 'table'},
        {label: t('tableRef'), value: 'tableRef'},
        {label: t('record'), value: 'record'},
    ]

    if (dragPanel != 'recordRef') {
        options.push({label: t('recordRef'), value: 'recordRef'});
    }

    if (fix && schema) {
        let fixedTable = schema.getSTable(fix.table);
        if (fixedTable && dragPanel != 'fix') {
            options.push({label: t('fix') + ' ' + getId(fix.table, fix.id), value: 'fix'});
        }
    }

    return <div style={{position: 'relative'}}>
        <Space size={'large'} style={{position: 'absolute', zIndex: 1}}>
            <Space>
                <Button onClick={() => setSettingOpen(true)}>
                    <SettingOutlined/>
                </Button>
                <Button onClick={() => setSearchOpen(true)}>
                    <SearchOutlined/>
                </Button>
                {schema ? <TableList schema={schema}/> : <Select id='table' loading={true}/>}
                {curTable ? <IdList curTable={curTable}/> : <Skeleton.Input/>}
                {nextId}
            </Space>
            <Space>
                <Radio.Group value={curPage} onChange={onChangeCurPage}
                             options={options} optionType={'button'}>
                </Radio.Group>

                <Button onClick={prev} disabled={!history.canPrev()}>
                    <LeftOutlined/>
                </Button>

                <Button onClick={next} disabled={!history.canNext()}>
                    <RightOutlined/>
                </Button>
            </Space>
        </Space></div>;
});

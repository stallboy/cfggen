import {Radio, RadioChangeEvent, Select, Skeleton, Space, Typography} from "antd";
import {LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {getFixedPage, historyNext, historyPrev, navTo, setDragPanel, store, useLocationData} from "../setting/store.ts";
import {getNextId, Schema} from "../table/schemaUtil.ts";
import {useHotkeys} from "react-hotkeys-hook";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {useTranslation} from "react-i18next";
import {memo, useState} from "react";
import {ActionIcon} from "@ant-design/pro-editor";
import {toggleFullScreen} from "../setting/TauriSeting.tsx";
import {VscLayoutSidebarLeft, VscLayoutSidebarLeftOff} from "react-icons/vsc";

const {Text} = Typography;

export const HeaderBar = memo(function HeaderBar({schema, curTable, setSettingOpen, setSearchOpen}: {
    schema: Schema | undefined;
    curTable: STable | null;
    setSettingOpen: (open: boolean) => void;
    setSearchOpen: (open: boolean) => void;
}) {
    const {curPage, curTableId, curId} = useLocationData();
    const {dragPanel, pageConf, history, isEditMode} = store;
    const [fix, setFix] = useState<string>(dragPanel);
    const navigate = useNavigate();
    const {t} = useTranslation();
    useHotkeys('alt+1', () => navigate(navTo('table', curTableId, curId)));
    useHotkeys('alt+2', () => navigate(navTo('tableRef', curTableId, curId)));
    useHotkeys('alt+3', () => navigate(navTo('record', curTableId, curId, isEditMode)));
    useHotkeys('alt+4', () => navigate(navTo('recordRef', curTableId, curId)));
    useHotkeys('alt+c', () => prev());
    useHotkeys('alt+v', () => next());
    useHotkeys('alt+enter', toggleFullScreen);

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
        {label: t('recordRef'), value: 'recordRef'}
    ]

    function onDragPanelSwitch() {
        if (dragPanel == 'none') {
            setDragPanel(fix);
        } else {
            setDragPanel('none');
        }
    }

    function onDragePanelSelect(value: string) {
        setFix(value);
        if (dragPanel == 'none') {
            if (value == 'recordRef') {
                navigate(navTo('recordRef', curTableId, curId, isEditMode));
            } else {
                const page = getFixedPage(pageConf, value);
                if (page) {
                    navigate(navTo('recordRef', page.table, page.id, isEditMode))
                }
            }
        } else {
            setDragPanel(value);
        }
    }

    let fixedOptions = [
        ...(pageConf.pages.map(fp => {
            return {label: fp.label, value: fp.label};
        })),
        {label: t('recordRef'), value: 'recordRef'},
    ]

    return <div style={{position: 'relative'}}>
        <Space size={'large'} style={{position: 'absolute', zIndex: 1}}>
            <Space size={'small'}>
                <ActionIcon icon={<SettingOutlined/>} onClick={() => setSettingOpen(true)}/>
                <ActionIcon icon={<SearchOutlined/>} onClick={() => setSearchOpen(true)}/>
                <ActionIcon icon={dragPanel == 'none' ? <VscLayoutSidebarLeftOff/> : <VscLayoutSidebarLeft/>}
                            onClick={onDragPanelSwitch}/>

                <Select options={fixedOptions}
                        style={{width: 100}}
                        value={fix}
                        onChange={onDragePanelSelect}/>

                {schema ? <TableList schema={schema}/> : <Select id='table' loading={true}/>}
                {curTable ? <IdList curTable={curTable}/> : <Skeleton.Input/>}
                {nextId}
            </Space>
            <Space size={'small'}>
                <Radio.Group value={curPage} onChange={onChangeCurPage}
                             options={options} optionType={'button'}>
                </Radio.Group>


                <ActionIcon icon={<LeftOutlined/>} onClick={prev} disabled={!history.canPrev()}/>
                <ActionIcon icon={<RightOutlined/>} onClick={next} disabled={!history.canNext()}/>
            </Space>
        </Space></div>;
});

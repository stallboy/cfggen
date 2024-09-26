import {Button, Radio, RadioChangeEvent, Select, Skeleton, Space, Typography} from "antd";
import {LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {getFixedPage, historyNext, historyPrev, navTo, setDragPanel, store, useLocationData} from "../setting/store.ts";
import {getNextId, Schema} from "../table/schemaUtil.ts";
import {useHotkeys} from "react-hotkeys-hook";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {useTranslation} from "react-i18next";
import {memo, useCallback, useMemo, useState} from "react";
import {toggleFullScreen} from "../setting/TauriSeting.tsx";
import {VscLayoutSidebarLeft, VscLayoutSidebarLeftOff} from "react-icons/vsc";

const {Text} = Typography;
const settingIcon = <SettingOutlined/>;
const searchIcon = <SearchOutlined/>;
const fixOffIcon = <VscLayoutSidebarLeftOff/>;
const fixOnIcon = <VscLayoutSidebarLeft/>;
const prevIcon = <LeftOutlined/>;
const nextIcon = <RightOutlined/>;


export const HeaderBar = memo(function HeaderBar({schema, curTable, setSettingOpen, setSearchOpen}: {
    schema: Schema | undefined;
    curTable: STable | null;
    setSettingOpen: (open: boolean) => void;
    setSearchOpen: (open: boolean) => void;
}) {
    const {curPage, curTableId, curId} = useLocationData();
    const {dragPanel, pageConf, history, isNextIdShow, isEditMode} = store;
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

    const prev = useCallback(() => {
        const path = historyPrev(curPage, history, isEditMode);
        if (path) {
            navigate(path);
        }
    }, [curPage, history, isEditMode, navigate]);

    const next = useCallback(() => {
        const path = historyNext(curPage, history, isEditMode);
        if (path) {
            navigate(path);
        }
    }, [curPage, history, isEditMode, navigate]);

    let nextId;
    if (isNextIdShow && curTable) {
        const nId = getNextId(curTable, curId);
        if (nId) {
            nextId = <Text>{t('nextSlot')} <Text copyable>{nId}</Text> </Text>
        }
    }

    const onSettingClick = useCallback(() => setSettingOpen(true), [setSettingOpen]);
    const onSearchClick = useCallback(() => setSearchOpen(true), [setSearchOpen]);
    const onDragPanelSwitch = useCallback(() => {
        if (dragPanel == 'none') {
            let go = fix
            if (fix == 'none') {
                if (pageConf.pages.length > 0){
                    go = pageConf.pages[0].label
                }else{
                    go = 'recordRef'
                }
                setFix(go)
            }
            setDragPanel(fix);
        } else {
            setDragPanel('none');
        }
    }, [dragPanel, fix]);
    const onDragPanelSelect = useCallback((value: string) => {
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
    }, [dragPanel, navigate, curTableId, curId, isEditMode, pageConf]);

    const fixedOptions = useMemo(() => [
        ...(pageConf.pages.map(fp => {
            return {label: fp.label, value: fp.label};
        })),
        {label: t('recordRef'), value: 'recordRef'},
    ], [pageConf.pages, t]);

    const options = useMemo(() => [
        {label: t('table'), value: 'table'},
        {label: t('tableRef'), value: 'tableRef'},
        {label: t('record'), value: 'record'},
        {label: t('recordRef'), value: 'recordRef'}
    ], [t]);


    const onChangeCurPage = useCallback((e: RadioChangeEvent) => {
        const page = e.target.value;
        navigate(navTo(page, curTableId, curId, isEditMode));
    }, [curTableId, curId, isEditMode, navigate]);

    return <div style={{position: 'relative'}}>
        <Space size={'large'} style={{position: 'absolute', zIndex: 1}}>
            <Space size={'small'}>
                <Button icon={settingIcon} onClick={onSettingClick}/>
                <Button icon={searchIcon} onClick={onSearchClick}/>
                <Button icon={dragPanel == 'none' ? fixOffIcon : fixOnIcon}
                        onClick={onDragPanelSwitch}/>
                {pageConf.pages.length > 0 &&
                    <Select options={fixedOptions}
                            style={{width: 100}}
                            value={fix}
                            onChange={onDragPanelSelect}/>
                }

                {schema ? <TableList schema={schema}/> : <Select id='table' loading={true}/>}
                {curTable ? <IdList curTable={curTable}/> : <Skeleton.Input/>}
                {nextId}
            </Space>
            <Space size={'small'}>
                <Radio.Group value={curPage} onChange={onChangeCurPage}
                             options={options} optionType={'button'}>
                </Radio.Group>

                <Button icon={prevIcon} onClick={prev} disabled={!history.canPrev()}/>
                <Button icon={nextIcon} onClick={next} disabled={!history.canNext()}/>
            </Space>
        </Space></div>;
});

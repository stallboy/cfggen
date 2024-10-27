import {Button, Radio, RadioChangeEvent, Select, Skeleton, Space, Typography} from "antd";
import {LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {historyNext, historyPrev, navTo, PageType, setDragPanel, store, useLocationData} from "../setting/store.ts";
import {getNextId, Schema} from "../table/schemaUtil.ts";
import {useHotkeys} from "react-hotkeys-hook";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {useTranslation} from "react-i18next";
import {memo, useCallback, useMemo} from "react";
import {toggleFullScreen} from "../setting/TauriSeting.tsx";

const {Text} = Typography;
const settingIcon = <SettingOutlined/>;
const searchIcon = <SearchOutlined/>;
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

    const dragOptions = useMemo(() => [
        ...(pageConf.pages.map(fp => {
            return {label: fp.label, value: fp.label};
        })),
        {label: t('recordRef'), value: 'recordRef'},
        {label: t('none'), value: 'none'},
        {label: t('finder'), value: 'finder'},
        {label: t('adder'), value: 'adder'},
    ], [pageConf.pages, t]);

    const options = useMemo(() => [
        {label: t('table'), value: 'table'},
        {label: t('tableRef'), value: 'tableRef'},
        {label: t('record'), value: 'record'},
        {label: t('recordRef'), value: 'recordRef'}
    ], [t]);


    const onChangeCurPage = useCallback((page: PageType) => {
        navigate(navTo(page, curTableId, curId, isEditMode));
    }, [curTableId, curId, isEditMode, navigate]);

    return <div style={{position: 'relative'}}>
        <Space size={'large'} style={{position: 'absolute', zIndex: 1}}>
            <Space size={'small'}>
                <Button icon={settingIcon} onClick={onSettingClick}/>
                <Button icon={searchIcon} onClick={onSearchClick}/>
                <Select options={dragOptions}
                        style={{width: 80}}
                        value={dragPanel}
                        onChange={setDragPanel}/>

                {schema ? <TableList schema={schema}/> : <Select id='table' loading={true}/>}
                {curTable ? <IdList curTable={curTable}/> : <Skeleton.Input/>}
                {nextId}
            </Space>
            <Space size={'small'}>
                <Select value={curPage} onChange={onChangeCurPage} options={options}/>
                <Button icon={prevIcon} onClick={prev} disabled={!history.canPrev()}/>
                <Button icon={nextIcon} onClick={next} disabled={!history.canNext()}/>
            </Space>
        </Space></div>;
});

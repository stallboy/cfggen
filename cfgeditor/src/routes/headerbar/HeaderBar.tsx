import {Button, Select, Skeleton, Space, Typography} from "antd";
import {LeftOutlined, RightOutlined} from "@ant-design/icons";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {
    historyCanPrev,
    historyNext,
    historyPrev,
    navTo,
    setDragPanel,
    useMyStore,
    useLocationData
} from "../../store/store.ts";
import {getNextId, Schema} from "../table/schemaUtil.tsx";
import {useHotkeys} from "react-hotkeys-hook";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {useTranslation} from "react-i18next";
import {memo, useCallback, useMemo} from "react";
import {toggleFullScreen} from "../setting/Operations.tsx";

const {Text} = Typography;
const prevIcon = <LeftOutlined/>;
const nextIcon = <RightOutlined/>;

const HEADER_STYLE = {position: 'relative'} as const;
const SPACE_STYLE = {position: 'absolute', zIndex: 1} as const;
const DRAG_SELECT_STYLE = {width: 120} as const;

export const HeaderBar = memo(function ({schema, curTable}: {
    schema: Schema | undefined;
    curTable: STable | null;
}) {
    const {curPage, curTableId, curId} = useLocationData();
    const {dragPanel, pageConf, history, isNextIdShow, isEditMode} = useMyStore();
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
        const path = historyPrev(curPage, curTableId, curId, history, isEditMode);
        if (path) {
            navigate(path);
        }
    }, [curPage, curTableId, curId, history, isEditMode, navigate]);

    const next = useCallback(() => {
        const path = historyNext(curPage, history, isEditMode);
        if (path) {
            navigate(path);
        }
    }, [curPage, history, isEditMode, navigate]);

    const {editingCurTable, editingCurId, editingIsEdited} = useMyStore();

    let unsavedSign;
    if (editingIsEdited && editingCurTable == curTableId && editingCurId == curId){
        unsavedSign = <Text>{t('unsaved')}</Text>
    }

    let nextId;
    if (isNextIdShow && curTable) {
        const nId = getNextId(curTable, curId);
        if (nId) {
            nextId = <Text>{t('nextSlot')} <Text copyable>{nId}</Text> </Text>
        }
    }

    const dragOptions = useMemo(() => [
        ...(pageConf.pages.map(fp => {
            return {label: fp.label, value: fp.label};
        })),
        {label: t('recordRef'), value: 'recordRef'},
        {label: t('finder'), value: 'finder'},
        {label: t('adder'), value: 'adder'},
        {label: t('setting'), value: 'setting'},
        {label: t('none'), value: 'none'},
    ], [pageConf.pages, t]);


    return <div style={HEADER_STYLE}>
        <Space size={'large'} style={SPACE_STYLE}>
            <Space size={'small'}>
                <Select options={dragOptions}
                        style={DRAG_SELECT_STYLE}
                        value={dragPanel}
                        onChange={setDragPanel}/>

                {schema ? <TableList schema={schema}/> : <Select id='table' loading={true}/>}
                {curTable ? <IdList curTable={curTable}/> : <Skeleton.Input/>}
                {unsavedSign}
                {nextId}

            </Space>
            <Space size={'small'}>
                <Button icon={prevIcon} onClick={prev} disabled={!historyCanPrev(curTableId, curId, history)}/>
                <Button icon={nextIcon} onClick={next} disabled={!history.canNext()}/>
            </Space>
        </Space></div>;
});

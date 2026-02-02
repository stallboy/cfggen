import {Button, Dropdown, Select, Skeleton, Space, Typography} from "antd";
import {LeftOutlined, RightOutlined, AppstoreOutlined} from "@ant-design/icons";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {UnreferencedButton} from "./UnreferencedButton.tsx";
import {
    historyCanPrev,
    historyNext,
    historyPrev,
    navTo,
    setDragPanel,
    useMyStore,
    useLocationData,
    useCurPageRecordOrRecordRef
} from "../../store/store.ts";
import {getNextId, Schema} from "../table/schemaUtil.tsx";
import {useHotkeys} from "react-hotkeys-hook";
import {useNavigate} from "react-router-dom";
import {STable} from "../../api/schemaModel.ts";
import {useTranslation} from "react-i18next";
import {memo, useCallback, useMemo} from "react";
import {toggleFullScreen} from "../setting/colorUtils.ts";

const {Text} = Typography;
const prevIcon = <LeftOutlined/>;
const nextIcon = <RightOutlined/>;

const HEADER_STYLE = {position: 'relative'} as const;
const SPACE_STYLE = {position: 'absolute', zIndex: 1} as const;


export const HeaderBar = memo(function ({schema, curTable}: {
    schema: Schema | undefined;
    curTable: STable | null;
}) {
    const { curPage } = useCurPageRecordOrRecordRef();
    const { curTableId, curId} = useLocationData();
    const {dragPanel, pageConf, history, isNextIdShow, isEditMode} = useMyStore();
    const navigate = useNavigate();
    const {t} = useTranslation();
    useHotkeys('alt+1', () => navigate(navTo('table', curTableId, curId)));
    useHotkeys('alt+2', () => navigate(navTo('tableRef', curTableId, curId)));
    useHotkeys('alt+3', () => navigate(navTo('record', curTableId, curId, isEditMode)));
    useHotkeys('alt+4', () => navigate(navTo('recordRef', curTableId, curId)));
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
    useHotkeys('alt+c', () => prev());
    useHotkeys('alt+v', () => next());

    const {editingCurTable, editingCurId, editingIsEdited} = useMyStore();

    let unsavedSign;
    if (editingIsEdited && editingCurTable == curTableId && editingCurId == curId) {
        unsavedSign = <Text>{t('unsaved')}</Text>
    }

    let nextId;
    if (isNextIdShow && curTable) {
        const nId = getNextId(curTable, curId);
        if (nId) {
            nextId = <Text>{t('nextSlot')} <Text copyable>{nId}</Text> </Text>
        }
    }

    const menuItems = useMemo(() => [
        ...(pageConf.pages.map(fp => {
            return {label: fp.label, key: fp.label};
        })),
        {label: t('recordRef'), key: 'recordRef'},
        {label: t('finder'), key: 'finder'},
        {label: t('chat'), key: 'chat'},
        {label: t('setting'), key: 'setting'},
        {label: t('none'), key: 'none'},
    ], [pageConf.pages, t]);

    return <div style={HEADER_STYLE}>
        <Space size={'small'} style={SPACE_STYLE}>
            <Space size={'small'}>
                <Dropdown menu={{
                    items: menuItems,
                    onClick: (e) => setDragPanel(e.key), selectedKeys: [dragPanel]
                }} trigger={['click']}>
                    <Button icon={<AppstoreOutlined/>} title={t('panelMenu')}/>
                </Dropdown>

                {schema ? <TableList schema={schema}/> : <Select id='table' loading={true}/>}
                {curTable ? <IdList curTable={curTable}/> : <Skeleton.Input/>}
                {curTable ? <UnreferencedButton curTable={curTable}/> : null}
                {unsavedSign}
                {nextId}

            </Space>
            <Space size={'small'}>
                <Button icon={prevIcon} onClick={prev} disabled={!historyCanPrev(curTableId, curId, history)}/>
                <Button icon={nextIcon} onClick={next} disabled={!history.canNext()}/>
            </Space>
        </Space>
    </div>;
});

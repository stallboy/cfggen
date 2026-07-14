import {Badge, Button, Dropdown, Flex, Select, Skeleton, Space, Tooltip, Typography} from "antd";
import {
    ApartmentOutlined,
    CloseOutlined,
    CompassOutlined,
    LeftOutlined,
    RightOutlined,
    FileAddOutlined,
    SettingOutlined, BarsOutlined
} from "@ant-design/icons";
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
} from "@/store/store";
import {getNextId, Schema} from "@/domain/schema";
import {useHotkeys} from "react-hotkeys-hook";
import {useNavigate} from "react-router";
import {STable} from "@/api/schemaModel";
import {useTranslation} from "react-i18next";
import {memo, useCallback, useMemo} from "react";
import {toggleFullScreen} from "@/utils/windowUtils";

const {Text} = Typography;
const prevIcon = <LeftOutlined/>;
const nextIcon = <RightOutlined/>;

const HEADER_STYLE = {position: 'relative'} as const;
const SPACE_STYLE = {position: 'absolute', zIndex: 1} as const;


export const HeaderBar = memo(function ({schema, curTable}: {
    schema: Schema | undefined;
    curTable: STable | null;
}) {
    const {curPage} = useCurPageRecordOrRecordRef();
    const {curTableId, curId} = useLocationData();
    const {dragPanel, pageConf, history, isNextIdShow, isEditMode} = useMyStore();
    const {editingCurTable, editingCurId, editingIsEdited} = useMyStore();
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

    // 未保存：附着在定位条右上角的小圆点（Badge 必须包裹子元素圆点才附着）
    const isUnsaved = editingIsEdited && editingCurTable == curTableId && editingCurId == curId;

    let nextId;
    if (isNextIdShow && curTable) {
        const nId = getNextId(curTable, curId);
        if (nId) {
            nextId = <Tooltip title={t('nextSlot')}>
                <Text copyable type="secondary">{nId}</Text>
            </Tooltip>;
        }
    }

    // 面板切换菜单：内置面板与用户固定页分组（type:'group'）
    const menuItems = useMemo(() => [
        {
            type: 'group' as const, label: t('builtinPanel'), children: [
                {key: 'finder', label: t('finder'), icon: <CompassOutlined/>},
                {key: 'recordRef', label: t('recordRef'), icon: <ApartmentOutlined/>},
                {key: 'add', label: t('addData'), icon: <FileAddOutlined/>},
                {key: 'setting', label: t('setting'), icon: <SettingOutlined/>},
                {key: 'none', label: t('none'), icon: <CloseOutlined/>},
            ]
        },
        ...(pageConf.pages.length ? [{
            type: 'group' as const,
            label: t('pages'),
            children: pageConf.pages.map(fp => ({key: fp.label, label: fp.label})),
        }] : []),
    ], [pageConf.pages, t]);

    // 定位条：表 + 记录 收成一个视觉整体
    const locator = schema
        ? <Space.Compact size="small">
            <TableList schema={schema}/>
            {curTable ? <IdList curTable={curTable}/> : <Skeleton.Input/>}
        </Space.Compact>
        : <Select id='table' loading={true}/>;

    return <div style={HEADER_STYLE}>
        <Flex align="center" justify="space-between" gap="small" style={SPACE_STYLE}>
            {/* 左段：面板切换 + 定位(表/记录) + 状态标记 */}
            <Space size="small" align="center">
                <Dropdown menu={{
                    items: menuItems,
                    onClick: (e) => setDragPanel(e.key), selectedKeys: [dragPanel]
                }} trigger={['click']}>
                    <Button size="small" icon={<BarsOutlined/>} title={t('panelMenu')}/>
                </Dropdown>

                <Badge dot={isUnsaved} status="warning">
                    {locator}
                </Badge>

                {nextId}
            </Space>

            {/* 右段：未引用入口 + 历史导航 */}
            <Space size="small" align="center">
                {curTable ? <UnreferencedButton curTable={curTable}/> : null}
                <Space.Compact>
                    <Button size="small" icon={prevIcon} onClick={prev} title="alt+c"
                            disabled={!historyCanPrev(curTableId, curId, history)}/>
                    <Button size="small" icon={nextIcon} onClick={next} title="alt+v" disabled={!history.canNext()}/>
                </Space.Compact>
            </Space>
        </Flex>
    </div>;
});

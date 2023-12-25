import {useEffect, useState} from "react";
import {
    Alert,
    App,
    Button,
    Drawer,
    Flex,
    Form,
    Input,
    InputNumber,
    Modal,
    Select,
    Space,
    Switch,
    Tabs,
    Tag
} from "antd";
import {CloseOutlined, LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {getNextId, newSchema, Schema} from "./model/schemaModel.ts";
import {History, HistoryItem} from "./model/historyModel.ts";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {TableSchema} from "./TableSchema.tsx";
import {TableRef} from "./TableRef.tsx";
import {TableRecord} from "./TableRecord.tsx";
import {TableRecordRef} from "./TableRecordRef.tsx";
import {SearchValue} from "./SearchValue.tsx";
import {useHotkeys} from "react-hotkeys-hook";
import {getInt, getBool, getStr, getJson} from "./func/localStore.ts";
import {RecordEditResult} from "./model/recordModel.ts";
import {useTranslation} from "react-i18next";
import {getId} from "./func/recordRefEntity.ts";
import {DraggablePanel} from "@ant-design/pro-editor";

export const pageTable = 'table'
export const pageTableRef = 'tableRef'
export const pageRecord = 'record'
export const pageRecordRef = 'recordRef'
export const pageFixed = 'fix'

// export type DraggablePanelType = 'recordRef' | 'fix' | 'none';

export class FixedPage {
    constructor(
        public table: string,
        public id: string,
        public refIn: boolean,
        public refOutDepth: number,
        public maxNode: number) {
    }
}

export function CfgEditorApp() {
    const [server, setServer] = useState<string>(getStr('server', 'localhost:3456'));
    const [schema, setSchema] = useState<Schema | null>(null);
    const [curTableId, setCurTableId] = useState<string>(getStr('curTableId', ''));
    const [curId, setCurId] = useState<string>(getStr('curId', ''));
    const [curPage, setCurPage] = useState<string>(getStr('curPage', pageRecord));
    const [editMode, setEditMode] = useState<boolean>(false);

    const [dragPanel, setDragPanel] = useState<string>(getStr('dragPanel', 'none'));
    const [fix, setFix] = useState<FixedPage | null>(getJson('fix'));

    const [maxImpl, setMaxImpl] = useState<number>(getInt('maxImpl', 10));
    const [refIn, setRefIn] = useState<boolean>(getBool('refIn', true));
    const [refOutDepth, setRefOutDepth] = useState<number>(getInt('refOutDepth', 5));
    const [maxNode, setMaxNode] = useState<number>(getInt('maxNode', 30));
    const [recordRefIn, setRecordRefIn] = useState<boolean>(getBool('recordRefIn', true));
    const [recordRefOutDepth, setRecordRefOutDepth] = useState<number>(getInt('recordRefOutDepth', 5));
    const [recordMaxNode, setRecordMaxNode] = useState<number>(getInt('recordMaxNode', 30));
    const [searchMax, setSearchMax] = useState<number>(getInt('searchMax', 50));

    const [history, setHistory] = useState<History>(new History());
    const [settingOpen, setSettingOpen] = useState<boolean>(false);
    const [searchOpen, setSearchOpen] = useState<boolean>(false);

    const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
    const [isFetching, setIsFetching] = useState<boolean>(false);
    const [fetchErr, setFetchErr] = useState<string>('');

    useHotkeys('alt+1', () => selectCurPage(pageTable));
    useHotkeys('alt+2', () => selectCurPage(pageTableRef));
    useHotkeys('alt+3', () => selectCurPage(pageRecord));
    useHotkeys('alt+4', () => selectCurPage(pageRecordRef));
    useHotkeys('alt+x', () => showSearch());
    useHotkeys('alt+c', () => prev());
    useHotkeys('alt+v', () => next());

    const {notification} = App.useApp();
    const {t} = useTranslation();

    useEffect(() => {
        tryConnect(server);
    }, []);

    function tryConnect(server: string) {
        setSchema(null);
        setIsFetching(true);

        const fetchData = async () => {
            const response = await fetch(`http://${server}/schemas`);
            const rawSchema = await response.json();
            const schema = new Schema(rawSchema);

            setSchema(schema);
            selectCurTableAndIdFromSchema(schema, curTableId, curId);
            setIsFetching(false);
            setIsModalOpen(false);
        }

        fetchData().catch((err) => {
            setIsFetching(false);
            setFetchErr(err.toString());
            setIsModalOpen(true);
        });
    }


    function selectCurTableAndIdFromSchema(schema: Schema,
                                           curTableName: string = curTableId,
                                           curIdStr: string = curId,
                                           fromOp: boolean = true) {
        if (schema == null) {
            return;
        }

        let curTab;
        if (curTableName.length > 0) {
            curTab = schema.getSTable(curTableName);
        }
        if (curTab == null) {
            curTab = schema.getFirstSTable();
        }

        if (curTab) {
            setCurTableId(curTab.name);

            let id = '';
            if (curIdStr.length > 0 && schema.hasId(curTab, curIdStr)) {
                id = curIdStr;
            } else if (curTab.recordIds.length > 0) {
                id = curTab.recordIds[0].id;
            }

            setCurId(id);
            if (fromOp) { // 如果是从prev，next中来的，就不要再设置history了
                setHistory(history.addItem(curTab.name, id));
            }
            localStorage.setItem('curTableId', curTab.name);
            localStorage.setItem('curId', id);
        }
    }

    function selectCurTable(curTableName: string) {
        if (schema) {
            selectCurTableAndIdFromSchema(schema, curTableName);
        }
    }

    function selectCurTableAndId(curTableName: string, curId: string) {
        if (schema) {
            selectCurTableAndIdFromSchema(schema, curTableName, curId);
        }
    }

    let curTable = schema ? schema.getSTable(curTableId) : null;

    function selectCurId(curId: string) {
        if (schema && curTable) {
            selectCurTableAndIdFromSchema(schema, curTable.name, curId);
        }
    }

    function selectHistoryCur(item: HistoryItem | null) {
        if (item && schema) {
            selectCurTableAndIdFromSchema(schema, item.table, item.id, false);
        }
    }

    function prev() {
        let newHistory = history.prev();
        setHistory(newHistory);
        selectHistoryCur(newHistory.cur());
    }

    function next() {
        let newHistory = history.next();
        setHistory(newHistory);
        selectHistoryCur(newHistory.cur());
    }

    function selectCurPage(page: string) {
        setCurPage(page);
        localStorage.setItem('curPage', page);
    }

    let nextId;
    if (curTable) {
        let nId = getNextId(curTable);
        if (nId) {
            nextId = <Tag> next id: {nId}</Tag>
        }
    }


    const showSetting = () => {
        setSettingOpen(true);
    };

    const onSettingClose = () => {
        setSettingOpen(false);
    };

    const showSearch = () => {
        setSearchOpen(true);
    };

    const onSearchClose = () => {
        setSearchOpen(false);
    };

    const leftOp = <Space>
        <Button onClick={showSetting}>
            <SettingOutlined/>
        </Button>
        <Button onClick={showSearch}>
            <SearchOutlined/>
        </Button>
        <TableList schema={schema} curTable={curTable} setCurTable={selectCurTable}/>
        <IdList curTable={curTable} curId={curId} setCurId={selectCurId}/>
        {nextId}
        <Button onClick={prev} disabled={!history.canPrev()}>
            <LeftOutlined/>
        </Button>

        <Button onClick={next} disabled={!history.canNext()}>
            <RightOutlined/>
        </Button>
    </Space>;

    function tryReconnect() {
        tryConnect(server);
    }

    let tableSchema = <div/>;
    let tableRef = <div/>;
    let tableRecord = <div/>;
    let tableRecordRef = <div/>;
    let tableRecordRefFixed = null;
    if (schema != null && curTable != null) {
        tableSchema = <TableSchema schema={schema}
                                   curTable={curTable}
                                   maxImpl={maxImpl}
                                   setCurTable={selectCurTable}
                                   setCurPage={selectCurPage}/>;

        tableRef = <TableRef schema={schema}
                             curTable={curTable}
                             setCurTable={selectCurTable}
                             refIn={refIn}
                             refOutDepth={refOutDepth}
                             maxNode={maxNode}
                             setCurPage={selectCurPage}/>;

        if (curId != null) {
            tableRecord = <TableRecord schema={schema}
                                       curTable={curTable}
                                       curId={curId}
                                       server={server}
                                       tryReconnect={tryReconnect}
                                       selectCurTableAndIdFromSchema={selectCurTableAndIdFromSchema}
                                       setCurPage={selectCurPage}
                                       editMode={editMode}
                                       setEditMode={setEditMode}/>;

            tableRecordRef = <TableRecordRef schema={schema}
                                             curTable={curTable}
                                             curId={curId}
                                             refIn={recordRefIn}
                                             refOutDepth={recordRefOutDepth}
                                             maxNode={recordMaxNode}
                                             server={server}
                                             tryReconnect={tryReconnect}
                                             setCurTableAndId={selectCurTableAndId}
                                             setCurPage={selectCurPage}
                                             setEditMode={setEditMode}/>;
        }
    }

    let items = [
        {key: pageTable, label: <Space>{t('table')}</Space>, children: tableSchema,},
        {key: pageTableRef, label: <Space>{t('tableRef')}</Space>, children: tableRef,},
        {key: pageRecord, label: <Space>{t('record')}</Space>, children: tableRecord,},

    ]

    if (dragPanel != pageRecordRef) {
        items.push({key: pageRecordRef, label: <Space>{t('recordRef')}</Space>, children: tableRecordRef,});
    }

    if (fix && schema) {
        let fixedTable = schema.getSTable(fix.table);
        if (fixedTable) {
            tableRecordRefFixed = <TableRecordRef schema={schema}
                                                  curTable={fixedTable}
                                                  curId={fix.id}
                                                  refIn={fix.refIn}
                                                  refOutDepth={fix.refOutDepth}
                                                  maxNode={fix.maxNode}
                                                  server={server}
                                                  tryReconnect={tryReconnect}
                                                  setCurTableAndId={selectCurTableAndId}
                                                  setCurPage={selectCurPage}
                                                  setEditMode={setEditMode}/>;
            if (dragPanel != pageFixed) {
                items.push({
                    key: pageFixed,
                    label: <Space>{t('fix') + ' ' + getId(fix.table, fix.id)}</Space>,
                    children: tableRecordRefFixed,
                });
            }
        }
    }

    function onTabChange(activeKey: string) {
        selectCurPage(activeKey);
    }


    function onChangeMaxImpl(value: number | null) {
        if (value) {
            setMaxImpl(value);
            localStorage.setItem('maxImpl', value.toString());
        }
    }

    function onChangeRefIn(checked: boolean) {
        setRefIn(checked);
        localStorage.setItem('refIn', checked ? 'true' : 'false');
    }

    function onChangeRefOutDepth(value: number | null) {
        if (value) {
            setRefOutDepth(value);
            localStorage.setItem('refOutDepth', value.toString());
        }
    }

    function onChangeMaxNode(value: number | null) {
        if (value) {
            setMaxNode(value);
            localStorage.setItem('maxNode', value.toString());
        }
    }

    function onChangeRecordRefIn(checked: boolean) {
        setRecordRefIn(checked);
        localStorage.setItem('recordRefIn', checked ? 'true' : 'false');
    }

    function onChangeRecordRefOutDepth(value: number | null) {
        if (value) {
            setRecordRefOutDepth(value);
            localStorage.setItem('recordRefOutDepth', value.toString());
        }
    }

    function onChangeRecordMaxNode(value: number | null) {
        if (value) {
            setRecordMaxNode(value);
            localStorage.setItem('recordMaxNode', value.toString());
        }
    }

    function onChangeSearchMax(value: number | null) {
        if (value) {
            setSearchMax(value);
            localStorage.setItem('searchMax', value.toString());
        }
    }

    function onChangeDragePanel(value: string) {
        setDragPanel(value);
        localStorage.setItem('dragPanel', value);
    }


    function onConnectServer(value: string) {
        setServer(value);
        localStorage.setItem('server', value);
        tryConnect(value);
    }


    function handleModalOk() {
        onConnectServer(server);
    }

    function onDeleteRecord() {
        let url = `http://${server}/recordDelete?table=${curTableId}&id=${curId}`;
        const postData = async () => {
            const response = await fetch(url, {
                method: 'POST',
                cache: "no-cache",
                mode: "cors",
                credentials: "same-origin",
                redirect: "follow",
                referrerPolicy: "no-referrer"
            });
            const editResult: RecordEditResult = await response.json();
            if (editResult.resultCode == 'deleteOk') {
                console.log(editResult);
                setSchema(newSchema(schema!!, editResult.table, editResult.recordIds));
                notification.info({
                    message: `post ${url} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });
            } else {
                notification.warning({
                    message: `post ${url} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        }

        postData().catch((err) => {
            notification.error({
                message: `post ${url} err: ${err.toString()}`,
                placement: 'topRight', duration: 4
            });
            tryReconnect();
        });

    }

    let deleteButton;
    if (schema && curTable && schema.isEditable && curTable.isEditable) {
        deleteButton = <Form.Item wrapperCol={{span: 18, offset: 6,}}>
            <Button type="primary" danger onClick={onDeleteRecord}>
                <CloseOutlined/>{t('deleteCurRecord')}
            </Button>
        </Form.Item>
    }

    function onAddFix() {
        let fp = new FixedPage(curTableId, curId, recordRefIn, recordRefOutDepth, recordMaxNode);
        setFix(fp);
        localStorage.setItem('fix', JSON.stringify(fp));
    }

    let addFixButton;
    if (schema && curTable && curPage == pageRecordRef) {
        addFixButton = <Form.Item wrapperCol={{span: 18, offset: 6,}}>
            <Button type="primary" onClick={onAddFix}>
                {t('addFix')}
            </Button>
        </Form.Item>
    }
    let tab = <Tabs tabBarExtraContent={{'left': leftOp}}
                    items={items}
                    activeKey={curPage}
                    onChange={onTabChange}
                    type="card"/>

    let dragPage = null;
    if (dragPanel == 'recordRef') {
        dragPage = tableRecordRef;
    } else if (dragPanel == 'fix') {
        dragPage = tableRecordRefFixed;
    }
    let content;
    if (dragPage) {
        content = <div style={{
            position: "absolute",
            background: '#f1f1f1',
            border: '2px solid #ddd',
            width: '100%',
            height: '100%',
            display: 'flex',
        }}>
            <DraggablePanel
                placement={'left'}
                style={{background: '#fff', width: '100%', padding: 12}}>
                {dragPage}
            </DraggablePanel>
            <div style={{ flex: 'auto'}}>{tab}</div>
        </div>;
    } else {
        content = tab;
    }
    return <>
        <Modal title={t('serverConnectFail')} open={isModalOpen}
               cancelButtonProps={{disabled: true}}
               closable={false}
               confirmLoading={isFetching}
               okText={t('reconnectCurServer')}
               onOk={handleModalOk}>

            <Flex vertical>
                <Alert message={fetchErr} type='error'/>
                <p> {t('netErrFixTip')} </p>
                <p> {t('curServer')}: {server}</p>
                <Form.Item label={t('newServer') + ':'}>
                    <Input.Search defaultValue={server} enterButton={t('connectNewServer')} onSearch={onConnectServer}/>
                </Form.Item>
            </Flex>
        </Modal>
        {content}
        <Drawer title="setting" placement="left" onClose={onSettingClose} open={settingOpen}>
            <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}>
                <Form.Item label={t('implsShowCnt')}>
                    <InputNumber value={maxImpl} min={1} max={500} onChange={onChangeMaxImpl}/>
                </Form.Item>

                <Form.Item label={t('refIn')}>
                    <Switch checked={refIn} onChange={onChangeRefIn}/>
                </Form.Item>

                <Form.Item label={t('refOutDepth')}>
                    <InputNumber value={refOutDepth} min={1} max={500} onChange={onChangeRefOutDepth}/>
                </Form.Item>

                <Form.Item label={t('maxNode')}>
                    <InputNumber value={maxNode} min={1} max={500} onChange={onChangeMaxNode}/>
                </Form.Item>

                <Form.Item label={t('recordRefIn')}>
                    <Switch checked={recordRefIn} onChange={onChangeRecordRefIn}/>
                </Form.Item>

                <Form.Item label={t('recordRefOutDepth')}>
                    <InputNumber value={recordRefOutDepth} min={1} max={500} onChange={onChangeRecordRefOutDepth}/>
                </Form.Item>

                <Form.Item label={t('recordMaxNode')}>
                    <InputNumber value={recordMaxNode} min={1} max={500} onChange={onChangeRecordMaxNode}/>
                </Form.Item>

                <Form.Item label={t('searchMaxReturn')}>
                    <InputNumber value={searchMax} min={1} max={500} onChange={onChangeSearchMax}/>
                </Form.Item>

                <Form.Item label={t('dragPanel')}>
                    <Select value={dragPanel} options={[{label: t('recordRef'), value: 'recordRef'},
                        {label: t('fix'), value: 'fix'},
                        {label: t('none'), value: 'none'}]} onChange={onChangeDragePanel}/>
                </Form.Item>

                <Form.Item label={t('curServer')}>
                    {server}
                </Form.Item>
                <Form.Item label={t('newServer')}>
                    <Input.Search defaultValue={server} enterButton={t('connect')} onSearch={onConnectServer}/>
                </Form.Item>

                <Form.Item label={<LeftOutlined/>}>
                    alt+x
                </Form.Item>
                <Form.Item label={<RightOutlined/>}>
                    alt+c
                </Form.Item>
                <Form.Item label={t('table')}>
                    alt+1
                </Form.Item>
                <Form.Item label={t('tableRef')}>
                    alt+2
                </Form.Item>
                <Form.Item label={t('record')}>
                    alt+3
                </Form.Item>
                <Form.Item label={t('recordRef')}>
                    alt+4
                </Form.Item>
                <Form.Item label={<SearchOutlined/>}>
                    alt+q
                </Form.Item>

                {addFixButton}
                {deleteButton}
            </Form>
        </Drawer>

        <Drawer title="search" placement="left" onClose={onSearchClose} open={searchOpen} size='large'>
            <SearchValue searchMax={searchMax}
                         server={server}
                         tryReconnect={tryReconnect}
                         setCurTableAndId={selectCurTableAndId}/>
        </Drawer>

    </>;

}


import {useEffect, useRef, useState} from "react";
import {Alert, App, Button, Drawer, Flex, Form, Input, Modal, Space, Tabs, Typography} from "antd";
import {saveAs} from 'file-saver';
import {LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";
import {TableSchema} from "./TableSchema.tsx";
import {TableRef} from "./TableRef.tsx";
import {TableRecord} from "./TableRecord.tsx";
import {TableRecordRef} from "./TableRecordRef.tsx";
import {SearchValue} from "./SearchValue.tsx";
import {useHotkeys} from "react-hotkeys-hook";
import {RecordEditResult} from "./model/recordModel.ts";
import {useTranslation} from "react-i18next";
import {getId} from "./func/recordRefEntity.ts";
import {DraggablePanel} from "@ant-design/pro-editor";
import {toBlob} from "html-to-image";
import {Setting} from "./Setting.tsx";
import {getNextId, newSchema, Schema} from "./model/schemaUtil.ts";
import {historyNext, historyPrev, setCurPage, store} from "./model/store.ts";

const {Text} = Typography;

export const pageTable = 'table'
export const pageTableRef = 'tableRef'
export const pageRecord = 'record'
export const pageRecordRef = 'recordRef'
export const pageFixed = 'fix'


export function CfgEditorApp() {
    const {
        schema, curTableId, curId, curPage, server,
        dragPanel, editMode,imageSizeScale, history
    } = store;

    const [settingOpen, setSettingOpen] = useState<boolean>(false);
    const [searchOpen, setSearchOpen] = useState<boolean>(false);

    const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
    const [isFetching, setIsFetching] = useState<boolean>(false);
    const [fetchErr, setFetchErr] = useState<string>('');

    useHotkeys('alt+1', () => setCurPage(pageTable));
    useHotkeys('alt+2', () => setCurPage(pageTableRef));
    useHotkeys('alt+3', () => setCurPage(pageRecord));
    useHotkeys('alt+4', () => setCurPage(pageRecordRef));
    useHotkeys('alt+x', () => showSearch());
    useHotkeys('alt+c', () => historyPrev());
    useHotkeys('alt+v', () => historyNext());

    const {notification} = App.useApp();
    const {t} = useTranslation();

    useEffect(() => {
        tryConnect(server);
    }, []);

    const ref = useRef<HTMLDivElement>(null)


    let curTable = schema ? schema.getSTable(curTableId) : null;



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


    let nextId;
    if (curTable) {
        let nId = getNextId(curTable, curId);
        if (nId) {
            nextId = <Text>{t('nextSlot')} <Text copyable>{nId}</Text> --- </Text>
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
        <TableList schema={schema} curTable={curTable} setCurTable={setCurTable}/>
        <IdList curTable={curTable} curId={curId} setCurId={selectCurId}/>
        {nextId}
        <Button onClick={historyPrev} disabled={!history.canPrev()}>
            <LeftOutlined/>
        </Button>

        <Button onClick={historyNext} disabled={!history.canNext()}>
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
        tableSchema = <div ref={ref} style={{background: '#fff'}}>
            <TableSchema schema={schema}
                         curTable={curTable}
                         maxImpl={maxImpl}
                         setCurTable={setCurTable}
                         setCurPage={setCurPage}
                         nodeShow={nodeShow}/>
        </div>;

        tableRef = <div ref={ref} style={{background: '#fff'}}>
            <TableRef schema={schema}
                      curTable={curTable}
                      setCurTable={setCurTable}
                      refIn={refIn}
                      refOutDepth={refOutDepth}
                      maxNode={maxNode}
                      setCurPage={setCurPage}
                      nodeShow={nodeShow}/>
        </div>;

        if (curId != null) {
            tableRecord = <div ref={ref} style={{background: '#fff'}}>
                <TableRecord schema={schema}
                             curTable={curTable}
                             curId={curId}
                             server={server}
                             tryReconnect={tryReconnect}
                             selectCurTableAndIdFromSchema={selectCurTableAndIdFromSchema}
                             setCurPage={setCurPage}
                             editMode={editMode}
                             setEditMode={setEditMode}
                             nodeShow={nodeShow}/>
            </div>;

            tableRecordRef = <div ref={ref} style={{background: '#fff'}}>
                <TableRecordRef schema={schema}
                                curTable={curTable}
                                curId={curId}
                                refIn={recordRefIn}
                                refOutDepth={recordRefOutDepth}
                                maxNode={recordMaxNode}
                                server={server}
                                tryReconnect={tryReconnect}
                                setCurTableAndId={setCurTableAndId}
                                setCurPage={setCurPage}
                                setEditMode={setEditMode}
                                query={query}
                                nodeShow={nodeShow}/>;
            </div>;
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
                                                  setCurTableAndId={setCurTableAndId}
                                                  setCurPage={setCurPage}
                                                  setEditMode={setEditMode}
                                                  query={query}
                                                  nodeShow={nodeShow}/>;
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
        setCurPage(activeKey);
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

    function onToPng() {
        if (ref.current === null) {
            return
        }

        let w = ref.current.offsetWidth * imageSizeScale;
        let h = ref.current.offsetHeight * imageSizeScale;

        toBlob(ref.current, {cacheBust: true, canvasWidth: w, canvasHeight: h, pixelRatio: 1})
            .then((blob) => {
                if (blob) {
                    let fn;
                    if (curPage.startsWith("table")) {
                        fn = `${curPage}_${curTableId}.png`;
                    } else {
                        fn = `${curPage}_${curTableId}_${curId}.png`;
                    }
                    saveAs(blob, fn);
                    notification.info({message: "save png to " + fn, duration: 3});
                }
            }).catch((err) => {
            notification.error({message: "save png failed: limit the max node count", duration: 3});
            console.log(err)
        })
    }

    let tab = <Tabs tabBarExtraContent={{'left': leftOp}}
                    items={items}
                    activeKey={curPage}
                    onChange={onTabChange}
                    style={{margin: 0}}
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
            background: '#fff',
            border: '1px solid #ddd', width: '100%',
            height: '100%',
            display: 'flex',
        }}>
            <DraggablePanel
                placement={'left'}
                style={{background: '#fff', width: '100%', padding: 12}}>
                {dragPage}
            </DraggablePanel>
            <div style={{flex: 'auto'}}>{tab}</div>
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
                    <Input.Search enterButton={t('connectNewServer')} onSearch={onConnectServer}/>
                </Form.Item>
            </Flex>
        </Modal>
        {content}
        <Drawer title="setting" placement="left" onClose={onSettingClose} open={settingOpen} size='large'>
            <Setting  {...{
                schema, curTableId, curId, curPage,
                curTable, hasFix: tableRecordRefFixed != null, onDeleteRecord,
                onConnectServer,
                onToPng,

            }}/>
        </Drawer>

        <Drawer title="search" placement="left" onClose={onSearchClose} open={searchOpen} size='large'>
            <SearchValue {...{tryReconnect}}/>
        </Drawer>

    </>;

}


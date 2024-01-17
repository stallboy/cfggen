import {useEffect, useRef, useState} from "react";
import {
    Alert,
    App,
    Button,
    Drawer,
    Flex,
    Form,
    Input,
    Modal,
    Radio,
    RadioChangeEvent,
    Space,
    Typography
} from "antd";
import {saveAs} from 'file-saver';
import {LeftOutlined, RightOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import {TableList} from "./TableList.tsx";
import {IdList} from "./IdList.tsx";

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
import {
    historyNext,
    historyPrev,
    navTo,
    setSchema, setSchemaNull,
    setServer,
    store, useLocationData
} from "./model/store.ts";
import {Outlet, useNavigate} from "react-router-dom";
import {STable} from "./model/schemaModel.ts";
import {fetchSchema} from "./model/api.ts";

const {Text} = Typography;

export type SchemaTableType = { schema: Schema, curTable: STable };


export function CfgEditorApp() {
    const {
        schema, server,
        fix, dragPanel,
        recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow,
        imageSizeScale, history
    } = store;

    const {curPage, curTableId, curId} = useLocationData();
    const navigate = useNavigate();
    const [settingOpen, setSettingOpen] = useState<boolean>(false);
    const [searchOpen, setSearchOpen] = useState<boolean>(false);
    const [isFetching, setIsFetching] = useState<boolean>(false);
    const [fetchError, setFetchError] = useState<string | null>(null);


    useHotkeys('alt+1', () => navigate(navTo('table', curTableId, curId)));
    useHotkeys('alt+2', () => navigate(navTo('tableRef', curTableId, curId)));
    useHotkeys('alt+3', () => navigate(navTo('record', curTableId, curId)));
    useHotkeys('alt+4', () => navigate(navTo('recordRef', curTableId, curId)));
    useHotkeys('alt+x', () => showSearch());
    useHotkeys('alt+c', () => prev());
    useHotkeys('alt+v', () => next());

    const {notification} = App.useApp();
    const {t} = useTranslation();
    const ref = useRef<HTMLDivElement>(null)

    useEffect(() => {
        if (!schema) {
            setSchemaNull();
            setIsFetching(true);
            setFetchError(null);

            const fetchData = async () => {
                const rawSchema = await fetchSchema(server);
                const schema = new Schema(rawSchema);
                const [tableId, id] = setSchema(schema, curTableId, curId);
                navigate(navTo(curPage, tableId, id));

                setIsFetching(false);
                setFetchError(null);
            }

            fetchData().catch((err) => {
                setIsFetching(false);
                setFetchError(err.toString());
            });
        }
    }, [schema])


    let curTable = schema ? schema.getSTable(curTableId) : null;

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

    function onChangeCurPage(e: RadioChangeEvent) {
        const page = e.target.value;
        navigate(navTo(page, curTableId, curId));
    }

    function onConnectServer(value: string) {
        setServer(value);
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
                const schemaNew = newSchema(schema!!, editResult.table, editResult.recordIds)
                const [tableId, id] = setSchema(schemaNew, curTableId, curId);
                navigate(navTo(curPage, tableId, id));

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


    let content;
    if (schema == null || curTable == null) {
        content = <></>
    } else {

        let dragPage = null;
        if (dragPanel == 'recordRef') {
            dragPage = <TableRecordRef schema={schema}
                                       curTable={curTable}
                                       curId={curId}
                                       refIn={recordRefIn}
                                       refOutDepth={recordRefOutDepth}
                                       maxNode={recordMaxNode}
                                       nodeShow={nodeShow}/>;
        } else if (dragPanel == 'fix' && fix) {
            let fixedTable = schema.getSTable(fix.table);
            if (fixedTable) {
                dragPage = <TableRecordRef schema={schema}
                                           curTable={fixedTable}
                                           curId={fix.id}
                                           refIn={fix.refIn}
                                           refOutDepth={fix.refOutDepth}
                                           maxNode={fix.maxNode}
                                           nodeShow={fix.nodeShow}/>;
            }
        }

        if (dragPage) {
            content = <div style={{
                position: "absolute",
                background: '#fff',
                display: 'flex',
                height: "100vh", width: "100vw"
            }}>
                <DraggablePanel
                    placement={'left'}
                    style={{background: '#fff', width: '100%', padding: 12}}>
                    {dragPage}
                </DraggablePanel>
                <div ref={ref} style={{flex: 'auto'}}>
                    <Outlet context={{schema, curTable} satisfies SchemaTableType}/>
                </div>
            </div>;
        } else {
            content = <div ref={ref} style={{height: "100vh", width: "100vw"}}>
                <Outlet context={{schema, curTable}  satisfies SchemaTableType}/>
            </div>;
        }
    }


    return <div style={{position: 'relative'}}>
        <Space size={'large'} style={{position: 'absolute', zIndex: 1}}>
            <Space>
                <Button onClick={showSetting}>
                    <SettingOutlined/>
                </Button>
                <Button onClick={showSearch}>
                    <SearchOutlined/>
                </Button>
                <TableList/>
                <IdList/>
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
        </Space>

        {content}

        <Modal title={t('serverConnectFail')} open={!schema && !!fetchError}
               cancelButtonProps={{disabled: true}}
               closable={false}
               confirmLoading={isFetching}
               okText={t('reconnectCurServer')}
               onOk={handleModalOk}>

            <Flex vertical>
                <Alert message={fetchError ?? ''} type='error'/>
                <p> {t('netErrFixTip')} </p>
                <p> {t('curServer')}: {server}</p>
                <Form.Item label={t('newServer') + ':'}>
                    <Input.Search enterButton={t('connectNewServer')} onSearch={onConnectServer}/>
                </Form.Item>
            </Flex>
        </Modal>


        <Drawer title="setting" placement="left" onClose={onSettingClose} open={settingOpen} size='large'>
            <Setting  {...{
                schema, curTableId, curId, curPage,
                curTable, onDeleteRecord,
                onConnectServer,
                onToPng,

            }}/>
        </Drawer>

        <Drawer title="search" placement="left" onClose={onSearchClose} open={searchOpen} size='large'>
            <SearchValue/>
        </Drawer>

    </div>;

}


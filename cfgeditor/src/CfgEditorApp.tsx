import {useEffect, useMemo, useRef, useState} from "react";
import {Alert, Drawer, Flex, Form, Input, Modal,} from "antd";
import {RecordRef} from "./routes/record/RecordRef.tsx";
import {SearchValue} from "./routes/search/SearchValue.tsx";
import {useHotkeys} from "react-hotkeys-hook";
import {useTranslation} from "react-i18next";
import {DraggablePanel} from "@ant-design/pro-editor";
import {Setting} from "./routes/setting/Setting.tsx";
import {Schema} from "./routes/table/schemaUtil.ts";
import {getLastNavToInLocalStore, setServer, store, useLocationData} from "./routes/setting/store.ts";
import {Outlet, useNavigate} from "react-router-dom";
import {STable} from "./routes/table/schemaModel.ts";
import {fetchSchema} from "./io/api.ts";
import {useQuery} from "@tanstack/react-query";
import {HeaderBar} from "./routes/headerbar/HeaderBar.tsx";
import {FlowGraph} from "./flow/FlowGraph.tsx";


export type SchemaTableType = { schema: Schema, curTable: STable };


export function CfgEditorApp() {
    const {
        server, fix, dragPanel,
        recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow,
    } = store;

    const {curTableId, curId} = useLocationData();
    const [settingOpen, setSettingOpen] = useState<boolean>(false);
    const [searchOpen, setSearchOpen] = useState<boolean>(false);
    const navigate = useNavigate();

    useHotkeys('alt+x', () => setSearchOpen(true));

    const {t} = useTranslation();
    const ref = useRef<HTMLDivElement>(null)
    const {isLoading, isError, error, data: schema} = useQuery({
        queryKey: ['schema'],
        queryFn: ({signal}) => fetchSchema(server, signal),
        staleTime: 1000 * 60 * 5,
    })

    useEffect(() => {
        if (schema && curTableId.length == 0) {
            navigate(getLastNavToInLocalStore());
        }
    }, [schema]);

    let curTable = schema ? schema.getSTable(curTableId) : null;

    const outletCtx = useMemo(() => {
        return {schema, curTable}
    }, [schema, curTable]);

    const onSettingClose = () => {
        setSettingOpen(false);
    };

    const onSearchClose = () => {
        setSearchOpen(false);
    };

    function onConnectServer(value: string) {
        setServer(value);
    }

    function handleModalOk() {
        onConnectServer(server);
    }


    let content;
    if ((!schema) || curTable == null) {
        // console.log("empty content");
        content = <></>
    } else {
        let dragPage = null;
        if (dragPanel == 'recordRef') {
            dragPage = <RecordRef schema={schema}
                                  curTable={curTable}
                                  curId={curId}
                                  refIn={recordRefIn}
                                  refOutDepth={recordRefOutDepth}
                                  maxNode={recordMaxNode}
                                  nodeShow={nodeShow}
                                  inDragPanelAndFix={false}/>;
        } else if (dragPanel == 'fix' && fix) {
            let fixedTable = schema.getSTable(fix.table);
            if (fixedTable) {
                dragPage = <RecordRef schema={schema}
                                      curTable={fixedTable}
                                      curId={fix.id}
                                      refIn={fix.refIn}
                                      refOutDepth={fix.refOutDepth}
                                      maxNode={fix.maxNode}
                                      nodeShow={fix.nodeShow}
                                      inDragPanelAndFix={true}/>;
            }
        }

        if (dragPage) {
            content = <div style={{
                position: "absolute",
                background: '#fff',
                display: 'flex',
                height: "100vh",
                width: "100vw"
            }}>
                <DraggablePanel
                    placement={'left'}
                    style={{background: '#fff', width: '100%', padding: 12}}>

                    <FlowGraph>
                        {dragPage}
                    </FlowGraph>
                </DraggablePanel>
                <div ref={ref} style={{flex: 'auto'}}>
                    <FlowGraph>
                        <Outlet context={outletCtx}/>
                    </FlowGraph>
                </div>
            </div>;
        } else {
            content = <div ref={ref} style={{height: "100vh", width: "100vw"}}>
                <FlowGraph>
                    <Outlet context={outletCtx}/>
                </FlowGraph>
            </div>;
        }
    }

    return <div>
        <HeaderBar schema={schema} curTable={curTable}
                   setSettingOpen={setSettingOpen} setSearchOpen={setSearchOpen}/>

        {content}

        <Modal title={t('serverConnectFail')} open={isError}
               cancelButtonProps={{disabled: true}}
               closable={false}
               confirmLoading={isLoading}
               okText={t('reconnectCurServer')}
               onOk={handleModalOk}>

            <Flex vertical>
                <Alert message={error ? error.message : ''} type='error'/>
                <p> {t('netErrFixTip')} </p>
                <p> {t('curServer')}: {server}</p>
                <Form.Item label={t('newServer') + ':'}>
                    <Input.Search enterButton={t('connectNewServer')} onSearch={onConnectServer}/>
                </Form.Item>
            </Flex>
        </Modal>

        <Drawer title="setting" placement="left" onClose={onSettingClose} open={settingOpen} size='large'>
            <Setting schema={schema} curTable={curTable} flowRef={ref}/>
        </Drawer>

        <Drawer title="search" placement="left" onClose={onSearchClose} open={searchOpen} size='large'>
            <SearchValue/>
        </Drawer>
    </div>;
}


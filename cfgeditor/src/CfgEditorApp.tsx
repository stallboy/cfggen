import {CSSProperties, useCallback, useEffect, useMemo, useRef, useState} from "react";
import {Alert, Drawer, Flex, Form, Input, Modal, Splitter,} from "antd";
import {RecordRef} from "./routes/record/RecordRef.tsx";
import {useHotkeys} from "react-hotkeys-hook";
import {useTranslation} from "react-i18next";
import {Setting} from "./routes/setting/Setting.tsx";
import {Schema} from "./routes/table/schemaUtil.ts";
import {
    getFixedPage,
    getLastNavToInLocalStore,
    setServer,
    store,
    useLocationData
} from "./routes/setting/store.ts";
import {Outlet, useNavigate} from "react-router-dom";
import {STable} from "./routes/table/schemaModel.ts";
import {fetchNotes, fetchSchema} from "./routes/api.ts";
import {useQuery} from "@tanstack/react-query";
import {HeaderBar} from "./routes/headerbar/HeaderBar.tsx";
import {FlowGraph} from "./flow/FlowGraph.tsx";
import {Query} from "./routes/search/Query.tsx";
import {Finder} from "./routes/search/Finder.tsx";
import {Adder} from "./routes/search/Adder.tsx";


export type SchemaTableType = {
    schema: Schema,
    notes?: Map<string, string>,
    curTable: STable
};

const contentDivStyle: CSSProperties = {
    position: "absolute",
    background: '#fff',
    display: 'flex',
    height: "100vh",
    width: "100vw"
};

const fullDivStyle = {height: "100vh", width: "100vw"};
const disabledProps = {disabled: true}
const autoOverflow = {overflow: 'auto'}
const fullHeight = {height: '100%'}

function onConnectServer(value: string) {
    setServer(value);
}

export function CfgEditorApp() {
    const {
        server, dragPanel, pageConf,
        recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow,
    } = store;

    const {curTableId, curId} = useLocationData();
    const [settingOpen, setSettingOpen] = useState<boolean>(false);
    const [queryOpen, setQueryOpen] = useState<boolean>(false);
    const navigate = useNavigate();

    useHotkeys('alt+x', () => setQueryOpen(true));

    const {t} = useTranslation();
    const ref = useRef<HTMLDivElement>(null)
    const {isLoading, isError, error, data: schema} = useQuery({
        queryKey: ['schema'],
        queryFn: ({signal}) => fetchSchema(server, signal),
        staleTime: 1000 * 60 * 5,
    })

    const {data: notes} = useQuery({
        queryKey: ['notes'],
        queryFn: ({signal}) => fetchNotes(server, signal),
        staleTime: 1000 * 60 * 5,
    })


    useEffect(() => {
        if (schema && curTableId.length == 0) {
            navigate(getLastNavToInLocalStore());
        }
    }, [curTableId.length, navigate, schema]);

    const curTable = schema ? schema.getSTable(curTableId) : null;

    const outletCtx = useMemo(() => {
        return {schema, notes, curTable}
    }, [schema, notes, curTable]);

    const onSettingClose = useCallback(() => {
        setSettingOpen(false);
    }, [setSettingOpen]);

    const onQueryClose = useCallback(() => {
        setQueryOpen(false);
    }, [setQueryOpen]);


    const handleModalOk = useCallback(() => {
        onConnectServer(server);
    }, [server]);

    let content;
    if ((!schema) || curTable == null) {
        // console.log("empty content");
        content = <></>
    } else {
        let dragPage = null;
        if (dragPanel == 'recordRef') {
            dragPage = <FlowGraph>
                <RecordRef schema={schema}
                           notes={notes}
                           curTable={curTable}
                           curId={curId}
                           refIn={recordRefIn}
                           refOutDepth={recordRefOutDepth}
                           maxNode={recordMaxNode}
                           nodeShow={nodeShow}
                           inDragPanelAndFix={false}/>
            </FlowGraph>;
        } else if (dragPanel == 'finder') {
            dragPage = <Finder schema={schema}/>;

        } else if (dragPanel == 'adder') {
            dragPage = <Adder schema={schema}/>

        } else if (dragPanel != 'none') {
            const fix = getFixedPage(pageConf, dragPanel);
            if (fix) {
                const fixedTable = schema.getSTable(fix.table);
                if (fixedTable) {
                    dragPage = <FlowGraph>
                        <RecordRef schema={schema}
                                   notes={notes}
                                   curTable={fixedTable}
                                   curId={fix.id}
                                   refIn={fix.refIn}
                                   refOutDepth={fix.refOutDepth}
                                   maxNode={fix.maxNode}
                                   nodeShow={fix.nodeShow}
                                   inDragPanelAndFix={true}/>
                    </FlowGraph>;
                }
            }
        }

        if (dragPage) {
            content = <Splitter style={contentDivStyle}>
                <Splitter.Panel defaultSize="20%" style={autoOverflow}>
                    <div style={fullHeight}>
                        {dragPage}
                    </div>
                </Splitter.Panel>
                <Splitter.Panel>
                    <div ref={ref} style={fullHeight}>
                        <FlowGraph>
                            <Outlet context={outletCtx}/>
                        </FlowGraph>
                    </div>
                </Splitter.Panel>
            </Splitter>;
        } else {
            content = <div ref={ref} style={fullDivStyle}>
                <FlowGraph>
                    <Outlet context={outletCtx}/>
                </FlowGraph>
            </div>;
        }
    }

    return <div>
        <HeaderBar schema={schema} curTable={curTable}
                   setSettingOpen={setSettingOpen} setSearchOpen={setQueryOpen}/>

        {content}

        <Modal title={t('serverConnectFail')} open={isError}
               cancelButtonProps={disabledProps}
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

        <Drawer title={t("setting")} placement="left" onClose={onSettingClose} open={settingOpen} size='large'>
            <Setting schema={schema} curTable={curTable} flowRef={ref}/>
        </Drawer>

        <Drawer title={t("query")} placement="left" onClose={onQueryClose} open={queryOpen} size='large'>
            <Query schema={schema}/>
        </Drawer>
    </div>
        ;
}


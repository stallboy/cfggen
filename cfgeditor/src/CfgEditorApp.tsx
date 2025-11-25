import {CSSProperties, memo, useCallback, useEffect, useMemo, useRef} from "react";
import {Alert, Flex, Form, Input, Modal, Splitter,} from "antd";
import {RecordRef} from "./routes/record/RecordRef.tsx";
import {useTranslation} from "react-i18next";
import {Setting} from "./routes/setting/Setting.tsx";
import {Schema} from "./routes/table/schemaUtil.tsx";
import {
    getFixedPage,
    getLastNavToInLocalStore,
    setServer,
    useMyStore,
    useLocationData
} from "./store/store.ts";
import {Outlet, useNavigate} from "react-router-dom";
import {STable} from "./routes/table/schemaModel.ts";
import {fetchNotes, fetchSchema} from "./routes/api.ts";
import {useQuery} from "@tanstack/react-query";
import {HeaderBar} from "./routes/headerbar/HeaderBar.tsx";
import {FlowGraph} from "./flow/FlowGraph.tsx";
import {Finder} from "./routes/search/Finder.tsx";


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

export const CfgEditorApp = memo(function CfgEditorApp() {
    const {
        server, dragPanel, pageConf,
        recordRefIn, recordRefOutDepth, recordMaxNode, nodeShow,
    } = useMyStore();

    const {curTableId, curId} = useLocationData();
    const navigate = useNavigate();

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
            dragPage = <Finder schema={schema}/> //TODO

        } else if (dragPanel == 'setting') {
            dragPage = <Setting schema={schema} curTable={curTable} flowRef={ref}/>

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
        <HeaderBar schema={schema} curTable={curTable}/>

        {content}

        <Modal title={t('serverConnectFail')} open={isError}
               cancelButtonProps={disabledProps}
               closable={false}
               confirmLoading={isLoading}
               okText={t('reconnectCurServer')}
               onOk={handleModalOk}>

            <Flex vertical>
                <Alert title={error ? error.message : ''} type='error'/>
                <p> {t('netErrFixTip')} </p>
                <p> {t('curServer')}: {server}</p>
                <Form.Item label={t('newServer') + ':'}>
                    <Input.Search enterButton={t('connectNewServer')} onSearch={onConnectServer}/>
                </Form.Item>
            </Flex>
        </Modal>


    </div>
        ;
});


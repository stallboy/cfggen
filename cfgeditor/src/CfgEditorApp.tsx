import {CSSProperties, lazy, memo, Suspense, useCallback, useEffect, useMemo, useRef} from "react";
import {Alert, Flex, Form, Input, Modal, Splitter,} from "antd";
import {useTranslation} from "react-i18next";
import {Schema} from "./domain/schema.tsx";
import {
    getFixedPage,
    getLastNavToInLocalStore,
    setServer,
    useMyStore,
    useLocationData,
    isFixedRefPage,
    isFixedUnrefPage
} from "./store/store.ts";
import {Outlet, useNavigate} from "react-router";
import {RawSchema, STable} from "./api/schemaModel.ts";
import {fetchNotes, fetchSchema} from "./api/api.ts";
import {notesToMap} from "./api/noteModel.ts";
import {useQuery} from "@tanstack/react-query";
import {HeaderBar} from "./routes/headerbar/HeaderBar.tsx";
import {FlowGraph} from "./flow/FlowGraph.tsx";
import {FlowStyleManager} from "./flow/FlowStyleManager.tsx";
import {Finder} from "./routes/search/Finder.tsx";

// Chat / Setting 仅在 dragPanel 切换到对应面板时才渲染，懒加载以推迟
// @ant-design/x* + openai + marked + dompurify 等重依赖的解析（不进首屏）
const Chat = lazy(() => import("./routes/add/Chat.tsx").then(m => ({default: m.Chat})));
const Setting = lazy(() => import("./routes/setting/Setting.tsx").then(m => ({default: m.Setting})));
const RecordRef = lazy(() => import("./routes/record/RecordRef.tsx").then(m => ({default: m.RecordRef})));


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

// select 必须是稳定引用：内联箭头每次 render 新身份 → React Query 每次 render 重跑 select →
// 重建 Schema（构造函数遍历全部 items、建多个 Map、为每张 table 建 idMap，毫秒级）；且 Schema 是含 Map
// 字段的 class 实例，replaceEqualDeep 判不等 → schema 引用每帧变 → outletCtx 每帧新建 → Outlet 子树
// （Table/TableRef/Record/RecordRef，context 变化绕过 memo）全树重渲。提为模块级常量后只在 rawSchema 变化时构造。
const schemaSelector = (rawSchema: RawSchema) => new Schema(rawSchema);

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
        select: schemaSelector,
    })

    const {data: notes} = useQuery({
        queryKey: ['notes'],
        queryFn: ({signal}) => fetchNotes(server, signal),
        staleTime: 1000 * 60 * 5,
        select: notesToMap,
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
        } else if (dragPanel == 'chat') {
            dragPage = <Suspense fallback={null}><Chat schema={schema} key={'chat-' + curTableId}/></Suspense>;

        } else if (dragPanel == 'setting') {
            dragPage = <Suspense fallback={null}><Setting schema={schema} curTable={curTable} flowRef={ref}/></Suspense>

        } else if (dragPanel != 'none') {
            const fix = getFixedPage(pageConf, dragPanel);
            if (fix) {
                const fixedTable = schema.getSTable(fix.table);
                if (fixedTable) {
                    if (isFixedRefPage(fix)) {  // 固定记录引用页面
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
                    } else if (isFixedUnrefPage(fix)) {  // 固定未引用记录页面
                        dragPage = <FlowGraph>
                            <RecordRef schema={schema}
                                       notes={notes}
                                       curTable={fixedTable}
                                       curId={undefined}  // 未引用模式，没有id
                                       refIn={false}       // 无意义，保留字段
                                       refOutDepth={fix.refOutDepth}
                                       maxNode={fix.maxNode}
                                       nodeShow={fix.nodeShow}
                                       inDragPanelAndFix={true}/>
                        </FlowGraph>;
                    }
                }
            }
        }

        if (dragPage) {
            content = <Splitter style={contentDivStyle}>
                <Splitter.Panel defaultSize="20%" style={autoOverflow}>
                    <div style={fullHeight}>
                        <Suspense fallback={null}>
                            {dragPage}
                        </Suspense>
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
        {/* 全局 CSS 变量（--edge-stroke-width）只挂一次，避免多画布 cleanup 互相抹掉 */}
        <FlowStyleManager/>
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


import {CSSProperties, lazy, memo, Suspense, useCallback, useEffect, useMemo, useRef} from "react";
import {Alert, Flex, Form, Input, Modal, Splitter,} from "antd";
import {useTranslation} from "react-i18next";
import {Schema} from "@/domain/schema";
import {
    getFixedPage,
    getLastNavToInLocalStore,
    setDragPanel,
    setServer,
    useMyStore,
    useLocationData,
    isFixedRefPage,
    isFixedUnrefPage
} from "@/store/store";
import {Outlet, useNavigate} from "react-router";
import {RawSchema} from "@/api/schemaModel";
import {fetchNotes, fetchSchema} from "@/api/api";
import {notesToMap} from "@/api/noteModel";
import {useQuery} from "@tanstack/react-query";
import {useHotkeys} from "react-hotkeys-hook";
import {HeaderBar} from "@/routes/headerbar/HeaderBar";
import {FlowGraph} from "@/flow/FlowGraph";
import {FlowStyleManager} from "@/flow/FlowStyleManager";
import {Finder} from "@/routes/search/Finder";
import {SidePanelShell} from "./SidePanelShell";
import {getCurrentEditingSession} from "@/services/editingSession";

// Chat / Setting 仅在 dragPanel 切换到对应面板时才渲染，懒加载以推迟
// @ant-design/x* + openai + marked + dompurify 等重依赖的解析（不进首屏）
const AddPanel = lazy(() => import("@/routes/add/AddPanel").then(m => ({default: m.AddPanel})));
const Setting = lazy(() => import("@/routes/setting/Setting").then(m => ({default: m.Setting})));
const RecordRef = lazy(() => import("@/routes/record/RecordRef").then(m => ({default: m.RecordRef})));

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

    // alt+s「提交」全局命令：单实例根注册唯一监听，直达当前编辑会话的 submit()。
    // funcSubmit 字段（仅根 STable 追加）唯一逻辑即 session.submit()（见 recordEditEntityCreator.ts），
    // 故「提交」是「当前编辑会话」的全局语义——不再 per-form 注册、不靠焦点路由。原先每个 EntityForm 各注册
    // 一个 useHotkeys 靠 DOM 冒泡「碰巧」命中，是历史 N 次触发 bug 的过设计修复。FlowGraph 多实例（分割布局），
    // 监听必须在更高的单实例点；getCurrentEditingSession 在非编辑视图为 null，自动无副作用。
    useHotkeys("alt+s", () => {
        getCurrentEditingSession()?.submit();
    }, {enableOnFormTags: true});

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

    // 旧版本 dragPanel='chat' 持久化值迁移到 'add'（AddPanel 取代了 Chat）
    useEffect(() => {
        if (dragPanel === 'chat') setDragPanel('add');
    }, [dragPanel]);

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
                           curPage={'recordRef'}
                           curId={curId}
                           refIn={recordRefIn}
                           refOutDepth={recordRefOutDepth}
                           maxNode={recordMaxNode}
                           nodeShow={nodeShow}
                           inDragPanelAndFix={false}/>
            </FlowGraph>;
        } else if (dragPanel == 'finder') {
            dragPage = <SidePanelShell><Finder schema={schema}/></SidePanelShell>;
        } else if (dragPanel == 'add') {
            dragPage = <SidePanelShell><Suspense fallback={null}><AddPanel schema={schema} key={'add-' + curTableId}/></Suspense></SidePanelShell>;

        } else if (dragPanel == 'setting') {
            dragPage = <SidePanelShell><Suspense fallback={null}><Setting schema={schema} curTable={curTable} flowRef={ref}/></Suspense></SidePanelShell>

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
                                       curPage={'recordRef'}
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
                                       curPage={'recordUnref'}
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


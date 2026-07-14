import {Background, Controls, Edge, Node, NodeTypes, ReactFlow, ReactFlowProvider} from "@xyflow/react";
import {Button, ConfigProvider, Result} from "antd";
import {useTranslation} from "react-i18next";
import {Entity} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";
import {memo, MouseEvent as ReactMouseEvent, ReactNode, useCallback, useMemo, useState} from "react";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";
import {FlowGraphContext} from "./FlowGraphContext.ts";

// EntityNode.data 是「呈现层下发袋」：entity 是纯 domain（不可变、memo-safe），
// nodeShow/notes 是呈现层数据，由 useEntityToGraph 经 convertNodeAndEdges 写入 node.data，
// 而非盖章到 entity.sharedSetting（domain 与 presentation 解耦，entity 保持不可变）。
// nodeShow 走 node.data（非子组件直接 useStore）以保留 FixedPage 的 per-graph override（doc A2）；
// query 不在此列——它无 per-graph override，渲染组件各自 useMyStore() 订阅（resso per-key，零多余重渲）。
// 用 type 别名而非 interface：xyflow 的 Node<T> 要求 T extends Record<string,unknown>，
// type 字面量带隐式索引签名可满足，interface 不带（会被判 index signature 缺失）。
export type EntityNodeData = {
    entity: Entity;
    nodeShow?: NodeShowType;
    notes?: Map<string, string>;
};

export type EntityNode = Node<EntityNodeData, "node">;
export type EntityEdge = Edge;
export type NodeMenuFunc = (entityNode: EntityNode) => MenuItem[];
export type NodeDoubleClickFunc = (entityNode: EntityNode) => void;

const nodeTypes: NodeTypes = {
    node: FlowNode,
};

const defaultNodes: EntityNode[] = [];
const defaultEdges: EntityEdge[] = [];

const proOptions = {hideAttribution: true};

// 图内 EntityForm 的主题（仅 Form.itemMarginBottom）。
// 原写在 EntityForm 里、每节点套一个 ConfigProvider：N 个可编辑节点 = N 个 ConfigProvider（各做一次 theme 合并
// + context Provider 实例，N=45 时是初始 mount 的可观开销）。上提到 FlowGraph 单实例：FlowGraph 子树唯一的
// antd Form 就是 EntityForm（Background/Controls/FlowContextMenu 均非 Form，FORM_THEME 对它们无影响），
// recordRef/table 等只读视图的 FlowGraph 节点无 Form 亦不受影响——语义不变，ConfigProvider 45→1。
const FORM_THEME = {
    components: {
        Form: {
            itemMarginBottom: 8,
        },
    },
};

export const FlowGraph = memo(function FlowGraph({children}: {
    children: ReactNode
}) {
    const {t} = useTranslation();
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);

    const [paneMenu, setPaneMenu] = useState<MenuItem[]>([]);
    const [nodeMenuFunc, setNodeMenuFunc] = useState<NodeMenuFunc>();
    const [nodeDoubleClickFunc, setNodeDoubleClickFunc] = useState<NodeDoubleClickFunc>();

    // 布局失败覆盖层：layoutError 由 useEntityToGraph 透传，retryLayout 为 invalidate 该图 layout 缓存的回调。
    const [layoutError, setLayoutError] = useState<Error | undefined>(undefined);
    const [retryLayout, setRetryLayout] = useState<(() => void) | undefined>(undefined);

    const onPaneContextMenu = useCallback((event: ReactMouseEvent<Element, MouseEvent> | MouseEvent) => {
            event.stopPropagation();
            event.preventDefault();
            setMenuStyle({top: event.clientY - 30, left: event.clientX - 30,});
            setMenuItems(paneMenu);
        },
        [paneMenu, setMenuStyle, setMenuItems],
    );
    const onNodeContextMenu = useCallback((event: ReactMouseEvent, flowNode: EntityNode) => {
            event.stopPropagation();
            event.preventDefault();           // Prevent native context menu from showing
            setMenuStyle({top: event.clientY - 30, left: event.clientX - 30,});
            setMenuItems(nodeMenuFunc ? nodeMenuFunc(flowNode) : undefined);
        },
        [nodeMenuFunc, setMenuStyle, setMenuItems],
    );
    const onNodeDoubleClick = useCallback((_event: ReactMouseEvent, flowNode: EntityNode) => {
            nodeDoubleClickFunc?.(flowNode);
        },
        [nodeDoubleClickFunc],
    );

    const closeMenu = useCallback(() => {
        setMenuStyle(undefined)
    }, [setMenuStyle]);

    const thisSetNodeMenuFunc = useCallback(function (func: NodeMenuFunc) {
        setNodeMenuFunc(() => func);
    }, [setNodeMenuFunc]);

    const thisSetNodeDoubleClickFunc = useCallback(function (func: NodeDoubleClickFunc) {
        setNodeDoubleClickFunc(() => func);
    }, [setNodeDoubleClickFunc]);

    // retryLayout 是函数——用「存回调」写法 setRetryLayout(() => fn) 避免 React 把它当 functional updater 调用。
    const thisSetRetryLayout = useCallback(function (fn: () => void) {
        setRetryLayout(() => fn);
    }, [setRetryLayout]);

    const ctx = useMemo(() => {
        return {
            setPaneMenu,
            setNodeMenuFunc: thisSetNodeMenuFunc,
            setNodeDoubleClickFunc: thisSetNodeDoubleClickFunc,
            setLayoutError,
            setRetryLayout: thisSetRetryLayout,
        }
    }, [setPaneMenu, thisSetNodeMenuFunc, thisSetNodeDoubleClickFunc, setLayoutError, thisSetRetryLayout]);


    return <ReactFlowProvider>
        <ConfigProvider theme={FORM_THEME}>
            {/* 定位容器：为布局失败覆盖层提供 relative 锚定，不影响 ReactFlow 撑满父级（100%/100%） */}
            <div className='flowGraphCanvas'>
                <ReactFlow
                    defaultNodes={defaultNodes}
                    defaultEdges={defaultEdges}
                    nodeTypes={nodeTypes}
                    minZoom={0.1}
                    maxZoom={2}
                    deleteKeyCode={null}
                    onNodeDoubleClick={onNodeDoubleClick}
                    onNodeContextMenu={onNodeContextMenu}
                    onPaneClick={closeMenu}
                    onNodeClick={closeMenu}
                    onMoveStart={closeMenu}
                    onNodeDragStart={closeMenu}
                    onPaneContextMenu={onPaneContextMenu}
                    // onlyRenderVisibleElements
                    proOptions={proOptions}>
                    <Background/>
                    <Controls showZoom={false}/>
                </ReactFlow>
                {(menuStyle && menuItems && menuItems.length > 0) &&
                    <FlowContextMenu menuStyle={menuStyle} menuItems={menuItems} closeMenu={closeMenu}/>}
                {layoutError &&
                    <div className='flowLayoutErrorOverlay'>
                        <Result
                            status='error'
                            title={t('layoutFailedTitle')}
                            subTitle={t('layoutFailedDesc')}
                            extra={retryLayout &&
                                <Button type='primary' onClick={retryLayout}>{t('retryLayout')}</Button>}
                        />
                    </div>}
            </div>
        </ConfigProvider>

        <FlowGraphContext value={ctx}>
            {children}
        </FlowGraphContext>
    </ReactFlowProvider>;

});



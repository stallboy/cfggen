import {Background, Controls, Edge, Node, NodeTypes, ReactFlow, ReactFlowProvider} from "@xyflow/react";
import {Entity} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";
import {memo, MouseEvent as ReactMouseEvent, ReactNode, useCallback, useMemo, useState} from "react";
import {FlowContextMenu, MenuItem, MenuStyle} from "./FlowContextMenu.tsx";
import {FlowNode} from "./FlowNode.tsx";
import {FlowGraphContext as FlowGraphContext1} from "./FlowGraphContext.ts";

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

export const FlowGraph = memo(function FlowGraph({children}: {
    children: ReactNode
}) {
    const [menuStyle, setMenuStyle] = useState<MenuStyle | undefined>(undefined);
    const [menuItems, setMenuItems] = useState<MenuItem[] | undefined>(undefined);

    const [paneMenu, setPaneMenu] = useState<MenuItem[]>([]);
    const [nodeMenuFunc, setNodeMenuFunc] = useState<NodeMenuFunc>();
    const [nodeDoubleClickFunc, setNodeDoubleClickFunc] = useState<NodeDoubleClickFunc>();

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

    const ctx = useMemo(() => {
        return {
            setPaneMenu,
            setNodeMenuFunc: thisSetNodeMenuFunc,
            setNodeDoubleClickFunc: thisSetNodeDoubleClickFunc,
        }
    }, [setPaneMenu, thisSetNodeMenuFunc, thisSetNodeDoubleClickFunc]);


    return <ReactFlowProvider>
        <ReactFlow
            defaultNodes={defaultNodes}
            defaultEdges={defaultEdges}
            nodeTypes={nodeTypes}
            minZoom={0.1}
            maxZoom={2}
            deleteKeyCode={null}
            // fitView
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

        <FlowGraphContext1 value={ctx}>
            {children}
        </FlowGraphContext1>
    </ReactFlowProvider>;

});



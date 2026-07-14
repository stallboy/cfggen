import {MenuItem} from "./FlowContextMenu.tsx";
import {createContext} from "react";
import type {NodeDoubleClickFunc, NodeMenuFunc} from "./FlowGraph.tsx";

export interface FlowGraphContextType {
    setPaneMenu: (menu: MenuItem[]) => void;
    setNodeMenuFunc: (func: NodeMenuFunc) => void;
    setNodeDoubleClickFunc: (func: NodeDoubleClickFunc) => void;
    // 布局失败反馈：消费端(useEntityToGraph)把 layout error 透传上来 + 登记 retry 回调，
    // FlowGraph 据此渲染 Result 覆盖层。undefined = 正常，清掉覆盖层。
    setLayoutError: (e: Error | undefined) => void;
    setRetryLayout: (fn: () => void) => void;
}

// 默认 undefined：<FlowGraph> 必须先渲染（它包裹 <Routes>，路由在 children 内靠 context 反向下发菜单）
// 才会经 value=ctx 提供真实实现。若在 FlowGraph 外误用，useContext 返回 undefined → 消费处 throw，
// 把误用变显式报错，而非旧 dummy noop 的静默失败。
export const FlowGraphContext = createContext<FlowGraphContextType | undefined>(undefined);

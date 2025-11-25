import {MenuItem} from "./FlowContextMenu.tsx";
import {createContext} from "react";
import {NodeDoubleClickFunc, NodeMenuFunc} from "./FlowGraph.tsx";

export interface FlowGraphContextType {
    setPaneMenu: (menu: MenuItem[]) => void;
    setNodeMenuFunc: (func: NodeMenuFunc) => void;
    setNodeDoubleClickFunc: (func: NodeDoubleClickFunc) => void;
}

function dummy() {
}

export const FlowGraphContext = createContext<FlowGraphContextType>({
    setPaneMenu: dummy,
    setNodeMenuFunc: dummy,
    setNodeDoubleClickFunc: dummy,
});
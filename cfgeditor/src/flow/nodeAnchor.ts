import type {NodeProps} from "@xyflow/react";
import type {EntityNode} from "./FlowGraph.tsx";

// 节点锚点：编辑命令（fold/unfold/moveUp/moveDown/delete/interface 切换/embedded 展开/funcAdd）
// 一律带 {id, 屏幕坐标}，供路由层定位被操作的节点。这里收口构造，避免各处重复字面量。
// positionAbsoluteX/Y 是节点在画布的绝对坐标（ReactFlow 提供），与视口缩放/平移无关。
export interface NodeAnchor {
    id: string;
    x: number;
    y: number;
}

export function nodeAnchor(nodeProps: NodeProps<EntityNode>): NodeAnchor {
    return {
        id: nodeProps.data.entity.id,
        x: nodeProps.positionAbsoluteX,
        y: nodeProps.positionAbsoluteY,
    };
}

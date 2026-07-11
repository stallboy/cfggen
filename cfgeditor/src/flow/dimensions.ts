import {Entity, isEditableEntity} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";

// 节点宽度在 xyflow 里同时驱动三件事，必须来自同一数字源：
//   1. ELK 边界框 —— calcWidthHeight 估算后喂给 elk.layout 当不可压缩边界框
//   2. CSS 渲染宽度 —— FlowNode 的 nodeStyle.width
//   3. Handle 绝对定位 —— EntityProperties/EntityForm 里 Handle 的 left 偏移
// 改 nodeShow.nodeWidth/editNodeWidth 时上述三处必须同步，故集中到此处。
//
// 注意：store.ts 里的 240/280 是持久化进 NodeShowType 的"默认值"（不同关注点），
// 且 oxlint 禁止 store→flow 导入，故不在此统一；此处 DEFAULT_* 是"nodeShow 字段缺失时的兜底"。

/** 非编辑态节点宽度兜底（nodeShow.nodeWidth 缺失时）。 */
export const DEFAULT_NODE_WIDTH = 240;
/** 编辑态节点宽度兜底（nodeShow.editNodeWidth 缺失时）。 */
export const DEFAULT_EDIT_NODE_WIDTH = 280;

/** 非编辑态（readonly/card）节点宽度。 */
export function getReadNodeWidth(nodeShow?: NodeShowType): number {
    return nodeShow?.nodeWidth ?? DEFAULT_NODE_WIDTH;
}

/** 编辑态节点宽度。 */
export function getEditNodeWidth(nodeShow?: NodeShowType): number {
    return nodeShow?.editNodeWidth ?? DEFAULT_EDIT_NODE_WIDTH;
}

/** 按 entity 是否可编辑取对应宽度（calcWidthHeight/FlowNode 共用）。nodeShow 由调用方传入（doc BR1）。 */
export function getNodeWidth(entity: Entity, nodeShow?: NodeShowType): number {
    return isEditableEntity(entity)
        ? getEditNodeWidth(nodeShow)
        : getReadNodeWidth(nodeShow);
}

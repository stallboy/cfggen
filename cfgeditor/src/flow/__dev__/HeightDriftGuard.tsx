import { useEffect } from "react";
import { useStore } from "@xyflow/react";
import { Entity } from "@/domain/entityModel";
import type { NodeShowType } from "@/domain/storageJson";
import { calcWidthHeight } from "../layout/calcWidthHeight.ts";

// 已从 FlowNode 移出并停用，现归档于 __dev__/（不在主路径装活代码）。
// 原在 FlowNode 内以 {import.meta.env.DEV && <HeightDriftGuard/>} 渲染，
// 但 dev-only 的 useStore(measured.height) 订阅在节点高度抖动时引发 re-render 风暴——实测禁用后 dev long task
// 总量 -45%、11.7→16.7s 后段周期 task 全消失。FlowNode 不再引用本文件。
//
// 恢复使用（见 docs/flow-refactor.md §5-A3 第3档：改为单次测量、不持续订阅 store）：
// FlowNode 加 `import {HeightDriftGuard} from "./__dev__/HeightDriftGuard.tsx"`，
// 并在节点 div 内放 `{import.meta.env.DEV && <HeightDriftGuard id={id} entity={entity} nodeShow={nodeShow} notes={notes} />}`。

// dev-only：记录已警告过 height drift 的节点 id，避免同一节点反复 console.warn 刷屏
const heightDriftWarned = new Set<string>();

// dev-only 实测对账护栏：节点挂载后 xyflow 用 ResizeObserver 回写 measured.height 到内部 store，
// 这里响应式读取并与 calcWidthHeight 估算比，偏差 >8px 且 >5% 时 warn（按 id 去重）。不重排不闪烁。
// 仅针对 height：width 两端同源（FlowNode 与 calcWidthHeight 都读 nodeShow.nodeWidth/editNodeWidth）不会漂。
// 注：NodeProps 在本版本无 measured 字段，故走 useStore 只读尺寸切片（doc §5 允许的 escape-hatch 场景）。
export function HeightDriftGuard({id, entity, nodeShow, notes}: {
    id: string;
    entity: Entity;
    nodeShow?: NodeShowType;
    notes?: Map<string, string>;
}) {
    const measuredHeight = useStore((s) => s.nodeLookup.get(id)?.measured?.height);
    useEffect(() => {
        if (measuredHeight === undefined) return;
        const [, est] = calcWidthHeight(entity, nodeShow, notes);
        const drift = Math.abs(measuredHeight - est);
        if (drift > 8 && drift / est > 0.05 && !heightDriftWarned.has(id)) {
            heightDriftWarned.add(id);
            console.warn(`[flow] node ${id} height drift: est=${est} measured=${measuredHeight} Δ=${drift}px`);
        }
    }, [id, entity, nodeShow, notes, measuredHeight]);
    return null;
}

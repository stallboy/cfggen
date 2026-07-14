// 视口（viewport）数学：xyflow 的 viewport 是一个线性变换参数（公开契约）
//   screenX = worldX * zoom + vp.x
//   screenY = worldY * zoom + vp.y
// 这些纯函数把 useEntityToGraph effect 里"手算视口"的逻辑抽出来，配不变量测试，
// 把"relayout 后锚点屏幕坐标不变"从注释提升为可执行契约。

import {Rect} from "@xyflow/react";
import {EditingObjectRes, EFitView} from "@/domain/entityModel";

export type Viewport = { x: number; y: number; zoom: number };
export type Point = { x: number; y: number };

/**
 * relayout 后让锚点的屏幕坐标保持不变（内部函数，由 pickViewportAction 调用）。
 *
 * 场景：ELK 重新布局后某个节点（锚点）的世界坐标由 anchorOld 变为 anchorNew，
 * 但要求它在屏幕上的位置不动（用户视角"图没跳"）。
 *
 * 求解：屏幕坐标 = 世界 * zoom + 平移。要使 (anchorNew*zoom + newVp) === (anchorOld*zoom + vp)，
 * 且 newVp.zoom === vp.zoom（缩放不变），解得：
 *   newVp.x = anchorOld.x * zoom - anchorNew.x * zoom + vp.x
 *   newVp.y = anchorOld.y * zoom - anchorNew.y * zoom + vp.y
 *
 * xyflow 没有提供"保持某点屏幕坐标不变"的高层原语（fitView 做不到），所以必须自算。
 */
function computeStableViewport(anchorOld: Point, anchorNew: Point, vp: Viewport): Viewport {
    return {
        zoom: vp.zoom,
        x: anchorOld.x * vp.zoom - anchorNew.x * vp.zoom + vp.x,
        y: anchorOld.y * vp.zoom - anchorNew.y * vp.zoom + vp.y,
    };
}

// ============================================================================
// 视口动作选择：把 useEntityToGraph effect 里「按 fitView 选视口动作」的分支抽纯函数。
// 行为与原 effect 逐分支等价：
//   - undefined / FitFull              → fitFull（全图适配，由 effect 调 fitView）
//   - FitId + 命中 id + 有 position    → fitId（算好新视口，由 effect 调 setViewport）
//   - FitId + id 不存在 / 无 position  → noop（删除节点等场景）
//   - NoChange                         → noop（undo/redo / 只读 / 固定页，不动视口）
// 副作用（fitView/setViewport 调用、setFitViewForPathname 回调）留在 effect；此处只决策 + 算数学。
// ============================================================================

export type ViewportAction =
    | { kind: 'fitFull' }
    | { kind: 'fitId'; viewport: Viewport }
    | { kind: 'noop' };

export function pickViewportAction(
    editingObjectRes: EditingObjectRes | undefined,
    id2RectMap: Map<string, Rect>,
    currentVp: Viewport,
    opts?: { prevId2RectMap?: Map<string, Rect> },   // 上一帧布局，仅 KeepStable 用
): ViewportAction {
    if (editingObjectRes === undefined || editingObjectRes.fitView === EFitView.FitFull) {
        return {kind: 'fitFull'};
    }
    if (editingObjectRes.fitView === EFitView.FitId && editingObjectRes.fitViewToIdPosition) {
        const {id, x, y} = editingObjectRes.fitViewToIdPosition;
        const nowXy = id2RectMap.get(id);
        if (nowXy !== undefined) {
            return {
                kind: 'fitId',
                viewport: computeStableViewport({x, y}, {x: nowXy.x, y: nowXy.y}, currentVp),
            };
        }
    }
    if (editingObjectRes.fitView === EFitView.KeepStable
        && editingObjectRes.fitViewToIdPosition && opts?.prevId2RectMap) {
        // undo/redo：anchorOld = 上一帧布局该 id 坐标（= undo 发起时锚点坐标，不用 position.x/y——那是操作发起时坐标，已过时），
        // anchorNew = 新布局同 id 坐标。锚点=被撤销操作的视觉焦点（delete 取父）。命中失败（锚点被删/无 prevMap）→ noop。
        const {id} = editingObjectRes.fitViewToIdPosition;
        const oldRect = opts.prevId2RectMap.get(id);
        const newRect = id2RectMap.get(id);
        if (oldRect && newRect) {
            return {
                kind: 'fitId',
                viewport: computeStableViewport({x: oldRect.x, y: oldRect.y}, {x: newRect.x, y: newRect.y}, currentVp),
            };
        }
    }
    return {kind: 'noop'};
}

// 视口（viewport）数学：xyflow 的 viewport 是一个线性变换参数（公开契约）
//   screenX = worldX * zoom + vp.x
//   screenY = worldY * zoom + vp.y
// 这些纯函数把 useEntityToGraph effect 里"手算视口"的逻辑抽出来，配不变量测试，
// 把"relayout 后锚点屏幕坐标不变"从注释提升为可执行契约。

export type Viewport = { x: number; y: number; zoom: number };
export type Point = { x: number; y: number };

/**
 * 世界坐标 → 屏幕坐标（线性变换）。
 * screenOf(world, vp) = world * zoom + vp.{x,y}
 */
export function screenOf(world: Point, vp: Viewport): Point {
    return {x: world.x * vp.zoom + vp.x, y: world.y * vp.zoom + vp.y};
}

/**
 * relayout 后让锚点的屏幕坐标保持不变。
 *
 * 场景：ELK 重新布局后某个节点（锚点）的世界坐标由 anchorOld 变为 anchorNew，
 * 但要求它在屏幕上的位置不动（用户视角"图没跳"）。
 *
 * 求解：要使 screenOf(anchorNew, newVp) === screenOf(anchorOld, vp)，
 * 且 newVp.zoom === vp.zoom（缩放不变），解得：
 *   newVp.x = anchorOld.x * zoom - anchorNew.x * zoom + vp.x
 *   newVp.y = anchorOld.y * zoom - anchorNew.y * zoom + vp.y
 *
 * xyflow 没有提供"保持某点屏幕坐标不变"的高层原语（fitView 做不到），所以必须自算。
 * 见 useEntityToGraph FitId 分支。
 */
export function computeStableViewport(anchorOld: Point, anchorNew: Point, vp: Viewport): Viewport {
    return {
        zoom: vp.zoom,
        x: anchorOld.x * vp.zoom - anchorNew.x * vp.zoom + vp.x,
        y: anchorOld.y * vp.zoom - anchorNew.y * vp.zoom + vp.y,
    };
}

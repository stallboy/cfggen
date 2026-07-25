import {EntityNode} from "../FlowGraph.tsx";

// 布局失败兜底：ELK 失败时把节点按网格铺开，避免全部塌叠在默认 (100,100) 成一摞不可读。
// 非精确布局——只求"看得见、不重叠成一摞"，retry 成功后由正常 applyRectToNodes 路径校正。
// 网格单元略大于最大节点（edit 280 宽 + padding），保证横向不贴边；纵向高度按典型行数留余量。
const FALLBACK_CELL_W = 320;
const FALLBACK_CELL_H = 260;
const FALLBACK_COLS = 5;
const FALLBACK_ORIGIN = 80;

export function spreadFallbackNodes(nodes: EntityNode[]): EntityNode[] {
    return nodes.map((n, i) => ({
        ...n,
        position: {
            x: FALLBACK_ORIGIN + (i % FALLBACK_COLS) * FALLBACK_CELL_W,
            y: FALLBACK_ORIGIN + Math.floor(i / FALLBACK_COLS) * FALLBACK_CELL_H,
        },
    }));
}

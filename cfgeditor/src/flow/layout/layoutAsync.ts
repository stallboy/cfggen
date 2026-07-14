import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs/lib/elk-api.js';
// 用 Web Worker 跑布局算法，避免几十~几百节点时分层/MR 树计算阻塞主线程（O1）
// workerUrl 指向 elkjs 自带的 worker 脚本（elk-worker.min.js），由 ELK 内部 new Worker 加载
import elkWorkerUrl from 'elkjs/lib/elk-worker.min.js?url';
import {EntityEdge, EntityNode} from "../FlowGraph.tsx";
import {Rect, XYPosition} from "@xyflow/react";
import {calcWidthHeight} from "./calcWidthHeight.ts";
import {NodePlacementStrategyType, NodeShowType} from "@/domain/storageJson";

// 模块级单例（worker 模式）：layout 在 Web Worker 跑，每次布局复用同一 worker
const elk = new ELK({workerUrl: elkWorkerUrl});


function nodeToLayoutChild(node: EntityNode, id2RectMap: Map<string, Rect>): ElkNode {
    const {entity, nodeShow, notes} = node.data;
    const [width, height] = calcWidthHeight(entity, nodeShow, notes);
    id2RectMap.set(node.id, {x: 0, y: 0, width, height})
    return {id: node.id, width, height};
}

function edgeToLayoutEdge(edge: EntityEdge): ElkExtendedEdge {
    return {
        id: edge.id,
        sources: [edge.source],
        targets: [edge.target]
    }
}


function toPositionMap(map: Map<string, XYPosition>, children: ElkNode[]) {
    for (const {id, x, y, children: subChildren} of children) {
        const rect = map.get(id);
        if (x != undefined && y != undefined && rect) {
            rect.x = x;
            rect.y = y;
        }
        if (subChildren) {
            toPositionMap(map, subChildren);
        }
    }
}

function allPositionXYOk(nodes: EntityNode[], map: Map<string, XYPosition>) {
    if (nodes.length != map.size) {
        return false;
    }
    for (const node of nodes) {
        const newPos = map.get(node.id);
        if (!newPos) {
            return false;
        }
    }
    return true;
}


/**
 * layoutAsync 的失败语义：失败一律 throw LayoutError，绝不 resolve 为 undefined。
 *
 * 原因：react-query 把 resolve 的 undefined 当成"成功但无数据"（isSuccess=true, data=undefined），
 * 既不进 retry/error 通道，也彻底打破下游 `if (data)` 守卫——表现为偶发空/旧图且零反馈。
 * throw 后 react-query 正确进入 error 态（会 retry），下游也能据 error 兜底。见 useEntityToGraph。
 */
export class LayoutError extends Error {
    readonly code: 'aborted' | 'no_children' | 'dropped_nodes';
    constructor(code: LayoutError['code'], message: string) {
        super(message);
        this.code = code;
        this.name = 'LayoutError';
    }
}

export async function layoutAsync(nodes: EntityNode[], edges: EntityEdge[], layoutStrategy: NodePlacementStrategyType, nodeShow?: NodeShowType, signal?: AbortSignal) {
    // elk 为模块级单例（见文件顶部），worker 模式下 layout 在 Web Worker 跑
    const id2RectMap = new Map<string, Rect>();

    // Use configurable spacing values with defaults
    const mrtreeSpacing = nodeShow?.mrtreeSpacing ?? 100;
    const layeredSpacing = nodeShow?.layeredSpacing ?? 60;
    const layeredNodeSpacing = nodeShow?.layeredNodeSpacing ?? 80;

    let options;
    if (layoutStrategy == 'mrtree') {
        options = {
            'elk.algorithm': 'mrtree',
            'elk.direction': 'RIGHT',
            'elk.edgeRouting': 'POLYLINE',
            'elk.spacing.nodeNode': mrtreeSpacing.toString(),
        }
    } else {
        options = {
            'elk.algorithm': 'layered',
            'elk.direction': 'RIGHT',
            'elk.edgeRouting': 'POLYLINE',
            'elk.layered.spacing.nodeNodeBetweenLayers': layeredNodeSpacing.toString(),
            'elk.spacing.nodeNode': layeredSpacing.toString(),
            'elk.layered.nodePlacement.strategy': layoutStrategy,
            'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
            'elk.layered.crossingMinimization.forceNodeModelOrder': 'true',
        };
    }

    const graph: ElkNode = {
        id: 'root',
        layoutOptions: options,
        children: nodes.map((n) => nodeToLayoutChild(n, id2RectMap)),
        edges: edges.map(edgeToLayoutEdge),
    };

    // 调用前已被 abort（react-query 在 query 变 stale/inactive 时 abort signal）→ 直接放弃
    if (signal?.aborted) throw new LayoutError('aborted', 'layout aborted before elk.layout');

    const {children} = await elk.layout(graph);

    // elk.layout 异步期间被 abort → 不再写回结果
    if (signal?.aborted) throw new LayoutError('aborted', 'layout aborted after elk.layout');
    if (!children) throw new LayoutError('no_children', 'elk.layout returned no children');

    toPositionMap(id2RectMap, children);

    // 校验 ELK 返回的位置 map 覆盖了所有输入 node：map 由 nodeToLayoutChild 按 node.id 预填，
    // 正常 map.size===nodes.length；出现重复 id（Map 去重后变小）等情况时校验失败 → throw 交 react-query retry。
    if (!allPositionXYOk(nodes, id2RectMap)) {
        throw new LayoutError('dropped_nodes', `layout dropped some nodes (expected ${nodes.length}, got ${id2RectMap.size})`);
    }
    return id2RectMap;
}



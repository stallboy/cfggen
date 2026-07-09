import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
// 用 Web Worker 跑布局算法，避免几十~几百节点时分层/MR 树计算阻塞主线程（O1）
// workerUrl 指向 elkjs 自带的 worker 脚本（elk-worker.min.js），由 ELK 内部 new Worker 加载
import elkWorkerUrl from 'elkjs/lib/elk-worker.min.js?url';
import {EntityEdge, EntityNode} from "./FlowGraph.tsx";
import {Rect, XYPosition} from "@xyflow/react";
import {calcWidthHeight} from "./calcWidthHeight.ts";
import {NodePlacementStrategyType, NodeShowType} from "../store/storageJson.ts";

// 模块级单例（worker 模式）：layout 在 Web Worker 跑，每次布局复用同一 worker
const elk = new ELK({workerUrl: elkWorkerUrl});


function nodeToLayoutChild(node: EntityNode, id2RectMap: Map<string, Rect>): ElkNode {
    const [width, height] = calcWidthHeight(node.data.entity);
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


export async function layoutAsync(nodes: EntityNode[], edges: EntityEdge[], layoutStrategy: NodePlacementStrategyType, nodeShow?: NodeShowType) {
    // elk 为模块级单例（见文件顶部），worker 模式下 layout 在 Web Worker 跑
    // console.log('layout', nodes.length, nodes, edges);
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
    // console.log(graph);

    const {children} = await elk.layout(graph);
    if (children) {
        toPositionMap(id2RectMap, children);

        // 重新取nodes，因为此时nodes可能跟异步layout请求开始时的nodes不同，所以要检验下是不是allPositionXYOk
        if (!allPositionXYOk(nodes, id2RectMap)) {
            console.log('layout ignored', nodes);
            return;
        }
        return id2RectMap;
    } else {
        console.log('layout children null');
    }

}



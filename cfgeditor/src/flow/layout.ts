import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
import {EntityEdge, EntityNode} from "./FlowGraph.tsx";
import {Rect, XYPosition} from "@xyflow/react";
import {calcWidthHeight} from "./FlowNode.tsx";
import {NodeShowType} from "../io/localStoreJson.ts";


function nodeToLayoutChild(node: EntityNode, nodeShow: NodeShowType, id2PosMap: Map<string, Rect>): ElkNode {
    const [width, height] = calcWidthHeight(node.data, nodeShow);
    id2PosMap.set(node.id, {x: 0, y: 0, width, height})
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
    for (let {id, x, y, children: subChildren} of children) {
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
    for (let node of nodes) {
        const newPos = map.get(node.id);
        if (!newPos) {
            return false;
        }
    }
    return true;
}


export async function asyncLayout(nodes: EntityNode[], edges: EntityEdge[], nodeShow: NodeShowType) {
    const elk = new ELK();
    // console.log('layout', nodes.length, nodes, edges);

    const id2PosMap = new Map<string, Rect>();

    const defaultOptions = {
        'elk.algorithm': 'layered',
        'elk.direction': 'RIGHT',
        'elk.edgeRouting': 'POLYLINE',
        'elk.layered.spacing.nodeNodeBetweenLayers': '80',
        'elk.spacing.nodeNode': '60',
        'elk.layered.nodePlacement.strategy': 'SIMPLE',
        'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
        'elk.layered.crossingMinimization.forceNodeModelOrder': 'true',
    };

    const graph: ElkNode = {
        id: 'root',
        layoutOptions: defaultOptions,
        children: nodes.map((n) => nodeToLayoutChild(n, nodeShow, id2PosMap)),
        edges: edges.map(edgeToLayoutEdge),
    };

    // console.log(graph);

    const {id, children} = await elk.layout(graph);


    // console.log('layout res', id, children);
    // if (id != pathname) {
    //     console.log('layout ignore other', id, pathname);
    // } else
    if (children) {
        // console.log('layout ok', id)
        toPositionMap(id2PosMap, children);

        // 重新取nodes，因为此时nodes可能跟异步layout请求开始时的nodes不同，所以要检验下是不是allPositionXYOk
        if (!allPositionXYOk(nodes, id2PosMap)) {
            console.log('layout ignored, nodes may changed', id, nodes);
            return;
        }
        // const edges = flowInstance.getEdges();
        // console.log('before', nodes);
        return id2PosMap;
    } else {
        console.log('layout children null');
    }


}



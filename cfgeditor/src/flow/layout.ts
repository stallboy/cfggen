import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
import {EntityEdge, EntityNode} from "./FlowGraph.tsx";
import {XYPosition} from "@xyflow/react";
import {calcWidthHeight} from "./FlowNode.tsx";
import {NodeShowType} from "../io/localStoreJson.ts";


function nodeToLayoutChild(node: EntityNode, nodeShow:NodeShowType): ElkNode {
    const [width, height] = calcWidthHeight(node.data, nodeShow);
    node.width = width;
    node.height = height;
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
        if (x != undefined && y != undefined) {
            map.set(id, {x, y});
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
    // console.log('layout', pathname, nodes, edges);

    // console.log("layout calc", pathname);
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
        children: nodes.map( (n) => nodeToLayoutChild(n, nodeShow)),
        edges: edges.map(edgeToLayoutEdge),
    };

    // console.log(graph);

    const flowNodeMap = new Map<string, EntityNode>();
    nodes.forEach(n => flowNodeMap.set(n.id, n));

    const {id, children} = await elk.layout(graph);


    // console.log('layout res', id, children);
    // if (id != pathname) {
    //     console.log('layout ignore other', id, pathname);
    // } else
    if (children) {
        // console.log('layout ok', id)
        const map = new Map<string, XYPosition>();
        toPositionMap(map, children);

        // 重新取nodes，因为此时nodes可能跟异步layout请求开始时的nodes不同，所以要检验下是不是allPositionXYOk
        if (!allPositionXYOk(nodes, map)) {
            console.log('layout ignored, nodes may changed', id, nodes);
            return;
        }
        // const edges = flowInstance.getEdges();
        // console.log('before', nodes);
        const newNodes = nodes.map(n => {
            const newPos = map.get(n.id);
            if (newPos) {
                // if (n.computed) {
                //     n.computed.positionAbsolute = undefined;
                // }
                // n.position = newPos;

                return {
                    ...n,
                    position: newPos,
                    // computed: {
                    //     ...n.computed,
                    //     positionAbsolute: undefined
                    // }
                    //去掉positionAbsolute，要不然getNodesBounds返回用这个
                }
            } else {
                console.log('not found', n, map)
                return n;
            }
            // n.style = undefined;
        })
        // edges.forEach(e => {
        //     e.style = {...e.style, visibility: undefined};
        // })
        // flowInstance.setEdges(edges);

        // console.log('layout res nodes', newNodes, edges);

        return newNodes;
    } else {
        console.log('layout children null');
    }


}



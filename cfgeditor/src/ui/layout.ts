import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
import {FlowEdge, FlowNode} from "./FlowEntityNode.tsx";
import {ReactFlowInstance, XYPosition} from "reactflow";

const elk = new ELK();

function nodeToLayoutChild(node: FlowNode): ElkNode {
    return {
        id: node.id,
        width: node.width!,
        height: node.height!,
    };
}

function edgeToLayoutEdge(edge: FlowEdge): ElkExtendedEdge {
    return {
        id: edge.id,
        sources: [edge.source],
        targets: [edge.target]
    }
}


function toPositonMap(map: Map<string, XYPosition>, children: ElkNode[]) {
    for (let {id, x, y, children: subChildren} of children) {
        if (x != undefined && y != undefined) {
            map.set(id, {x, y});
        }
        if (subChildren) {
            toPositonMap(map, subChildren);
        }
    }
}

export function layout(flowInstance: ReactFlowInstance) {
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

    const nodes = flowInstance.getNodes();
    const edges = flowInstance.getEdges();

    const graph: ElkNode = {
        id: 'root',
        layoutOptions: defaultOptions,
        children: nodes.map(nodeToLayoutChild),
        edges: edges.map(edgeToLayoutEdge),
    };

    const flowNodeMap = new Map<string, FlowNode>();
    nodes.forEach(n => flowNodeMap.set(n.id, n));

    elk.layout(graph).then(({children}) => {
        if (children && edges) {
            const map = new Map<string, XYPosition>();
            toPositonMap(map, children);
            const newNodes: FlowNode[] = nodes.map(n => {
                const newPos = map.get(n.id);
                return {...n, ...(newPos ? {position: newPos} : {}), style: undefined};
            })

            const newEdges: FlowEdge[] = edges.map(e => {
                return {...e, style: {...e.style, visibility: undefined}};
            })

            flowInstance.setNodes(newNodes);
            flowInstance.setEdges(newEdges);

            setTimeout(() => {
                flowInstance.fitView({duration: 300});
            })

        }
    });
}



import Dagre from '@dagrejs/dagre';
import {ReactFlowInstance} from '@xyflow/react';
import {EntityEdge, EntityNode} from "./FlowGraph.tsx";

const g = new Dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}));

const getLayoutedElements = (nodes: EntityNode[], edges: EntityEdge[]) => {
    g.setGraph({rankdir: 'LR', align: 'UL', ranker: 'tight-tree'});

    edges.forEach((edge) => g.setEdge(edge.source, edge.target));
    nodes.forEach((node) => g.setNode(node.id, {
        id: node.id,
        width: node.width!,
        height: node.height!,
    }));

    Dagre.layout(g);

    return {
        nodes: nodes.map((node) => {
            const {x, y} = g.node(node.id);
            return {...node, position: {x, y}};
        }),
        edges,
    };
};

export function syncLayout(flowInstance: ReactFlowInstance) {
    const nodes = flowInstance.getNodes();
    const edges = flowInstance.getEdges();

    const layouted = getLayoutedElements(nodes, edges);

    flowInstance.setNodes([...layouted.nodes]);
    flowInstance.setEdges([...layouted.edges]);
    flowInstance.fitView({
        minZoom: 0.3,
        maxZoom: 1
    })

}
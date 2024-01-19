import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
import {FlowEdge, FlowNode} from "./FlowGraph.tsx";
import {ReactFlowInstance, Viewport, XYPosition} from "reactflow";
import {QueryClient} from "@tanstack/react-query";

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

export function layout(flowInstance: ReactFlowInstance, pathname: string, queryClient: QueryClient) {
    queryClient.fetchQuery({
        queryKey: ['layout', pathname],
        queryFn: async () => {
            // console.log("layout", pathname);
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

            return await elk.layout(graph);
        }
    }).then(({children}) => {
        if (children) {
            const map = new Map<string, XYPosition>();
            toPositonMap(map, children);

            const nodes = flowInstance.getNodes();
            const edges = flowInstance.getEdges();
            // change in place，maybe beacause of the zustand store
            nodes.forEach(n => {
                const newPos = map.get(n.id);
                if (newPos) {
                    n.position = newPos;
                }
                n.style = undefined;
            })
            edges.forEach(e => {
                e.style = {...e.style, visibility: undefined};
            })
            flowInstance.setNodes(nodes);
            flowInstance.setEdges(edges);
            const viewport = queryClient.getQueryData<Viewport>(['viewport', pathname]);
            if (viewport) {
                flowInstance.setViewport(viewport);
            } else {
                setTimeout(() => {
                    // console.log("fitView", pathname);
                    flowInstance.fitView();
                    const viewport = flowInstance.getViewport();
                    queryClient.setQueryData(['viewport', pathname], viewport);
                })
            }
        }
    });
}



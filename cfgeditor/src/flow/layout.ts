import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
import {EntityEdge, EntityNode} from "./FlowGraph.tsx";
import {getNodesBounds, getViewportForBounds, ReactFlowInstance, XYPosition} from "reactflow";
import {QueryClient} from "@tanstack/react-query";


const elk = new ELK();

function nodeToLayoutChild(node: EntityNode): ElkNode {
    let width = node.width
    if (!width) {
        width = 300;
        console.log('width', width);
    }

    let height = node.height
    if (!height) {
        height = 300;
        console.log('height', height);
    }

    return {id: node.id, width, height,};
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

function allWidthHeightOk(nodes: EntityNode[]) {
    for (let node of nodes) {
        if (!node.width || !node.height) {
            return false;
        }
    }
    return true;
}

export function layout(flowInstance: ReactFlowInstance, width: number, height: number, pathname: string, queryClient: QueryClient) {
    const nodes = flowInstance.getNodes();
    if (!allWidthHeightOk(nodes)) {
        // console.log('layout ignore')
        return;
    }
    const edges = flowInstance.getEdges();
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


            const graph: ElkNode = {
                id: 'root',
                layoutOptions: defaultOptions,
                children: nodes.map(nodeToLayoutChild),
                edges: edges.map(edgeToLayoutEdge),
            };

            const flowNodeMap = new Map<string, EntityNode>();
            nodes.forEach(n => flowNodeMap.set(n.id, n));

            return await elk.layout(graph);
        },
    }).then(({children}) => {
        if (children) {
            // console.log('layout ok', children.length)
            const map = new Map<string, XYPosition>();
            toPositionMap(map, children);

            const nodes = flowInstance.getNodes();
            // const edges = flowInstance.getEdges();
            // console.log('before', nodes);
            // change in place，maybe because of the zustand store
            const newNodes = nodes.map(n => {
                const newPos = map.get(n.id);
                if (newPos) {
                    return {
                        //去掉positionAbsolute，要不然getNodesBounds返回用这个
                        id: n.id,
                        data: n.data,
                        position: newPos,
                        width: n.width,
                        height: n.height,
                        type: n.type
                    }
                } else {
                    return n;
                }
                // n.style = undefined;
            })
            // edges.forEach(e => {
            //     e.style = {...e.style, visibility: undefined};
            // })
            flowInstance.setNodes(newNodes);
            // flowInstance.setEdges(edges);
            const bounds = getNodesBounds(newNodes);

            const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1);
            flowInstance.setViewport(viewportForBounds);
            // console.log('set viewport', newNodes, bounds, viewportForBounds);
        } else {
            console.log('children null');
        }
    });
}



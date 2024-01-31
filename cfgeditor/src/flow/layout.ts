import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
import {EntityEdge, EntityNode} from "./FlowGraph.tsx";
import {getNodesBounds, getViewportForBounds, ReactFlowInstance, XYPosition} from "@xyflow/react";
import {MutableRefObject} from "react";
import {queryClient} from "../main.tsx";


const elk = new ELK();

function nodeToLayoutChild(node: EntityNode): ElkNode {
    let width = node.computed?.width
    if (!width) {
        width = 300;
        console.log('width', width);
    }

    let height = node.computed?.height
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
        if (!node.computed?.width || !node.computed?.height) {
            return false;
        }
    }
    return true;
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


async function asyncLayout(nodes: EntityNode[], edges: EntityEdge[],
                           oldPathnameRef: MutableRefObject<string | null>, pathname: string) {
    if (oldPathnameRef.current) {
        await queryClient.cancelQueries({queryKey: ['layout', oldPathnameRef.current]})
    }
    oldPathnameRef.current = pathname;
    // console.log('layout req', pathname);

    return await queryClient.fetchQuery({
        queryKey: ['layout', pathname],
        queryFn: async () => {
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
                id: pathname,
                layoutOptions: defaultOptions,
                children: nodes.map(nodeToLayoutChild),
                edges: edges.map(edgeToLayoutEdge),
            };

            const flowNodeMap = new Map<string, EntityNode>();
            nodes.forEach(n => flowNodeMap.set(n.id, n));

            return await elk.layout(graph);
        },
        staleTime: 1000 * 60 * 5
    })
}


export function layout(flowInstance: ReactFlowInstance, oldPathnameRef: MutableRefObject<string | null>,
                       width: number, height: number, pathname: string) {
    const nodes = flowInstance.getNodes();
    if (!allWidthHeightOk(nodes)) {
        console.log('layout ignore')
        return;
    }
    const edges = flowInstance.getEdges();

    console.log('layout', pathname, nodes, edges);
    asyncLayout(nodes, edges, oldPathnameRef, pathname).then(({id, children}) => {
        oldPathnameRef.current = null;
        // console.log('layout res', id, children);
        // if (id != pathname) {
        //     console.log('layout ignore other', id, pathname);
        // } else
        if (children) {
            // console.log('layout ok', id)
            const map = new Map<string, XYPosition>();
            toPositionMap(map, children);

            // 重新取nodes，因为此时nodes可能跟异步layout请求开始时的nodes不同，所以要检验下是不是allPositionXYOk
            const nodes = flowInstance.getNodes();
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
                        computed: {
                            ...n.computed,
                            positionAbsolute: undefined
                        }
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
            flowInstance.setNodes(newNodes);
            // flowInstance.setEdges(edges);

            console.log('layout res set nodes', nodes, edges);
            const bounds = getNodesBounds(nodes);

            const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1, 0);
            flowInstance.setViewport(viewportForBounds);
        } else {
            console.log('layout children null');
        }
    }).catch((reason: any) => {
        if (typeof reason == 'object' && 'revert' in reason) {
            //CancelledError是正常的
        } else {
            console.log('layout err', reason, nodes, edges);
        }
    });
}



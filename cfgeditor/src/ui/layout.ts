
// import ELK, {ElkNode, ElkExtendedEdge} from 'elkjs';
// const elk = new ELK();

// function layout(nodes: FlowNode[], edges: Edge[]) {
//     const defaultOptions = {
//         'elk.algorithm': 'layered',
//         'elk.layered.spacing.nodeNodeBetweenLayers': '100',
//         'elk.spacing.nodeNode': '80',
//         'elk.direction': 'RIGHT',
//     };
//
//     const graph: ElkNode = {
//         id: 'root',
//         layoutOptions: defaultOptions,
//         children: nodes as ElkNode[],
//         edges: edges as unknown as ElkExtendedEdge[],
//     };
//
//
//     elk.layout(graph).then(({children}) => {
//         // By mutating the children in-place we saves ourselves from creating a
//         // needless copy of the nodes array.
//         if (children) {
//             children.forEach((node: ElkNode) => {
//                 (node as FlowNode).position = {x: node.x!, y: node.y!};
//
//                 // console.log(node.id, node.x, node.y, "--", node.width, node.height);
//             });
//
//             // setNodes(children as FlowNode[]);
//             // window.requestAnimationFrame(() => {
//             //     fitView();
//             // });
//         }
//     });
// }

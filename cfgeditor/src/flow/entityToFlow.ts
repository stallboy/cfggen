import {Entity, EntityBaseField, EntityEdgeType, EntityGraph} from "./entityModel.ts";
import {edgeStorkColor} from "./colors.ts";
import {FlowEdge, FlowNode} from "./FlowGraph.tsx";

function findField(entity: Entity, name: string) {
    let fields: EntityBaseField[] | undefined = entity.fields;
    if (!fields && entity.edit) {
        fields = entity.edit.editFields;
    }
    return fields && fields.find(f => f.name == name);
}

export function fillHandles(entityMap: Map<string, Entity>) {
    for (let entity of entityMap.values()) {
        for (let {sourceHandle, target, targetHandle} of entity.sourceEdges) {
            if (sourceHandle == '@out') {
                entity.handleOut = true;
            } else {
                let field = findField(entity, sourceHandle);
                if (field) {
                    field.handleOut = true;
                } else {
                    console.log(sourceHandle + " handle not found for", entity);
                }
            }

            let targetEntity = entityMap.get(target);
            if (targetEntity) {
                if (targetHandle == '@in') {
                    targetEntity.handleIn = true;
                } else if (targetHandle.startsWith('@in_')) {
                    let targetField = findField(targetEntity, targetHandle.substring(4));
                    if (targetField) {
                        targetField.handleIn = true;
                    } else {
                        console.log(targetHandle + " handle not found for", targetEntity);
                    }
                } else {
                    console.error(targetHandle + ' not found for', targetEntity);
                }
            }
        }
    }
}

export function convertNodeAndEdges(graph: EntityGraph) {
    const nodes: FlowNode[] = []
    const edges: FlowEdge[] = []

    let ei = 1;
    for (let entity of graph.entityMap.values()) {
        entity.query = graph.query;
        entity.nodeShow = graph.nodeShow;

        nodes.push({
            id: entity.id,
            data: entity,
            type: 'node',
            position: {x: 100, y: 100},
            style: {visibility: 'hidden'},
        })
        for (let edge of entity.sourceEdges) {
            let fe: FlowEdge = {
                id: '' + (ei++),
                source: entity.id,
                sourceHandle: edge.sourceHandle,
                target: edge.target,
                targetHandle: edge.targetHandle,

                type: 'simplebezier',
                style: {stroke: edgeStorkColor, visibility: 'hidden'},
            }

            if (edge.type == EntityEdgeType.Ref) {
                fe.animated = true;
            }
            edges.push(fe);
        }
    }
    return {nodes, edges};
}

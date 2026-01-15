import {Entity, DisplayField, EntityEditField, isReadOnlyEntity, isEditableEntity, EntityEdgeType, EntityGraph} from "./entityModel.ts";
import {getEdgeColor} from "./colors.ts";
import {EntityEdge, EntityNode} from "./FlowGraph.tsx";

function findField(entity: Entity, name: string): DisplayField | EntityEditField | undefined {
    if (isReadOnlyEntity(entity)) {
        return entity.fields.find(f => f.name === name);
    }

    if (isEditableEntity(entity)) {
        const field = entity.edit.fields.find(f => f.name === name);
        if (field) return field;

        // 检查 interface 的 implFields
        for (const editField of entity.edit.fields) {
            if (editField.type === 'interface') {
                const implField = editField.implFields.find(f => f.name === name);
                if (implField) return implField;
            }
        }
    }

    return undefined;
}

export function fillHandles(entityMap: Map<string, Entity>) {
    for (const entity of entityMap.values()) {
        for (const {sourceHandle, target, targetHandle} of entity.sourceEdges) {
            if (sourceHandle == '@out') {
                entity.handleOut = true;
            } else {
                const field = findField(entity, sourceHandle);
                if (field) {
                    field.handleOut = true;
                } else {
                    console.log(sourceHandle + " handle not found for", entity);
                }
            }

            const targetEntity = entityMap.get(target);
            if (targetEntity) {
                if (targetHandle == '@in') {
                    targetEntity.handleIn = true;
                } else if (targetHandle.startsWith('@in_')) {
                    const targetField = findField(targetEntity, targetHandle.substring(4));
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

export function convertNodeAndEdges({entityMap, sharedSetting}: EntityGraph): {
    nodes: EntityNode[],
    edges: EntityEdge[]
} {
    const nodes: EntityNode[] = []
    const edges: EntityEdge[] = []

    let ei = 1;
    const edgeColor = getEdgeColor(sharedSetting?.nodeShow);

    for (const entity of entityMap.values()) {
        entity.sharedSetting = sharedSetting;

        nodes.push({
            id: entity.id,
            data: {entity: entity},
            type: 'node',
            position: {x: 100, y: 100},
        })
        for (const edge of entity.sourceEdges) {
            const fe: EntityEdge = {
                id: `${entity.id}_${edge.target}_${ei++}`,
                source: entity.id,
                sourceHandle: edge.sourceHandle,
                target: edge.target,
                targetHandle: edge.targetHandle,

                type: 'simplebezier',
                style: {
                    stroke: edgeColor,
                },
            }

            if (edge.type == EntityEdgeType.Ref) {
                fe.animated = true;
            }
            edges.push(fe);
        }
    }
    return {nodes, edges};
}

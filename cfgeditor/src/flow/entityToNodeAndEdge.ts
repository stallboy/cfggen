import {Entity, EntityBaseField, EntityEdgeType, EntityGraph} from "./entityModel.ts";
import {edgeStorkColor} from "./colors.ts";
import {EntityEdge, EntityNode} from "./FlowGraph.tsx";

function findField({fields, edit}: Entity, name: string) {
    let fs: EntityBaseField[] | undefined = fields;
    if (!fs && edit) {
        fs = edit.editFields;
    }
    const eq = ((f: EntityBaseField) => f.name == name);
    const f = fs && fs.find(eq);
    if (f) {
        return f;
    }

    if (edit) {
        for (const {implFields} of edit.editFields) {
            const f = implFields && implFields.find(eq);
            if (f) {
                return f;
            }
        }
    }
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
                    stroke: edgeStorkColor,
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

import {Entity, DisplayField, EntityEditField, isReadOnlyEntity, isEditableEntity, EntityEdgeType} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";
import {getEdgeColor} from "./colors.ts";
import {EntityEdge, EntityNode} from "../FlowGraph.tsx";
import {devLog, devError} from "../devLog.ts";

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
                    devLog(sourceHandle + " handle not found for", entity);
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
                        devLog(targetHandle + " handle not found for", targetEntity);
                    }
                } else {
                    devError(targetHandle + ' not found for', targetEntity);
                }
            }
        }
    }
}

/**
 * 把 entityMap 转成 xyflow 的 nodes/edges。
 *
 * nodeShow/notes 是呈现层数据，写入 node.data（EntityNodeData）供 FlowNode 及布局估算读取，
 * **不再盖章到 entity.sharedSetting**——entity 保持纯 domain、不可变 memo-safe。
 * query 不在此处下发：它无 per-graph override，渲染组件各自 useMyStore() 订阅。
 */
export function convertNodeAndEdges({entityMap, nodeShow, notes}: {
    entityMap: Map<string, Entity>;
    nodeShow?: NodeShowType;
    notes?: Map<string, string>;
}): {
    nodes: EntityNode[],
    edges: EntityEdge[]
} {
    const nodes: EntityNode[] = []
    const edges: EntityEdge[] = []

    let ei = 1;
    const edgeColor = getEdgeColor(nodeShow);

    for (const entity of entityMap.values()) {
        nodes.push({
            id: entity.id,
            data: {entity, nodeShow, notes},
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

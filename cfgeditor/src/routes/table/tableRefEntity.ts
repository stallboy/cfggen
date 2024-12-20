import {SItem, STable} from "./schemaModel.ts";
import {Entity, EntityEdgeType, EntityType} from "../../flow/entityModel.ts";
import {Schema} from "./schemaUtil.tsx";

function createEntity(item: SItem, id: string, entityType: EntityType = EntityType.Normal): Entity {
    return {
        id: id,
        label: item.name,
        sourceEdges: [],
        entityType: entityType,
        userData: item,
    };
}

export function includeRefTables(entityMap: Map<string, Entity>, curTable: STable, schema: Schema,
                                 refIn: boolean, maxOutDepth: number, maxNode: number) {

    const curEntity = createEntity(curTable, curTable.name);
    entityMap.set(curEntity.id, curEntity);


    if (refIn && curTable.refInTables) {

        for (const ref of curTable.refInTables) {
            let refInEntity = entityMap.get(ref);
            if (refInEntity) {
                continue;
            }

            const refInTable = schema.getSTable(ref);
            if (!refInTable) {
                console.log(ref + " not found!")
                continue; // 不该发生
            }

            refInEntity = createEntity(refInTable, ref, EntityType.RefIn);
            entityMap.set(ref, refInEntity);
            refInEntity.sourceEdges.push({
                sourceHandle: "@out",
                target: curTable.name,
                targetHandle: "@in",
                type: EntityEdgeType.Ref,
            })

            if (entityMap.size > maxNode / 2) {
                break;
            }
        }
    }


    let frontier: SItem[] = [curTable];
    let entityFrontier: Entity[] = [curEntity];
    let depth = 1;
    while (depth <= maxOutDepth) {

        const newFrontier: SItem[] = [];
        const newEntityFrontier: Entity[] = [];

        const refTableNames = schema.getAllRefTablesByItems(frontier);
        for (const ref of refTableNames) {
            let refEntity = entityMap.get(ref);
            if (refEntity) {
                continue;
            }

            const refTable = schema.getSTable(ref);
            if (!refTable) {
                console.log(ref + " not found!")
                continue; // 不该发生
            }

            const entityType = depth == 1 ? EntityType.Ref : EntityType.Ref2;
            refEntity = createEntity(refTable, ref, entityType);
            entityMap.set(ref, refEntity);

            newFrontier.push(refTable);
            newEntityFrontier.push(refEntity);

            if (entityMap.size > maxNode) {
                break;
            }
        }

        for (const oldEntity of entityFrontier) {
            const item = oldEntity.userData as SItem;
            const directRefs = schema.getAllRefTablesByItem(item);
            for (const ref of directRefs) {
                if (entityMap.has(ref)) {
                    oldEntity.sourceEdges.push({
                        sourceHandle: "@out",
                        target: ref,
                        targetHandle: "@in",
                        type: EntityEdgeType.Ref,

                    })
                }
            }
        }

        frontier = newFrontier;
        entityFrontier = newEntityFrontier;
        depth++;

        if (entityMap.size > maxNode) {
            break;
        }
    }
}

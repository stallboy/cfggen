import {SItem, STable} from "@/api/schemaModel.ts";
import {CardEntity, Entity, EntityEdgeType, EntityType} from "@/domain/entityModel.ts";
import {Schema} from "@/domain/schema.ts";
import {HANDLE_IN, HANDLE_OUT} from "@/domain/handleIds.ts";;

function createEntity(item: SItem, id: string, entityType: EntityType = EntityType.Normal): CardEntity<SItem> {
    return {
        id: id,
        label: item.name,
        type: 'card',
        brief: {
            value: '',
        },
        sourceEdges: [],
        entityType: entityType,
        userData: item,
    };
}

function addRefEdgesToExisting(entities: CardEntity<SItem>[], entityMap: Map<string, Entity>, schema: Schema) {
    for (const oldEntity of entities) {
        const item = oldEntity.userData;
        if (!item) continue; // createEntity 必设 userData，守卫仅为类型收窄
        const directRefs = schema.getAllRefTablesByItem(item);
        for (const ref of directRefs) {
            if (entityMap.has(ref)) {
                oldEntity.sourceEdges.push({
                    sourceHandle: HANDLE_OUT,
                    target: ref,
                    targetHandle: HANDLE_IN,
                    type: EntityEdgeType.Ref,

                })
            }
        }
    }
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
                sourceHandle: HANDLE_OUT,
                target: curTable.name,
                targetHandle: HANDLE_IN,
                type: EntityEdgeType.Ref,
            })

            if (entityMap.size > maxNode / 2) {
                break;
            }
        }
    }


    let frontier: SItem[] = [curTable];
    let entityFrontier: CardEntity<SItem>[] = [curEntity];
    let depth = 1;
    while (depth <= maxOutDepth) {

        const newFrontier: SItem[] = [];
        const newEntityFrontier: CardEntity<SItem>[] = [];

        const refTableNames = schema.getAllRefTablesByItems(frontier);
        for (const ref of refTableNames) {
            if (entityMap.has(ref)) {
                continue;
            }

            const refTable = schema.getSTable(ref);
            if (!refTable) {
                console.log(ref + " not found!")
                continue; // 不该发生
            }

            const entityType = depth == 1 ? EntityType.Ref : EntityType.Ref2;
            const refEntity = createEntity(refTable, ref, entityType);
            entityMap.set(ref, refEntity);

            newFrontier.push(refTable);
            newEntityFrontier.push(refEntity);

            if (entityMap.size > maxNode) {
                break;
            }
        }

        addRefEdgesToExisting(entityFrontier, entityMap, schema);

        frontier = newFrontier;
        entityFrontier = newEntityFrontier;
        depth++;

        if (entityMap.size > maxNode) {
            break;
        }
    }

    // 循环退出后 entityFrontier 是最后一轮新建的末层节点，其出边尚未画；
    // 补画一次（只连 entityMap 中已存在的目标，如回指边），不新建节点。depth>1 表示 while 至少跑过一轮
    if (depth > 1) {
        addRefEdgesToExisting(entityFrontier, entityMap, schema);
    }
}

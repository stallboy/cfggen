import {BriefRecord, RefId, Refs, TableMap} from "../model/recordModel.ts";
import {Entity, EntityConnectionType, EntityType, FieldsShowType} from "../model/graphModel.ts";

export function getLastName(id: string): string {
    let seps = id.split('.');
    return seps[seps.length - 1];
}

export function getLabel(id: string): string {
    let seps = id.split('.');
    return seps[seps.length - 1];
}

export function getId(table: string, id: string): string {
    return table + "-" + id;
}

function isRefIdInTableMap(refId: RefId, tableMap: TableMap): boolean {
    let recordMap = tableMap[refId.table];
    if (recordMap) {
        let briefRecord = recordMap[refId.id];
        if (briefRecord) {
            return true;
        }
    }
    return false;
}

export function createRefs(entity: Entity, refs: Refs, tableMap: TableMap) {
    let refIdMap = refs.$refs;
    if (refIdMap == null) {
        return;
    }
    for (let refName in refIdMap) {
        let refIds: RefId[] = refIdMap[refName];
        let connectToSockets = [];
        for (let refId of refIds) {
            if (isRefIdInTableMap(refId, tableMap)) {
                connectToSockets.push({
                    entityId: getId(refId.table, refId.id),
                    inputKey: 'input',
                    connectionType: EntityConnectionType.Ref
                });
            }
        }
        if (connectToSockets.length > 0) {
            entity.outputs.push({
                output: {key: refName, label: refName},
                connectToSockets: connectToSockets
            });
        }
    }
}

export function createRefEntities(entityMap: Map<string, Entity>, tableMap: TableMap, isCreateRefs: boolean = true) {
    for (let table in tableMap) {
        let recordMap = tableMap[table]
        for (let id in recordMap) {
            let briefRecord: BriefRecord = recordMap[id];
            let refId: RefId = {table, id};
            let eid = getId(table, id);

            let entityType;
            if (briefRecord.depth == 0) {
                entityType = EntityType.Normal;
            } else if (briefRecord.depth == 1) {
                entityType = EntityType.Ref;
            } else if (briefRecord.depth > 1) {
                entityType = EntityType.Ref2;
            } else {
                entityType = EntityType.RefIn;
            }

            let entity: Entity = {
                id: eid,
                label: getLabel(table),
                fields: [{key: 'id', name: 'id', value: id},
                    {key: 'value', name: 'value', value: briefRecord.value}
                ],
                inputs: [],
                outputs: [],

                fieldsShow: FieldsShowType.Direct,
                entityType: entityType,
                userData: refId,
            };
            if (isCreateRefs){
                createRefs(entity, briefRecord, tableMap);
            }
            entityMap.set(eid, entity);
        }
    }
}


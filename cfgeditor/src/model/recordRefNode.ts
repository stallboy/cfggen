import {BriefRecord, RefId, Refs, TableMap} from "./recordModel.ts";
import {Entity, EntityConnectionType, EntityNodeType, FieldsShowType} from "./graphModel.ts";

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

export function createRefs(node: Entity, refs: Refs, tableMap: TableMap) {
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
                    nodeId: getId(refId.table, refId.id),
                    inputKey: 'input',
                    connectionType: EntityConnectionType.Ref
                });
            }
        }
        if (connectToSockets.length > 0) {
            node.outputs.push({
                output: {key: refName, label: refName},
                connectToSockets: connectToSockets
            });
        }
    }
}

export function createRefNodes(entityMap: Map<string, Entity>, tableMap: TableMap, isCreateRefs: boolean = true) {
    for (let table in tableMap) {
        let recordMap = tableMap[table]
        for (let id in recordMap) {
            let briefRecord: BriefRecord = recordMap[id];
            let refId: RefId = {table, id};
            let eid = getId(table, id);

            let nodeType;
            if (briefRecord.depth == 0) {
                nodeType = EntityNodeType.Normal;
            } else if (briefRecord.depth == 1) {
                nodeType = EntityNodeType.Ref;
            } else if (briefRecord.depth > 1) {
                nodeType = EntityNodeType.Ref2;
            } else {
                nodeType = EntityNodeType.RefIn;
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
                nodeType,
                userData: refId,
            };
            if (isCreateRefs){
                createRefs(entity, briefRecord, tableMap);
            }
            entityMap.set(eid, entity);
        }
    }
}


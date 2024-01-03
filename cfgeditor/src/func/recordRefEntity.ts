import {BriefRecord, RefId, Refs} from "../model/recordModel.ts";
import {Entity, EntityConnectionType, EntityType, FieldsShowType} from "../model/entityModel.ts";
import {Schema} from "../model/schemaModel.ts";

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

function isRefIdInTableMap(refId: RefId, briefRecords: BriefRecord[]): boolean {
    for (let br of briefRecords) {
        if (br.table == refId.table && br.id == refId.id){
            return true;
        }
    }
    return false;
}

export function createRefs(entity: Entity, refs: Refs, briefRecords: BriefRecord[]) {
    let refIdMap = refs.$refs;
    if (refIdMap == null) {
        return;
    }
    for (let refName in refIdMap) {
        let refIds: RefId[] = refIdMap[refName];
        let connectToSockets = [];
        for (let refId of refIds) {
            if (isRefIdInTableMap(refId, briefRecords)) {
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

export function createRefEntities(entityMap: Map<string, Entity>, schema: Schema, refs: BriefRecord[], isCreateRefs: boolean = true) {
    JSON.parse
    for (let briefRecord of refs) {
        let table = briefRecord.table;
        let id = briefRecord.id;
        let imgPrefix;
        let sTable = schema.getSTable(table);
        if (sTable) {
            imgPrefix = sTable.imgPrefix;
        }
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

        let img = briefRecord.img;
        if (img && imgPrefix) {
            img = imgPrefix + img;
        }

        let entity: Entity = {
            id: eid,
            label: getId(getLabel(table), id),
            brief: {
                img: img,
                title: briefRecord.title,
                description: briefRecord.description,
                value: briefRecord.value
            },

            inputs: [],
            outputs: [],

            fieldsShow: FieldsShowType.Direct,
            entityType: entityType,
            userData: refId,
        };
        if (isCreateRefs) {
            createRefs(entity, briefRecord, refs);
        }
        entityMap.set(eid, entity);
    }
}


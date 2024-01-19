import {BriefRecord, RefId, Refs} from "../model/recordModel.ts";
import {Entity, EntityEdgeType, EntityType, FieldsShowType} from "../model/entityModel.ts";

import {Schema} from "../model/schemaUtil.ts";

export function getLastName(id: string): string {
    let seps = id.split('.');
    return seps[seps.length - 1];
}

export function getLabel(id: string): string {
    let seps = id.split('.');
    return seps[seps.length - 1];
}

export function getId(table: string, id: string): string {
    return table + "_" + id;
}

function isRefIdInBriefRecords(toTable: string, toId: string, briefRecords: BriefRecord[]): boolean {
    for (let {id, table} of briefRecords) {
        if (table == toTable && id == toId) {
            return true;
        }
    }
    return false;
}

function alwaysOk(_t: string) {
    return true;
}

export function createRefs(entity: Entity, refs: Refs, briefRecords: BriefRecord[],
                           checkTable: ((table: string) => boolean) = alwaysOk,
                           isEnityBrief: boolean = false) {
    let fieldRefs = refs.$refs;
    if (fieldRefs == null) {
        return;
    }
    for (let {firstField, label, toId, toTable} of fieldRefs) {
        if (checkTable(toTable) && isRefIdInBriefRecords(toTable, toId, briefRecords)) {
            entity.sourceEdges.push({
                sourceHandle: isEnityBrief ? '@out' : firstField,
                target: getId(toTable, toId),
                targetHandle: '@in', // target肯定brief模式
                type: EntityEdgeType.Ref,
                label: label,
            });
        }
    }
}

export function createRefEntities(entityMap: Map<string, Entity>, schema: Schema, refs: BriefRecord[],
                                  isCreateRefs: boolean = true, containEnum: boolean = true) {
    let checkTable = alwaysOk;
    if (!containEnum) {
        checkTable = (t: string) => {
            let sT = schema.getSTable(t);
            if (sT == null) {
                return false;
            }
            return sT.entryType != 'eEnum';
        };
    }

    for (let briefRecord of refs) {
        let table = briefRecord.table;
        let id = briefRecord.id;
        let imgPrefix;
        let sTable = schema.getSTable(table);
        if (sTable == null) {
            continue;
        }

        if (!containEnum && sTable.entryType == 'eEnum') {
            continue;
        }

        imgPrefix = sTable.imgPrefix;
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
                descriptions: briefRecord.descriptions,
                value: briefRecord.value
            },

            inputs: [],
            outputs: [],
            sourceEdges: [],

            fieldsShow: FieldsShowType.Direct,
            entityType: entityType,
            userData: refId,
        };
        if (isCreateRefs) {
            createRefs(entity, briefRecord, refs, checkTable, true);
        }
        entityMap.set(eid, entity);
    }
}


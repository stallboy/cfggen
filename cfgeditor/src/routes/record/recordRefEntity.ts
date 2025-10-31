import {BriefRecord, RefId, Refs} from "./recordModel.ts";
import {Entity, EntityEdgeType, EntityType} from "../../flow/entityModel.ts";

import {Schema} from "../table/schemaUtil.tsx";
import {findAllResInfos} from "../../res/findAllResInfos.ts";
import {TauriConf} from "../../store/storageJson.ts";
import {ResInfo} from "../../res/resInfo.ts";

export function getLastName(id: string): string {
    const seps = id.split('.');
    return seps[seps.length - 1];
}

export function getLabel(id: string): string {
    const idx = id.indexOf('.')
    if (idx > 0) {
        return id.substring(idx + 1);
    } else {
        return id;
    }
}

export function getId(table: string, id: string): string {
    return table + "_" + id;
}

function isRefIdInBriefRecords(toTable: string, toId: string, briefRecords: BriefRecord[]): boolean {
    for (const {id, table} of briefRecords) {
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
    const fieldRefs = refs.$refs;
    if (fieldRefs == null) {
        return;
    }
    for (const {firstField, label, toId, toTable} of fieldRefs) {
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

export interface CreateRefEntitiesParameter {
    entityMap: Map<string, Entity>;
    schema: Schema;
    briefRecordRefs: BriefRecord[];

    isCreateRefs: boolean; //true;
    checkTable?: (t: string) => boolean;
    recordRefInShowLinkMaxNode?: number;
    tauriConf: TauriConf;
    resourceDir: string;
    resMap: Map<string, ResInfo[]>;
}


export function createRefEntities({
                                      entityMap,
                                      schema,
                                      briefRecordRefs,
                                      isCreateRefs,
                                      checkTable,
                                      recordRefInShowLinkMaxNode,
                                      tauriConf,
                                      resourceDir,
                                      resMap,
                                  }: CreateRefEntitiesParameter) {

    let isRefInNotShowLink = false;
    if (recordRefInShowLinkMaxNode) {
        let refInCount = 0;
        for (const briefRecord of briefRecordRefs) {
            if (briefRecord.depth == -1) {
                refInCount++;
            }
        }
        if (refInCount > recordRefInShowLinkMaxNode) {
            isRefInNotShowLink = true;
        }
    }


    const myCheckTable = checkTable ?? alwaysOk;
    for (const briefRecord of briefRecordRefs) {
        const table = briefRecord.table;
        const id = briefRecord.id;
        const sTable = schema.getSTable(table);
        if (sTable == null) {
            continue;
        }

        if (!myCheckTable(table)) {
            continue;
        }

        const refId: RefId = {table, id};
        const eid = getId(table, id);

        let isRefIn = false;
        let entityType;
        if (briefRecord.depth == 0) {
            entityType = EntityType.Normal;
        } else if (briefRecord.depth == 1) {
            entityType = EntityType.Ref;
        } else if (briefRecord.depth > 1) {
            entityType = EntityType.Ref2;
        } else {
            entityType = EntityType.RefIn;
            isRefIn = true;
        }


        const label = getId(getLabel(table), id);

        const entity: Entity = {
            id: eid,
            label: label,
            brief: {
                title: briefRecord.title,
                descriptions: briefRecord.descriptions,
                value: briefRecord.value
            },
            sourceEdges: [],
            entityType: entityType,
            userData: refId,
            assets: findAllResInfos({
                label,
                refs: briefRecord,
                tauriConf,
                resourceDir,
                resMap
            })
        };


        let createLink = isCreateRefs;
        if (createLink) {
            if (isRefIn && isRefInNotShowLink) {
                createLink = false;
            }
        }
        if (createLink) {
            createRefs(entity, briefRecord, briefRecordRefs, myCheckTable, true);
        }
        entityMap.set(eid, entity);
    }
}


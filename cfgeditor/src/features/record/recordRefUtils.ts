import {BriefRecord, FieldRef, JSONObject, RefId, Refs} from "@/api/recordModel.ts";
import {Entity, EntityEdgeType, EntityType} from "@/domain/entityModel.ts";

import {Schema} from "@/domain/schema.ts";
import {findAllResInfos} from "@/res/findAllResInfos.ts";
import {TauriConf} from "@/domain/storageJson.ts";
import {ResInfo} from "@/domain/resInfo.ts";
import {HANDLE_IN, HANDLE_OUT} from "@/domain/handleIds.ts";
import {resolveReferenceTarget, visibleRecordNode} from '@/domain/referenceTarget';

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

function alwaysOk() {
    return true;
}

export function createRefs(entity: Entity, refs: Refs, briefRecords: BriefRecord[],
                           checkTable: ((table: string) => boolean) = alwaysOk,
                           isEnityBrief: boolean = false,
                           detailedRecord?: RefId) {
    const fieldRefs = refs.$refs;
    if (fieldRefs == null) {
        return;
    }
    for (const reference of fieldRefs) {
        const {firstField, label, toId, toTable} = reference;
        const sameRecord = detailedRecord?.table === toTable && detailedRecord.id === toId;
        if (checkTable(toTable) && (sameRecord || isRefIdInBriefRecords(toTable, toId, briefRecords))) {
            entity.sourceEdges.push({
                sourceHandle: isEnityBrief ? HANDLE_OUT : firstField,
                target: getId(toTable, toId),
                targetHandle: HANDLE_IN, // target肯定brief模式
                type: EntityEdgeType.Ref,
                label: label,
                ...(reference.toPath ? {reference} : {}),
            });
        }
    }
}

/** Project validated metadata onto existing edit nodes, including folded/embedded source objects. */
export function createEditingRefs(entities: Map<string, Entity>, object: JSONObject, record: RefId,
                                  briefs: BriefRecord[], pending: boolean) {
    const rootId = getId(record.table, record.id);
    let hasReferences = false;
    function visit(value: unknown, path: (string | number)[]) {
        if (Array.isArray(value)) {
            value.forEach((item, index) => visit(item, [...path, index]));
        } else if (value && typeof value === 'object') {
            const object = value as JSONObject & Refs;
            if (object.$refs?.length) {
                hasReferences = true;
                if (!pending) {
                    const source = visibleRecordNode(rootId, path, entities);
                    const entity = source && entities.get(source.nodeId);
                    // Existing primitive edit controls have no field handle; use their node's outlet.
                    if (entity) createRefs(entity, object, briefs, undefined, true, record);
                }
            }
            for (const [name, child] of Object.entries(value)) {
                if (!name.startsWith('$')) visit(child, [...path, name]);
            }
        }
    }
    visit(object, []);
    const root = entities.get(rootId);
    if (root && pending && hasReferences) root.referencesPending = true;
}

/** Run after all detailed nodes and brief cards exist, before fillHandles. */
export function locateRecordRefs(entities: Map<string, Entity>, revision?: string, pendingLabel = 'Pending validation') {
    for (const entity of entities.values()) {
        for (const edge of entity.sourceEdges) {
            if (!edge.reference) continue;
            const target = resolveReferenceTarget(edge.reference, entities, revision);
            if (!target) continue;
            edge.target = target.nodeId;
            if (target.stale) edge.label = `${edge.label ?? ''} · ${pendingLabel}`;
            else if (!target.exact) edge.label = `${edge.label ?? ''} → ${target.pathLabel}`;
        }
    }
}

/** A brief's existing open-record action can carry an unambiguous incoming object target. */
export function incomingObjectReferences(entities: ReadonlyMap<string, Entity>, nodeId: string): FieldRef[] {
    const references = new Map<string, FieldRef>();
    for (const source of entities.values()) for (const edge of source.sourceEdges) {
        if (edge.target === nodeId && edge.reference) {
            references.set(JSON.stringify([edge.reference.toRevision, edge.reference.toPath]), edge.reference);
        }
    }
    return [...references.values()];
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

        const entity: Entity<RefId> = {
            id: eid,
            label: label,
            type: 'card',
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


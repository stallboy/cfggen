import {Entity, EntityEdgeType, EntityField, EntitySourceEdge, EntityType} from "../../flow/entityModel.ts";
import {SField, SStruct, STable} from "../table/schemaModel.ts";
import {BriefRecord, JSONArray, JSONObject, JSONValue, RefId, Refs} from "./recordModel.ts";
import {createRefs, getLabel} from "./recordRefEntity.ts";
import {getField, Schema} from "../table/schemaUtil.ts";
import {findAllResInfos} from "../../res/findAllResInfos.ts";
import {TauriConf} from "../setting/storageJson.ts";
import {ResInfo} from "../../res/resInfo.ts";


export class RecordEntityCreator {
    constructor(public entityMap: Map<string, Entity>,
                public schema: Schema,
                public refId: RefId,
                public refs: BriefRecord[],
                public tauriConf: TauriConf,
                public resouceDir: string,
                public resMap: Map<string, ResInfo[]>) {
    }

    createRecordEntity(id: string,
                       obj: JSONObject & Refs,
                       label?: string,
                       arrayIndex?: number): Entity | null {

        const fields: EntityField[] = [];
        const type: string = obj['$type'] as string;
        if (type == null) {
            console.error('$type missing');
            return null;
        }

        let sItem: STable | SStruct | null = null;
        if (!type.startsWith("$")) {
            sItem = this.schema.itemIncludeImplMap.get(type) as STable | SStruct;
            if (sItem == null) {
                console.error(type + ' not found!');
                return null;
            }
        }

        const sourceEdges: EntitySourceEdge[] = [];

        for (const fieldKey in obj) {
            if (fieldKey.startsWith("$")) {
                continue;
            }
            const fieldValue: JSONValue = obj[fieldKey];

            let sField: SField | null = null;
            if (sItem) {
                sField = getField(sItem, fieldKey);
            }
            const comment = sField?.comment ?? fieldKey;

            const field = {
                key: fieldKey,
                name: fieldKey,
                comment: comment,
                value: '',
            }
            fields.push(field);

            const ft = typeof fieldValue
            if (ft == 'object') {
                if (Array.isArray(fieldValue)) {  // list or map, (map is list of $entry)
                    const fArr: JSONArray = fieldValue as JSONArray;
                    if (fArr.length == 0) {
                        field.value = '[]'
                    } else {
                        const ele = fArr[0];
                        if (typeof ele == 'object') { // list of struct/interface, or map
                            let i = 0;
                            for (const e of fArr) {
                                const fObj: JSONObject & Refs = e as JSONObject & Refs;
                                const childId: string = `${id}-${fieldKey}[${i}]`;
                                const childEntity = this.createRecordEntity(childId, fObj, undefined, i + 1);
                                i++;

                                if (childEntity) {
                                    sourceEdges.push({
                                        sourceHandle: fieldKey,
                                        target: childEntity.id,
                                        targetHandle: '@in',
                                        type: EntityEdgeType.Normal
                                    })
                                }
                            }
                            field.value = `[]*${i}`

                        } else {  // list of primitive value
                            field.value = fArr.join(',')
                        }
                    }
                } else { // struct or interface
                    const fObj: JSONObject & Refs = fieldValue as JSONObject & Refs;
                    const childId: string = id + "-" + fieldKey;
                    const childEntity = this.createRecordEntity(childId, fObj);
                    if (childEntity) {
                        sourceEdges.push({
                            sourceHandle: fieldKey,
                            target: childEntity.id,
                            targetHandle: '@in',
                            type: EntityEdgeType.Normal,
                        });
                    }
                    field.value = '<>';
                }
            } else { // primitive
                let valueStr: string = fieldValue.toString();
                if (ft == 'boolean') {
                    const fb = fieldValue as boolean
                    valueStr = fb ? '✔️' : '✘';
                }
                field.value = valueStr
            }
        }

        let thisLabel = label ?? getLabel(type);
        thisLabel = arrayIndex === undefined ? thisLabel : thisLabel + '.' + arrayIndex;

        const entity: Entity = {
            id: id,
            label: thisLabel,
            fields: fields,
            sourceEdges: sourceEdges,
            entityType: EntityType.Normal,
            userData: this.refId,
            assets: findAllResInfos({
                label: thisLabel,
                refs: obj,
                tauriConf: this.tauriConf,
                resourceDir: this.resouceDir,
                resMap: this.resMap,
            }),
        };

        this.entityMap.set(id, entity);
        createRefs(entity, obj, this.refs);
        return entity;
    }

}

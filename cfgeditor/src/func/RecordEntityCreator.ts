import {
    Entity,
    EntityEdgeType,
    EntityField,
    EntitySourceEdge,
    EntityType,
    FieldsShowType
} from "../model/entityModel.ts";
import {SField, SStruct, STable} from "../model/schemaModel.ts";
import {BriefRecord, JSONArray, JSONObject, JSONValue, RefId, Refs} from "../model/recordModel.ts";
import {createRefs, getLabel} from "./recordRefEntity.ts";
import {getField, Schema} from "../model/schemaUtil.ts";


export class RecordEntityCreator {
    constructor(public entityMap: Map<string, Entity>,
                public schema: Schema,
                public refId: RefId,
                public refs: BriefRecord[]) {
    }

    createRecordEntity(id: string, obj: JSONObject & Refs): Entity | null {
        let fields: EntityField[] = [];
        let type: string = obj['$type'] as string;
        if (type == null) {
            console.error('$type missing');
            return null;
        }

        let label = getLabel(type);

        let sItem: STable | SStruct | null = null;
        if (!type.startsWith("$")) {
            sItem = this.schema.itemIncludeImplMap.get(type) as STable | SStruct;
            if (sItem == null) {
                console.error(type + ' not found!');
                return null;
            }
        }

        let sourceEdges: EntitySourceEdge[] = [];

        for (let fieldKey in obj) {
            if (fieldKey.startsWith("$")) {
                continue;
            }
            let fieldValue: JSONValue = obj[fieldKey];

            let sField: SField | null = null;
            if (sItem) {
                sField = getField(sItem, fieldKey);
            }
            let comment = sField?.comment ?? fieldKey;

            const field = {
                key: fieldKey,
                name: fieldKey,
                comment: comment,
                value: '',
            }
            fields.push(field);

            let ft = typeof fieldValue
            if (ft == 'object') {
                if (Array.isArray(fieldValue)) {  // list or map, (map is list of $entry)
                    let fArr: JSONArray = fieldValue as JSONArray;
                    if (fArr.length == 0) {
                        field.value = '[]'
                    } else {
                        let ele = fArr[0];
                        if (typeof ele == 'object') { // list of struct/interface, or map
                            let i = 0;
                            for (let e of fArr) {
                                let fObj: JSONObject & Refs = e as JSONObject & Refs;
                                let childId: string = `${id}-${fieldKey}[${i}]`;
                                let childEntity = this.createRecordEntity(childId, fObj);
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
                    let fObj: JSONObject & Refs = fieldValue as JSONObject & Refs;
                    let childId: string = id + "-" + fieldKey;
                    let childEntity = this.createRecordEntity(childId, fObj);
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
                    let fb = fieldValue as boolean
                    valueStr = fb ? '✔️' : '✘';
                }
                field.value = valueStr
            }
        }

        let entity: Entity = {
            id: id,
            label: label,
            fields: fields,
            inputs: [],
            outputs: [],
            sourceEdges: sourceEdges,

            fieldsShow: FieldsShowType.Direct,
            entityType: EntityType.Normal,
            userData: this.refId,
        };

        this.entityMap.set(id, entity);
        createRefs(entity, obj, this.refs);
        return entity;
    }

}

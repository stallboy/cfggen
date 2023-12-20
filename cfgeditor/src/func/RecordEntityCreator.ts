import {
    Entity,
    EntityConnectionType,
    EntityField,
    EntityType,
    EntitySocketOutput,
    FieldsShowType
} from "../model/graphModel.ts";
import {getField, Schema, SField, SStruct, STable} from "../model/schemaModel.ts";
import {JSONArray, JSONObject, JSONValue, RefId, Refs, TableMap} from "../model/recordModel.ts";
import {createRefs, getLabel} from "./recordRefEntity.ts";

export class RecordEntityCreator {
    constructor(public entityMap: Map<string, Entity>,
                public schema: Schema,
                public refId: RefId,
                public refs : TableMap) {
    }

    createEntity(id: string, obj: JSONObject & Refs): Entity | null {
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

        let outputs: EntitySocketOutput[] = [];

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

            let ft = typeof fieldValue
            if (ft == 'object') {
                if (Array.isArray(fieldValue)) {  // list or map, (map is list of $entry)
                    let fArr: JSONArray = fieldValue as JSONArray;
                    if (fArr.length == 0) {
                        fields.push({
                            key: fieldKey,
                            name: fieldKey,
                            comment: comment,
                            value: '[]'
                        });
                    } else {
                        let ele = fArr[0];
                        if (typeof ele == 'object') { // list of struct/interface, or map
                            let i = 0;
                            let connectToSockets = [];
                            for (let e of fArr) {
                                let fObj: JSONObject & Refs = e as JSONObject & Refs;
                                let childId: string = `${id}-${fieldKey}[${i}]`;
                                let childEntity = this.createEntity(childId, fObj);
                                i++;

                                if (childEntity) {
                                    connectToSockets.push({
                                        entityId: childEntity.id,
                                        inputKey: 'input',
                                        connectionType: EntityConnectionType.Normal
                                    });
                                }
                            }

                            outputs.push({
                                output: {key: fieldKey, label: fieldKey},
                                connectToSockets: connectToSockets
                            });

                        } else {  // list of primitive value
                            let i = 0;
                            for (let e of fArr) {
                                fields.push({
                                    key: `${fieldKey}[${i}]`,
                                    name: i == 0 ? fieldKey : "",
                                    comment: i == 0 ? comment : "",
                                    value: e.toString(),
                                });
                                i++;
                            }

                        }
                    }
                } else { // struct or interface
                    let fObj: JSONObject & Refs = fieldValue as JSONObject & Refs;
                    let childId: string = id + "-" + fieldKey;
                    let childEntity = this.createEntity(childId, fObj);
                    if (childEntity) {
                        outputs.push({
                            output: {key: fieldKey, label: fieldKey},
                            connectToSockets: [{
                                entityId: childEntity.id,
                                inputKey: 'input',
                                connectionType: EntityConnectionType.Normal
                            }]
                        });
                    }
                }
            } else { // primitive
                let valueStr: string = fieldValue.toString();
                if (ft == 'boolean') {
                    let fb = fieldValue as boolean
                    valueStr = fb ? '✔️' : '✘';
                }

                fields.push({
                    key: fieldKey,
                    name: fieldKey,
                    comment: comment,
                    value: valueStr
                });
            }
        }

        let entity: Entity = {
            id: id,
            label: label,
            fields: fields,
            inputs: [],
            outputs: outputs,

            fieldsShow: FieldsShowType.Direct,
            entityType: EntityType.Normal,
            userData: this.refId,
        };

        this.entityMap.set(id, entity);
        createRefs(entity, obj, this.refs);
        return entity;
    }

}

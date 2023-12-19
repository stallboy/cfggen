import {
    PrimitiveType,
    Entity,
    EntityConnectionType,
    EntityEditField,
    EntityNodeType,
    EntitySocketOutput,
    FieldsShowType
} from "./graphModel.ts";
import {getField, getImpl, Schema, SInterface, SItem, SStruct, STable} from "./schemaModel.ts";
import {JSONArray, JSONObject, JSONValue, RefId, Refs} from "./recordModel.ts";
import {getLabel, getLastName} from "./recordRefNode.ts";


function getImplNames(sInterface: SInterface): string[] {
    let impls = [];
    for (let impl of sInterface.impls) {
        impls.push(impl.name);
    }
    return impls;
}

const setOfPrimitive = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

function isPrimitiveType(type: string): boolean {
    return setOfPrimitive.has(type);
}


export class RecordEditNodeCreator {
    constructor(public entityMap: Map<string, Entity>,
                public schema: Schema,
                public refId: RefId,
                public setEditingRecord: ((record: JSONObject & Refs) => void)) {
    }

    createNodes(id: string, sItem: SItem, obj: JSONObject & Refs, isArrayItem: boolean = false): Entity | null {
        let label = getLabel(sItem.name);

        let type: string = obj['$type'] as string;
        if (type == null) {
            console.error('$type missing');
            return null;
        }
        let fields: EntityEditField[] = this.makeEditFields(sItem, obj);

        let structural: STable | SStruct;
        if ('impls' in sItem) {
            structural = this.schema.itemIncludeImplMap.get(type) as SStruct;
        } else {
            structural = sItem;
        }


        let outputs: EntitySocketOutput[] = [];
        for (let fieldKey in obj) {
            if (fieldKey.startsWith("$")) {
                continue;
            }
            let fieldValue: JSONValue = obj[fieldKey];
            let ft = typeof fieldValue
            if (ft != 'object') {
                continue;
            }

            let sField = getField(structural, fieldKey);
            if (sField == null) {
                continue;
            }

            if (Array.isArray(fieldValue)) {  // list or map, (map is list of $entry)
                let fArr: JSONArray = fieldValue as JSONArray;
                if (fArr.length == 0) {
                    continue;
                }
                let ele = fArr[0];
                if (typeof ele != 'object') { // list of primitive value
                    continue;
                }

                if (!sField.type.startsWith("list<")) {
                    continue;
                }
                let itemTypeId = sField.type.substring(5, sField.type.length - 1);
                let itemType = this.schema.itemIncludeImplMap.get(itemTypeId);
                if (itemType == null) {
                    continue;
                }

                // list of struct/interface, or map
                let i = 0;
                let connectToSockets = [];
                for (let e of fArr) {
                    let itemObj = e as JSONObject & Refs;
                    let childId: string = `${id}-${fieldKey}[${i}]`;
                    let childNode = this.createNodes(childId, itemType, itemObj, true);
                    i++;

                    if (childNode) {
                        connectToSockets.push({
                            nodeId: childNode.id,
                            inputKey: 'input',
                            connectionType: EntityConnectionType.Normal
                        });
                    }
                }

                outputs.push({
                    output: {key: fieldKey, label: fieldKey},
                    connectToSockets: connectToSockets
                });


            } else { // struct or interface
                let fieldType = this.schema.itemIncludeImplMap.get(sField.type);
                if (fieldType == null) {
                    continue;
                }
                let fieldObj = fieldValue as JSONObject & Refs;
                let childId: string = id + "-" + fieldKey;
                let childNode = this.createNodes(childId, fieldType, fieldObj);
                if (childNode) {
                    outputs.push({
                        output: {key: fieldKey, label: fieldKey},
                        connectToSockets: [{
                            nodeId: childNode.id,
                            inputKey: 'input',
                            connectionType: EntityConnectionType.Normal
                        }]
                    });
                }
            }
        }

        if (isArrayItem) {
            fields.push({
                name: '$del',
                comment: '',
                type: 'funcDelete',
                eleType: 'bool',
                value: () => {
                    // TODO
                }
            });
        }

        let entity: Entity = {
            id: id,
            label: label,
            fields: [],
            editFields: fields,
            inputs: [],
            outputs: outputs,

            fieldsShow: FieldsShowType.Edit,
            nodeType: EntityNodeType.Normal,
            userData: this.refId,
        };

        this.entityMap.set(id, entity);
        return entity;
    }

    makeEditFields(sItem: SItem, obj: JSONObject): EntityEditField[] {
        let fields: EntityEditField[] = [];
        let type: string = obj['$type'] as string;
        if ('impls' in sItem) {
            let implName = getLastName(type);
            let sInterface = sItem as SInterface;
            let impl = getImpl(sInterface, implName) as SStruct;
            fields.push({
                name: '$impl',
                type: 'interface',
                eleType: sInterface.name,
                value: implName,
                autoCompleteOptions: getImplNames(sInterface),
                implFields: this.makeEditFields(impl, obj),
                interfaceOnChangeImpl: (newImplName: string) => {
                    let newObj: JSONObject;
                    if (newImplName == implName) {
                        newObj = obj;
                    } else {
                        let newImpl = getImpl(sInterface, newImplName);
                        if (newImpl) {
                            newObj = this.schema.defaultValueOfStruct(newImpl);
                        }
                    }

                },
            })

        } else {
            let structural = sItem as (SStruct | STable);
            this.makeStructuralEditFields(fields, structural, obj);

            if ('pk' in structural) {
                fields.push({
                    name: '$submit',
                    comment: '',
                    type: 'funcSubmit',
                    eleType: 'bool',
                    value: () => {
                        // TODO
                    }
                });
            }
        }
        return fields;

    }


    makeStructuralEditFields(fields: EntityEditField[], structural: SStruct | STable, obj: JSONObject) {
        for (let sf of structural.fields) {
            let fieldValue = obj[sf.name];
            if (isPrimitiveType(sf.type)) {
                let v;
                if (fieldValue) {
                    v = fieldValue as (boolean | number | string);
                } else if (sf.type == 'bool') {
                    v = false;
                } else if (sf.type == 'str' || sf.type == 'text') {
                    v = '';
                } else {
                    v = 0;
                }

                fields.push({
                    name: sf.name,
                    comment: sf.comment,
                    type: 'primitive',
                    eleType: sf.type as PrimitiveType,
                    value: v,
                });
            } else if (sf.type.startsWith('list<')) {
                let itemType = sf.type.substring(5, sf.type.length - 1);
                if (isPrimitiveType(itemType)) {
                    let v = fieldValue ? fieldValue as (boolean | number | string) : [];
                    fields.push({
                        name: sf.name,
                        comment: sf.comment,
                        type: 'arrayOfPrimitive',
                        eleType: itemType as PrimitiveType,
                        value: v,
                    });
                } else {
                    fields.push({
                        name: sf.name,
                        comment: sf.comment,
                        type: 'funcAdd',
                        eleType: itemType,
                        value: () => {
                            // TODO
                        }
                    });
                }

            } else { // 为简单，不支持map<
                // ignore
            }
        }

    }

}

import {
    Entity,
    EntityEdgeType,
    EntityEditField,
    EntityEditFieldOptions,
    EntitySourceEdge,
    EntityType,
    PrimitiveType
} from "../../flow/entityModel.ts";
import {SInterface, SItem, SStruct, STable} from "../table/schemaModel.ts";
import {JSONArray, JSONObject, JSONValue, RefId} from "./recordModel.ts";
import {getId, getLabel, getLastName} from "./recordRefEntity.ts";
import {getField, getIdOptions, getImpl, isPkInteger, Schema} from "../table/schemaUtil.ts";
import {
    applyNewEditingObject, editState,
    onAddItemForArray,
    onDeleteItemFromArray,
    onUpdateFormValues,
    onUpdateInterfaceValue
} from "./editingObject.ts";


const setOfPrimitive = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

function isPrimitiveType(type: string): boolean {
    return setOfPrimitive.has(type);
}


export class RecordEditEntityCreator {
    curRefId: RefId;

    constructor(public entityMap: Map<string, Entity>,
                public schema: Schema,
                public curTable: STable,
                public curId: string,
    ) {
        this.curRefId = {table: curTable.name, id: curId};
    }

    createThis() {
        let id = getId(this.curTable.name, this.curId);
        this.createEntity(id, this.curTable, editState.editingObject, []);
    }

    createEntity(id: string, sItem: SItem, obj: JSONObject,
                 fieldChain: (string | number)[], onDeleteFunc ?: () => void):
        Entity | null {
        let label = getLabel(sItem.name);

        let type: string = obj['$type'] as string;
        if (type == null) {
            console.error('$type missing');
            return null;
        }
        let editFields: EntityEditField[] = this.makeEditFields(sItem, obj, fieldChain);

        let structural: STable | SStruct;
        if ('impls' in sItem) {
            structural = this.schema.itemIncludeImplMap.get(type) as SStruct;
        } else {
            structural = sItem;
        }

        let sourceEdges: EntitySourceEdge[] = [];
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

                if (!sField.type.startsWith("list<")) {  // 不支持map
                    continue;
                }
                let itemTypeId = sField.type.substring(5, sField.type.length - 1);
                let itemType = this.schema.itemIncludeImplMap.get(itemTypeId);
                if (itemType == null) {
                    continue;
                }

                // list of struct/interface
                let i = 0;
                for (let e of fArr) {
                    let itemObj = e as JSONObject;
                    let childId: string = `${id}-${fieldKey}[${i}]`;
                    let arrayIndex = i;

                    const onDeleteFunc = () => {
                        onDeleteItemFromArray(arrayIndex, [...fieldChain, fieldKey]);
                    }

                    let childEntity = this.createEntity(childId, itemType, itemObj,
                        [...fieldChain, fieldKey, i], onDeleteFunc);
                    i++;

                    if (childEntity) {
                        sourceEdges.push({
                            sourceHandle: fieldKey,
                            target: childEntity.id,
                            targetHandle: '@in',
                            type: EntityEdgeType.Normal,
                        })
                    }
                }

            } else { // struct or interface
                let fieldType = this.schema.itemIncludeImplMap.get(sField.type);
                if (fieldType == null) {
                    continue;
                }
                let fieldObj = fieldValue as JSONObject;
                let childId: string = id + "-" + fieldKey;
                let childEntity = this.createEntity(childId, fieldType, fieldObj, [...fieldChain, fieldKey]);
                if (childEntity) {
                    sourceEdges.push({
                        sourceHandle: fieldKey,
                        target: childEntity.id,
                        targetHandle: '@in',
                        type: EntityEdgeType.Normal,
                    });
                }
            }
        }

         const editOnUpdateValues = (values: any) => {
            onUpdateFormValues(this.schema, values, fieldChain);
        };

        let entity: Entity = {
            id: id,
            label: label,
            edit: {editFields, editOnDelete:onDeleteFunc, editOnUpdateValues},
            sourceEdges: sourceEdges,

            entityType: EntityType.Normal,
            userData: this.curRefId,
        };

        this.entityMap.set(id, entity);
        return entity;
    }

    makeEditFields(sItem: SItem,
                   obj: JSONObject,
                   fieldChain: (string | number)[]):
        EntityEditField[] {
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
                autoCompleteOptions: getImplNameOptions(sInterface),
                implFields: this.makeEditFields(impl, obj, fieldChain),
                interfaceOnChangeImpl: (newImplName: string) => {
                    let newObj: JSONObject;
                    if (newImplName == implName) {
                        newObj = obj;
                    } else {
                        let newImpl = getImpl(sInterface, newImplName) as SStruct;
                        newObj = this.schema.defaultValueOfStructural(newImpl);
                    }

                    onUpdateInterfaceValue(newObj, fieldChain);
                    // console.log(newObj);
                },
            })

        } else {
            let structural = sItem as (SStruct | STable);
            this.makeStructuralEditFields(fields, structural, obj, fieldChain);

            let funcClear = () => {
                let defaultValue = this.schema.defaultValueOfStructural(structural);
                applyNewEditingObject(defaultValue);
            };

            if ('pk' in structural) {
                fields.push({
                    name: '$submit',
                    comment: '',
                    type: 'funcSubmit',
                    eleType: 'bool',
                    value: {
                        funcSubmit: editState.submitEditingObject,
                        funcClear
                    }
                });
            }
        }
        return fields;

    }


    makeStructuralEditFields(fields: EntityEditField[],
                             structural: SStruct | STable,
                             obj: JSONObject,
                             fieldChain: (string | number)[]) {
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
                    autoCompleteOptions: this.getAutoCompleteOptions(structural, sf.name),
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
                        autoCompleteOptions: this.getAutoCompleteOptions(structural, sf.name),
                    });
                } else {
                    fields.push({
                        name: sf.name,
                        comment: sf.comment,
                        type: 'funcAdd',
                        eleType: itemType,
                        value: () => {
                            let sFieldable = this.schema.itemIncludeImplMap.get(itemType) as SStruct | SInterface;
                            let defaultValue = this.schema.defaultValue(sFieldable);
                            onAddItemForArray(defaultValue, [...fieldChain, sf.name]);
                        }
                    });
                }

            } else { // struct or interface or 不支持的map<
                fields.push({
                    name: sf.name,
                    comment: sf.comment,
                    type: 'structRef',
                    eleType: sf.type,
                    value: '[]',
                });
            }
        }

    }

    getAutoCompleteOptions(structural: SStruct | STable,
                           fieldName: string): EntityEditFieldOptions | undefined {
        if (!structural.foreignKeys) {
            return;
        }
        let fkTable = null;
        for (let fk of structural.foreignKeys) {
            if (fk.keys.length == 1 && fk.keys[0] == fieldName &&
                (fk.refType == 'rPrimary' || fk.refType == 'rNullablePrimary')) {
                fkTable = fk.refTable;
                break;
            }
        }
        if (fkTable == null) {
            return;
        }
        let sTable = this.schema.getSTable(fkTable);
        if (sTable == null) {
            return;
        }

        const isValueInteger = isPkInteger(sTable)
        const isEnum = sTable.entryType == 'eEnum'
        const options = getIdOptions(sTable, isValueInteger && isEnum);

        return {options, isValueInteger, isEnum};
    }

}


function getImplNameOptions(sInterface: SInterface): EntityEditFieldOptions {
    const impls = [];
    for (let {name} of sInterface.impls) {
        impls.push({value: name, label: name});
    }
    return {options: impls, isValueInteger: false, isEnum: true};
}
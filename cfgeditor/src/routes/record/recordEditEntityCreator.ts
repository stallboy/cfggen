import {
    EditableEntity, Entity, PrimitiveValue,
    EntityEdgeType, EntityEdit,
    EntityEditField,
    EntityEditFieldOptions, EntityPosition,
    EntitySourceEdge,
    EntityType,
    PrimitiveType, PrimitiveEditField
} from "../../flow/entityModel.ts";
import {SField, SInterface, SItem, SStruct, STable} from "../table/schemaModel.ts";
import {JSONArray, JSONObject, JSONValue, RefId} from "./recordModel.ts";
import {getId, getLabel, getLastName} from "./recordRefEntity.ts";
import {getField, getIdOptions, getImpl, getMapEntryTypeName, isPkInteger, Schema} from "../table/schemaUtil.tsx";
import {
    applyNewEditingObject, editState,
    onAddItemToArray,
    onDeleteItemFromArray, onMoveItemInArray, onUpdateFold,
    onUpdateFormValues,
    onUpdateInterfaceValue, onUpdateNote
} from "./editingObject.ts";


const setOfPrimitive = new Set<string>(['bool', 'int', 'long', 'float', 'str', 'text']);

function isPrimitiveType(type: string): boolean {
    return setOfPrimitive.has(type);
}


interface ArrayItemParam {
    onDeleteFunc: (position: EntityPosition) => void;
    onMoveUpFunc?: (position: EntityPosition) => void;
    onMoveDownFunc?: (position: EntityPosition) => void;
}

export class RecordEditEntityCreator {
    curRefId: RefId;

    constructor(public entityMap: Map<string, EditableEntity | Entity>,
                public schema: Schema,
                public curTable: STable,
                public curId: string,
                public folds: Folds,
                public setFolds: (f: Folds) => void,
    ) {
        this.curRefId = {table: curTable.name, id: curId};
    }

    createThis() {
        const id = getId(this.curTable.name, this.curId);
        this.createEntity(id, this.curTable, editState.editingObject, []);
    }

    createEntity(id: string,
                 sItem: SItem,
                 obj: JSONObject,
                 fieldChain: (string | number)[],
                 arrayItemParam?: ArrayItemParam,
                 canBeEmbedded?: boolean): EditableEntity | null {

        const type: string = obj['$type'] as string;
        if (type == null) {
            console.error('$type missing');
            return null;
        }
        let structural: STable | SStruct;
        if ('impls' in sItem) {
            structural = this.schema.itemIncludeImplMap.get(type) as SStruct;
        } else {
            structural = sItem;
        }

        const note: string | undefined = obj['$note'] as string | undefined;
        let fold: boolean | undefined = this.folds.isFold(fieldChain);
        if (fold === undefined) { // 只有本地状态没有设置时才用服务器的
            fold = obj['$fold'] as boolean | undefined;
        }

        let hasChild: boolean = false;

        const editFields: EntityEditField[] = this.makeEditFields(sItem, obj, fieldChain);
        const sourceEdges: EntitySourceEdge[] = [];


        for (const fieldKey in obj) {
            if (fieldKey.startsWith("$")) {
                continue;
            }
            const fieldValue: JSONValue = obj[fieldKey];
            const ft = typeof fieldValue
            if (ft != 'object') {
                continue;
            }

            const sField = getField(structural, fieldKey);
            if (sField == null) {
                continue;
            }

            if (Array.isArray(fieldValue)) {  // list or map, (map is list of $entry)
                const fArr: JSONArray = fieldValue as JSONArray;
                const fArrLen = fArr.length
                if (fArrLen == 0) {
                    continue;
                }
                const ele = fArr[0];
                if (typeof ele != 'object') { // list of primitive value
                    continue;
                }

                const itemTypeId = getItemTypeId(sField.type, structural, fieldKey);
                if (itemTypeId == undefined) {
                    continue;
                }

                const itemType = this.schema.itemIncludeImplMap.get(itemTypeId);
                if (itemType == null) {
                    continue;
                }
                hasChild = true;
                if (fold) {
                    continue;
                }

                // list of struct/interface, or map
                let i = 0;
                for (const e of fArr) {
                    const itemObj = e as JSONObject;
                    const childId: string = `${id}-${fieldKey}[${i}]`;
                    const arrayIndex = i;

                    const chain = [...fieldChain, fieldKey]
                    const onDeleteFunc = (position: EntityPosition) => {
                        onDeleteItemFromArray(arrayIndex, chain, position);
                    }

                    let onMoveUpFunc;
                    if (arrayIndex > 0) {
                        onMoveUpFunc = (position: EntityPosition) => {
                            onMoveItemInArray(arrayIndex, arrayIndex - 1, chain, position);
                        }
                    }

                    let onMoveDownFunc;
                    if (arrayIndex < fArrLen - 1) {
                        onMoveDownFunc = (position: EntityPosition) => {
                            onMoveItemInArray(arrayIndex, arrayIndex + 1, chain, position);
                        }
                    }


                    const childEntity = this.createEntity(
                        childId, itemType, itemObj,
                        [...fieldChain, fieldKey, arrayIndex],
                        {
                            onDeleteFunc,
                            onMoveUpFunc,
                            onMoveDownFunc,
                        });
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
                const fieldType = this.schema.itemIncludeImplMap.get(sField.type);
                if (fieldType == null) {
                    continue;
                }

                // 检查是否为内嵌模式
                const canEmbed = canBeEmbeddedCheck(fieldValue as JSONObject, sField, this.schema);

                if (canEmbed) {
                    const isFolded = this.folds.isFold([...fieldChain, fieldKey]);
                    if (isFolded !== false) {
                        // fold=true 或 undefined，内嵌模式，不创建子节点
                        continue;
                    }
                    // fold=false，展开模式，继续创建子节点
                }

                // 正常模式: 创建子节点
                hasChild = true;
                if (fold) {
                    continue;
                }

                const fieldObj = fieldValue as JSONObject;
                const childId: string = id + "-" + fieldKey;
                const childEntity = this.createEntity(
                    childId,
                    fieldType,
                    fieldObj,
                    [...fieldChain, fieldKey],
                    undefined,  // arrayItemParam
                    canEmbed    // canBeEmbedded 标志
                );
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


        const editOnUpdateValues = (values: Record<string, unknown>) => {
                onUpdateFormValues(this.schema, values, fieldChain);
            }
        ;

        const editOnUpdateNote = (note?: string) => {
            onUpdateNote(note, fieldChain);
        };

        const editOnUpdateFold = (fold: boolean, position: EntityPosition, embeddedFieldChain?: (string | number)[]) => {
            // 如果提供了 embeddedFieldChain，使用它；否则使用默认的 fieldChain
            const targetChain = embeddedFieldChain ?? fieldChain;
            onUpdateFold(fold, targetChain, position);
            const newFolds = this.folds.setFold(targetChain, fold);
            this.setFolds(newFolds);
        };


        let label = getLabel(sItem.name);
        if (arrayItemParam) {
            const idx = fieldChain[fieldChain.length - 1] as number + 1
            label = label + '.' + idx
        }

        const edit: EntityEdit = {
            fields: editFields,  // 重命名：editFields -> fields
            editOnDelete: arrayItemParam?.onDeleteFunc,
            editOnMoveUp: arrayItemParam?.onMoveUpFunc,
            editOnMoveDown: arrayItemParam?.onMoveDownFunc,
            editOnUpdateValues,
            editOnUpdateNote,
            editOnUpdateFold,
            fold,
            hasChild,
            canBeEmbedded,
        }

        if (sItem.type != 'table') {
            edit.editFieldChain = fieldChain;
            edit.editObj = obj;
            edit.editAllowObjType = sItem.id ?? sItem.name;
        }

        const entity: EditableEntity = {
            id: id,
            label: label,
            type: 'editable',
            edit: edit,
            sourceEdges: sourceEdges,

            entityType: EntityType.Normal,
            note: note,
            userData: this.curRefId,
        };

        this.entityMap.set(id, entity);
        return entity;
    }

    makeEditFields(sItem: SItem,
                   obj: JSONObject,
                   fieldChain: (string | number)[]):
        EntityEditField[] {
        const fields: EntityEditField[] = [];
        const type: string = obj['$type'] as string;
        if ('impls' in sItem) { // is interface
            const implName = getLastName(type);
            const sInterface = sItem as SInterface;
            const impl = getImpl(sInterface, implName) as SStruct;
            fields.push({
                name: '$impl',
                comment: sItem.comment,
                type: 'interface',
                eleType: sInterface.name,
                value: implName,
                autoCompleteOptions: getImplNameOptions(sInterface),
                implFields: this.makeEditFields(impl, obj, fieldChain),
                interfaceOnChangeImpl: (newImplName: string, position: EntityPosition) => {
                    let newObj: JSONObject;
                    if (newImplName == implName) {
                        newObj = obj;
                    } else {
                        const newImpl = getImpl(sInterface, newImplName) as SStruct;
                        newObj = this.schema.defaultValueOfStructural(newImpl);
                    }
                    onUpdateInterfaceValue(newObj, fieldChain, position);
                },
            })

        } else {
            const structural = sItem as (SStruct | STable);
            this.makeStructuralEditFields(fields, structural, obj, fieldChain);

            const funcClear = () => {
                const defaultValue = this.schema.defaultValueOfStructural(structural);
                applyNewEditingObject(defaultValue);
            };

            if ('pk' in structural) { // is STable
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
        for (const sf of structural.fields) {
            const fieldValue = obj[sf.name];
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
            } else {
                const itemTypeId = getItemTypeId(sf.type, structural, sf.name);
                if (itemTypeId != undefined) { // list or map
                    if (isPrimitiveType(itemTypeId)) {
                        const v: PrimitiveValue[] = fieldValue ? fieldValue as PrimitiveValue[] : [];
                        fields.push({
                            name: sf.name,
                            comment: sf.comment,
                            type: 'arrayOfPrimitive',
                            eleType: itemTypeId as PrimitiveType,
                            value: v,
                            autoCompleteOptions: this.getAutoCompleteOptions(structural, sf.name),
                        });
                    } else {
                        fields.push({
                            name: sf.name,
                            comment: sf.comment,
                            type: 'funcAdd',
                            eleType: itemTypeId,
                            value: (position: EntityPosition) => {
                                const sFieldable = this.schema.itemIncludeImplMap.get(itemTypeId) as SStruct | SInterface;
                                const defaultValue = this.schema.defaultValue(sFieldable);
                                onAddItemToArray(defaultValue, [...fieldChain, sf.name], position);
                            }
                        });
                    }
                } else { // struct or interface
                    const fieldType = this.schema.itemIncludeImplMap.get(sf.type);
                    if (!fieldType) {
                        fields.push({
                            name: sf.name,
                            comment: sf.comment,
                            type: 'structRef',
                            eleType: sf.type,
                            value: '[]',
                        });
                        continue;
                    }

                    const fieldObj = fieldValue as JSONObject;

                    // 判断是否可以内嵌
                    if (canBeEmbeddedCheck(fieldObj, sf, this.schema)) {
                        const isFolded = this.folds.isFold([...fieldChain, sf.name]);

                        if (isFolded !== false) {
                            // fold=true 或 undefined，内嵌模式
                            let embeddedField: EntityEditField | null = null;

                            if (fieldType.type === 'struct') {
                                const struct = fieldType as SStruct;
                                if (struct.fields.length === 0) {
                                    // 条件1a: struct没有字段 - 创建空的占位字段
                                    embeddedField = {
                                        name: sf.name,
                                        comment: sf.comment,
                                        type: 'primitive',
                                        eleType: 'str',
                                        value: '<>', // 空字段，无实际值

                                    };
                                } else {
                                    // 条件1b: struct只有一个primitive字段
                                    embeddedField = getEmbeddedFieldFromStruct(struct, fieldObj);
                                }
                            } else if (fieldType.type === 'interface') {
                                const iface = fieldType as SInterface;
                                const type = fieldObj['$type'] as string;
                                const implName = getLastName(type);
                                const impl = getImpl(iface, implName);

                                if (impl && impl.fields.length === 0) {
                                    // 条件2: interface无字段impl
                                    embeddedField = {
                                        name: sf.name,
                                        comment: sf.comment,
                                        type: 'primitive',
                                        eleType: 'str',
                                        value: implName,
                                    };
                                } else if (impl && impl.fields.length === 1) {
                                    // 条件3: interface的defaultImpl且只有一个primitive字段
                                    embeddedField = getEmbeddedFieldFromStruct(impl, fieldObj);
                                }
                            }

                            if (embeddedField) {
                                fields.push({
                                    name: sf.name,
                                    comment: sf.comment,
                                    type: 'structRef',
                                    eleType: sf.type,
                                    value: '[]',
                                    handleOut: true,

                                    // 新增字段
                                    isEmbedded: true,
                                    embeddedField: embeddedField as PrimitiveEditField,
                                    embeddedFieldChain: [...fieldChain, sf.name],
                                });
                                continue; // 跳过后续处理
                            }
                        }
                        // 如果 isFolded=false，则走正常流程创建独立子节点
                    }

                    // 正常模式: 创建独立子节点
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

    }

    getAutoCompleteOptions(structural: SStruct | STable,
                           fieldName: string): EntityEditFieldOptions | undefined {
        if (!structural.foreignKeys) {
            return;
        }
        let fkTable = null;
        for (const fk of structural.foreignKeys) {
            if (fk.keys.length == 1 && fk.keys[0] == fieldName &&
                (fk.refType == 'rPrimary' || fk.refType == 'rNullablePrimary')) {
                fkTable = fk.refTable;
                break;
            }
        }
        if (fkTable == null) {
            return;
        }
        const sTable = this.schema.getSTable(fkTable);
        if (sTable == null) {
            return;
        }

        const isValueInteger = isPkInteger(sTable)
        const isEnum = sTable.entryType == 'eEnum'
        const options = getIdOptions(sTable, isValueInteger && isEnum);

        return {options, isValueInteger, isEnum};
    }
}


function getItemTypeId(type: string, structural: SStruct | STable, fieldName: string) {
    if (type.startsWith("list<")) {  // list
        return type.substring(5, type.length - 1);
    } else if (type.startsWith("map<")) { //map
        return getMapEntryTypeName(structural, fieldName);
    }
}

function getImplNameOptions(sInterface: SInterface): EntityEditFieldOptions {
    const impls = [];
    for (const {name, comment} of sInterface.impls) {
        impls.push({value: name, label: name, labelstr: name, title: comment});
    }
    return {options: impls, isValueInteger: false, isEnum: true};
}

export interface ChainFold {
    chain: (string | number)[],
    fold: boolean
}

export class Folds {

    constructor(public list: ChainFold[]) {
    }

    setFold(chain: (string | number)[], fold: boolean): Folds {
        const f = this.isFold(chain);
        if (f === fold) {
            return this;
        }

        if (f === undefined) {
            return new Folds([...this.list, {chain, fold}]);
        }

        const newList: ChainFold[] = [];
        for (const c of this.list) {
            if (isChainEqual(c.chain, chain)) {
                newList.push({chain, fold});
            } else {
                newList.push(c);
            }
        }
        return new Folds(newList);
    }

    isFold(chain: (string | number)[]): boolean | undefined {
        for (const c of this.list) {
            if (isChainEqual(c.chain, chain)) {
                return c.fold;
            }
        }
    }
}

function isChainEqual(a: (string | number)[], b: (string | number)[]) {
    if (a === b) {
        return true;
    }
    if (a == null || b == null) {
        return false;
    }
    if (a.length !== b.length) {
        return false;
    }
    for (let i = 0; i < a.length; ++i) {
        if (a[i] !== b[i])
            return false;
    }
    return true;
}

/**
 * 判断字段是否可以内嵌显示
 * 条件1a: struct没有字段
 * 条件1b: struct只有一个primitive字段
 * 条件2: interface的impl没有字段
 * 条件3: interface的impl是defaultImpl且只有一个primitive字段
 */
function canBeEmbeddedCheck(fieldValue: JSONObject, sField: SField, schema: Schema): boolean {
    const fieldType = schema.itemIncludeImplMap.get(sField.type);
    if (!fieldType) return false;

    if (fieldType.type === 'struct') {
        const struct = fieldType as SStruct;
        // 条件1a: struct没有字段
        if (struct.fields.length === 0) {
            return true;
        }
        // 条件1b: struct只有一个primitive字段
        if (struct.fields.length === 1) {
            const onlyField = struct.fields[0];
            return isPrimitiveType(onlyField.type);
        }
        return false;
    }

    if (fieldType.type === 'interface') {
        const iface = fieldType as SInterface;
        const type = fieldValue['$type'] as string;
        const implName = getLastName(type);
        const impl = getImpl(iface, implName);
        if (!impl) return false;

        // 条件2: impl没有字段
        if (impl.fields.length === 0) {
            return true;
        }

        // 条件3: impl是defaultImpl且只有一个primitive字段
        if (implName === iface.defaultImpl && impl.fields.length === 1) {
            const onlyField = impl.fields[0];
            return isPrimitiveType(onlyField.type);
        }

        return false;
    }

    return false;
}

/**
 * 从struct中提取内嵌字段
 */
function getEmbeddedFieldFromStruct(struct: SStruct, obj: JSONObject): PrimitiveEditField | null {
    if (struct.fields.length !== 1) return null;

    const onlyField = struct.fields[0];
    const fieldValue = obj[onlyField.name];

    if (isPrimitiveType(onlyField.type)) {
        let v;
        if (fieldValue) {
            v = fieldValue as (boolean | number | string);
        } else if (onlyField.type === 'bool') {
            v = false;
        } else if (onlyField.type === 'str' || onlyField.type === 'text') {
            v = '';
        } else {
            v = 0;
        }

        return {
            name: onlyField.name,
            comment: onlyField.comment,
            type: 'primitive',
            eleType: onlyField.type as PrimitiveType,
            value: v,
        };
    }
    return null;
}

import {
    EditableEntity, Entity, PrimitiveValue,
    EntityEdgeType, EntityEdit,
    EntityEditField,
    EntityEditFieldOptions, EntityPosition,
    EntitySourceEdge,
    EntityType,
    PrimitiveType,
    PrimitiveEditField,
    StructRefEditField
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
// 导入新的embedding模块
import { canBeEmbeddedCheck } from "./embedding/embeddingChecker";
import { EmbeddingFieldExtractor } from "./embedding/embeddingFieldExtractor";
import { isPrimitiveType } from "./embedding/embeddingConfig";
// 导入Fold状态管理助手
import { FoldStateHelper } from "../../flow/embedded/FoldStateHelper";


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

                // 检查单元素list是否可以被内嵌
                let itemCanBeEmbedded = false;
                if (fArrLen === 1) {
                    const itemObj = fArr[0] as JSONObject;

                    // 构造临时SField用于内嵌检查（类型去掉list包装）
                    const tempSField: SField = {
                        ...sField,
                        type: itemTypeId, // 去掉list<...>包装
                    };

                    // 判断元素是否可以内嵌
                    if (canBeEmbeddedCheck(itemObj, tempSField, this.schema)) {
                        itemCanBeEmbedded = true;

                        // 读取元素的fold状态
                        let isItemFolded = this.folds.isFold([...fieldChain, fieldKey, 0]);
                        if (isItemFolded === undefined) {
                            // 本地状态没有设置时，读取元素的$fold字段
                            isItemFolded = itemObj['$fold'] as boolean | undefined;
                        }

                        if (isItemFolded !== false) {
                            // 元素被内嵌，不创建子entity，但标记有子节点（因为内嵌字段也算子节点）
                            hasChild = true;
                            continue;
                        }
                    }
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
                        },
                        itemCanBeEmbedded  // 传递canBeEmbedded标志
                    );
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
                    let isFolded = this.folds.isFold([...fieldChain, fieldKey]);
                    if (isFolded === undefined) {
                        // 本地状态没有设置时，读取对象的$fold字段
                        const fieldObj = fieldValue as JSONObject;
                        isFolded = fieldObj['$fold'] as boolean | undefined;
                    }
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
                        newObj['$fold'] = false;  // 明确设置为展开状态，避免自动内嵌
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
            const editField = this.createEditField(sf, obj, fieldChain, structural);
            if (editField) {
                fields.push(editField);
            }
        }
    }

    /**
     * 创建单个字段的编辑字段
     */
    private createEditField(
        sField: SField,
        obj: JSONObject,
        fieldChain: (string | number)[],
        structural: SStruct | STable
    ): EntityEditField | null {
        const fieldValue = obj[sField.name];

        if (isPrimitiveType(sField.type)) {
            return this.createPrimitiveEditField(sField, fieldValue, structural);
        }

        const itemTypeId = getItemTypeId(sField.type, structural, sField.name);
        if (itemTypeId != undefined) {
            // list or map
            return this.createListOrMapEditField(sField, fieldValue, itemTypeId, fieldChain, structural);
        } else {
            // struct or interface
            return this.createStructuralRefEditField(sField, fieldValue, fieldChain);
        }
    }

    /**
     * 创建原始类型编辑字段
     */
    private createPrimitiveEditField(
        sField: SField,
        fieldValue: unknown,
        structural: SStruct | STable
    ): PrimitiveEditField {
        const value = this.getPrimitiveValue(fieldValue, sField.type);

        return {
            name: sField.name,
            comment: sField.comment,
            type: 'primitive',
            eleType: sField.type as PrimitiveType,
            value,
            autoCompleteOptions: this.getAutoCompleteOptions(structural, sField.name),
        };
    }

    /**
     * 获取原始类型的值（带默认值处理）
     */
    private getPrimitiveValue(fieldValue: unknown, fieldType: string): PrimitiveValue {
        if (fieldValue) {
            return fieldValue as PrimitiveValue;
        }
        switch (fieldType) {
            case 'bool': return false;
            case 'str':
            case 'text': return '';
            default: return 0;
        }
    }

    /**
     * 创建list或map类型编辑字段
     */
    private createListOrMapEditField(
        sField: SField,
        fieldValue: unknown,
        itemTypeId: string,
        fieldChain: (string | number)[],
        structural: SStruct | STable
    ): EntityEditField {
        if (isPrimitiveType(itemTypeId)) {
            // list<primitive> or map<primitive>
            const v: PrimitiveValue[] = fieldValue ? fieldValue as PrimitiveValue[] : [];
            return {
                name: sField.name,
                comment: sField.comment,
                type: 'arrayOfPrimitive',
                eleType: itemTypeId as PrimitiveType,
                value: v,
                autoCompleteOptions: this.getAutoCompleteOptions(structural, sField.name),
            };
        } else {
            // list<struct> 或 list<interface>
            const v = fieldValue as JSONArray;

            // 尝试内嵌单元素list
            const embeddedField = this.tryCreateEmbeddedFieldForList(
                sField, v, itemTypeId, fieldChain
            );
            if (embeddedField) {
                return embeddedField;
            }

            // 非单元素list或不可内嵌，按现有逻辑处理（显示添加按钮）
            return {
                name: sField.name,
                comment: sField.comment,
                type: 'funcAdd',
                eleType: itemTypeId,
                value: (position: EntityPosition) => {
                    const sFieldable = this.schema.itemIncludeImplMap.get(itemTypeId) as SStruct | SInterface;
                    const defaultValue = this.schema.defaultValue(sFieldable);
                    onAddItemToArray(defaultValue, [...fieldChain, sField.name], position);
                }
            };
        }
    }

    /**
     * 尝试为list创建内嵌字段
     */
    private tryCreateEmbeddedFieldForList(
        sField: SField,
        v: JSONArray,
        itemTypeId: string,
        fieldChain: (string | number)[]
    ): StructRefEditField | null {
        if (!v || v.length !== 1) {
            return null;
        }

        const itemObj = v[0] as JSONObject;
        const itemType = this.schema.itemIncludeImplMap.get(itemTypeId);
        if (!itemType) {
            return null;
        }

        // 构造临时SField用于内嵌检查（类型去掉list包装）
        const tempSField: SField = {
            ...sField,
            type: itemTypeId, // 去掉list<...>包装
        };

        // 判断元素是否可以内嵌
        if (!canBeEmbeddedCheck(itemObj, tempSField, this.schema)) {
            return null;
        }

        // 读取元素的fold状态
        const isFolded = this.getFoldState([...fieldChain, sField.name, 0], itemObj);
        if (isFolded === false) {
            return null; // 展开状态，不内嵌
        }

        // 提取内嵌字段数据
        const embeddedData = this.extractEmbeddedFieldData(itemType, itemObj, [...fieldChain, sField.name, 0]);
        if (!embeddedData) {
            return null;
        }

        return {
            name: sField.name,
            comment: sField.comment,
            type: 'structRef',
            eleType: sField.type,
            value: '<>',
            handleOut: true,
            embeddedField: embeddedData,
        };
    }

    /**
     * 创建struct或interface引用编辑字段
     */
    private createStructuralRefEditField(
        sField: SField,
        fieldValue: unknown,
        fieldChain: (string | number)[]
    ): StructRefEditField {
        const fieldType = this.schema.itemIncludeImplMap.get(sField.type);
        if (!fieldType) {
            return {
                name: sField.name,
                comment: sField.comment,
                type: 'structRef',
                eleType: sField.type,
                value: '[]',
            };
        }

        const fieldObj = fieldValue as JSONObject;

        // 尝试内嵌
        const embeddedField = this.tryCreateEmbeddedFieldForStruct(sField, fieldObj, fieldType, fieldChain);
        if (embeddedField) {
            return embeddedField;
        }

        // 正常模式: 创建独立子节点
        return {
            name: sField.name,
            comment: sField.comment,
            type: 'structRef',
            eleType: sField.type,
            value: '[]',
        };
    }

    /**
     * 尝试为struct/interface创建内嵌字段
     */
    private tryCreateEmbeddedFieldForStruct(
        sField: SField,
        fieldObj: JSONObject,
        fieldType: SStruct | SInterface,
        fieldChain: (string | number)[]
    ): StructRefEditField | null {
        // 判断是否可以内嵌
        if (!canBeEmbeddedCheck(fieldObj, sField, this.schema)) {
            return null;
        }

        const isFolded = this.getFoldState([...fieldChain, sField.name], fieldObj);
        if (isFolded === false) {
            return null; // 展开状态，不内嵌
        }

        // 提取内嵌字段数据
        const embeddedData = this.extractEmbeddedFieldData(fieldType, fieldObj, [...fieldChain, sField.name]);
        if (!embeddedData) {
            return null;
        }

        return {
            name: sField.name,
            comment: sField.comment,
            type: 'structRef',
            eleType: sField.type,
            value: '<>',
            handleOut: true,
            embeddedField: embeddedData,
        };
    }

    /**
     * 提取内嵌字段数据
     */
    private extractEmbeddedFieldData(
        fieldType: SStruct | SInterface,
        obj: JSONObject,
        embeddedFieldChain: (string | number)[]
    ): { fields: Array<{value: PrimitiveValue; type: PrimitiveType; name: string; comment?: string}>; note?: string; implName?: string; embeddedFieldChain: (string | number)[] } | null {
        let embeddedFields: ReturnType<typeof getEmbeddedFieldValues> | null = null;
        let implNameToDisplay: string | undefined = undefined;

        if (fieldType.type === 'struct') {
            const struct = fieldType as SStruct;
            embeddedFields = getEmbeddedFieldValues(struct, obj);
        } else if (fieldType.type === 'interface') {
            const iface = fieldType as SInterface;
            const result = EmbeddingFieldExtractor.extractFromInterface(iface, obj);
            if (result) {
                embeddedFields = result.fields;
                implNameToDisplay = result.implName;
            }
        }

        if (!embeddedFields) {
            return null;
        }

        // 获取note
        const note = obj['$note'] as string | undefined;

        return {
            fields: embeddedFields,
            note: note,
            implName: implNameToDisplay,
            embeddedFieldChain: embeddedFieldChain,
        };
    }

    /**
     * 获取fold状态（优先本地状态，其次对象字段）
     * 使用FoldStateHelper统一管理
     */
    private getFoldState(fieldChain: (string | number)[], obj?: JSONObject): boolean | undefined {
        return FoldStateHelper.getFoldState(this.folds, fieldChain, obj);
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
 * 从struct或interface中提取内嵌字段的值
 * 使用新的embedding模块统一处理
 */
function getEmbeddedFieldValues(
    struct: SStruct,
    obj: JSONObject
): Array<{value: PrimitiveValue; type: PrimitiveType; name: string; comment?: string}> | null {
    return EmbeddingFieldExtractor.extractFromStruct(struct, obj);
}

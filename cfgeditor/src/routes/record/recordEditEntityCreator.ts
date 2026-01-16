import {
    EditableEntity, Entity, PrimitiveValue,
    EntityEdgeType, EntityEdit,
    EntityEditField,
    EntityEditFieldOptions, EntityPosition,
    EntitySourceEdge,
    EntityType,
    PrimitiveType
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

/**
 * 过滤掉空list字段
 * @param fields 字段定义数组
 * @param obj 实际数据对象
 * @returns 过滤后的字段数组
 */
function filterEmptyListFields(fields: SField[], obj: JSONObject): SField[] {
    return fields.filter(field => {
        // 检查是否为list类型
        if (!field.type.startsWith('list<')) {
            return true; // 非list类型，保留
        }

        // list类型，检查实际值是否为空数组
        const fieldValue = obj[field.name];
        if (!Array.isArray(fieldValue)) {
            return true; // 不是数组，保留
        }

        // 是空数组，过滤掉
        if (fieldValue.length === 0) {
            return false;
        }

        // 非空数组，保留
        return true;
    });
}

// 检查是否为number类型
function isNumberType(type: string): boolean {
    return ['int', 'long', 'float'].includes(type);
}

// 统计字段类型分布
function analyzeFieldTypes(fields: SField[]): {
    boolCount: number;
    numberCount: number;
    primitiveCount: number;
    allPrimitive: boolean;
} {
    let boolCount = 0;
    let numberCount = 0;
    let primitiveCount = 0;

    for (const field of fields) {
        if (isPrimitiveType(field.type)) {
            primitiveCount++;
            if (field.type === 'bool') boolCount++;
            if (isNumberType(field.type)) numberCount++;
        }
    }

    return {
        boolCount,
        numberCount,
        primitiveCount,
        allPrimitive: primitiveCount === fields.length,
    };
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
                        // list<struct> 或 list<interface>
                        const v = fieldValue as JSONArray;

                        // 检查是否为单元素list
                        if (v && v.length === 1) {
                            const itemObj = v[0] as JSONObject;
                            const itemType = this.schema.itemIncludeImplMap.get(itemTypeId);

                            if (itemType) {
                                // 构造临时SField用于内嵌检查（类型去掉list包装）
                                const tempSField: SField = {
                                    ...sf,
                                    type: itemTypeId, // 去掉list<...>包装
                                };

                                // 判断元素是否可以内嵌
                                if (canBeEmbeddedCheck(itemObj, tempSField, this.schema)) {
                                    // 读取元素的fold状态
                                    let isFolded = this.folds.isFold([...fieldChain, sf.name, 0]);
                                    if (isFolded === undefined) {
                                        // 本地状态没有设置时，读取元素的$fold字段
                                        isFolded = itemObj['$fold'] as boolean | undefined;
                                    }

                                    if (isFolded !== false) {
                                        // 内嵌模式
                                        let embeddedFields: ReturnType<typeof getEmbeddedFieldValues> | null = null;
                                        let implNameToDisplay: string | undefined = undefined;

                                        if (itemType.type === 'struct') {
                                            const struct = itemType as SStruct;
                                            embeddedFields = getEmbeddedFieldValues(struct, itemObj);
                                        } else if (itemType.type === 'interface') {
                                            const iface = itemType as SInterface;
                                            const type = itemObj['$type'] as string;
                                            const implName = getLastName(type);
                                            const impl = getImpl(iface, implName);

                                            if (impl) {
                                                embeddedFields = getEmbeddedFieldValues(impl, itemObj);

                                                // 记录implName（非defaultImpl时需要显示）
                                                if (implName !== iface.defaultImpl) {
                                                    implNameToDisplay = implName;
                                                }
                                            }
                                        }

                                        if (embeddedFields) {
                                            // 从元素获取note
                                            const note = itemObj['$note'] as string | undefined;

                                            fields.push({
                                                name: sf.name,
                                                comment: sf.comment,
                                                type: 'structRef',
                                                eleType: sf.type,
                                                value: '<>',
                                                handleOut: true,
                                                embeddedField: {
                                                    fields: embeddedFields,
                                                    note: note,
                                                    implName: implNameToDisplay,
                                                    embeddedFieldChain: [...fieldChain, sf.name, 0],  // 包含索引0
                                                },
                                            });
                                            continue;  // 跳过后续的funcAdd逻辑
                                        }
                                    }
                                }
                            }
                        }

                        // 非单元素list或不可内嵌，按现有逻辑处理（显示添加按钮）
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
                        let isFolded = this.folds.isFold([...fieldChain, sf.name]);
                        if (isFolded === undefined) {
                            // 本地状态没有设置时，读取对象的$fold字段
                            isFolded = fieldObj['$fold'] as boolean | undefined;
                        }

                        if (isFolded !== false) {
                            // fold=true 或 undefined，内嵌模式
                            let embeddedFields: ReturnType<typeof getEmbeddedFieldValues> | null = null;
                            let implNameToDisplay: string | undefined = undefined;

                            if (fieldType.type === 'struct') {
                                const struct = fieldType as SStruct;
                                embeddedFields = getEmbeddedFieldValues(struct, fieldObj);
                            } else if (fieldType.type === 'interface') {
                                const iface = fieldType as SInterface;
                                const type = fieldObj['$type'] as string;
                                const implName = getLastName(type);
                                const impl = getImpl(iface, implName);

                                if (impl) {
                                    embeddedFields = getEmbeddedFieldValues(impl, fieldObj);

                                    // 记录implName（非defaultImpl时需要显示）
                                    if (implName !== iface.defaultImpl) {
                                        implNameToDisplay = implName;
                                    }
                                }
                            }

                            if (embeddedFields) {
                                // 直接从 obj 获取 note
                                const note = fieldObj['$note'] as string | undefined;

                                fields.push({
                                    name: sf.name,
                                    comment: sf.comment,
                                    type: 'structRef',
                                    eleType: sf.type,
                                    value: '<>',
                                    handleOut: true,
                                    embeddedField: {
                                        fields: embeddedFields,
                                        note: note,
                                        implName: implNameToDisplay,
                                        embeddedFieldChain: [...fieldChain, sf.name],
                                    },
                                });
                                continue;
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
 * 条件1b: struct只有1个primitive字段
 * 条件1c: struct只有≤3个number字段
 * 条件1d: struct只有≤4个bool字段
 * 条件1e: struct只有1个bool和1个number字段
 * 条件2a: impl没有字段
 * 条件2b: impl只有1个primitive字段
 * 条件2c: impl只有≤2个number字段
 * 条件2d: impl只有≤3个bool字段
 * 条件2e: impl只有1个bool和1个number字段
 */
function canBeEmbeddedCheck(fieldValue: JSONObject, sField: SField, schema: Schema): boolean {
    const fieldType = schema.itemIncludeImplMap.get(sField.type);
    if (!fieldType) return false;

    if (fieldType.type === 'struct') {
        const struct = fieldType as SStruct;
        // 先过滤空list字段
        const filteredFields = filterEmptyListFields(struct.fields, fieldValue);
        const analysis = analyzeFieldTypes(filteredFields);

        // 条件1a: struct没有字段（过滤后）
        if (filteredFields.length === 0) {
            return true;
        }

        // 条件1b: struct只有1个primitive字段
        if (filteredFields.length === 1 && analysis.allPrimitive) {
            return true;
        }

        // 条件1c: struct只有≤3个number字段
        if (filteredFields.length <= 3 &&
            filteredFields.length === analysis.numberCount &&
            analysis.allPrimitive) {
            return true;
        }

        // 条件1d: struct只有≤4个bool字段
        if (filteredFields.length <= 4 &&
            filteredFields.length === analysis.boolCount &&
            analysis.allPrimitive) {
            return true;
        }

        // 条件1e: struct只有1个bool和1个number字段
        if (filteredFields.length === 2 &&
            analysis.boolCount === 1 &&
            analysis.numberCount === 1 &&
            analysis.allPrimitive) {
            return true;
        }

        return false;
    }

    if (fieldType.type === 'interface') {
        const iface = fieldType as SInterface;
        const type = fieldValue['$type'] as string;
        const implName = getLastName(type);
        const impl = getImpl(iface, implName);
        if (!impl) return false;

        // 先过滤空list字段
        const filteredFields = filterEmptyListFields(impl.fields, fieldValue);
        const analysis = analyzeFieldTypes(filteredFields);

        // 条件2a: impl没有字段（过滤后）
        if (filteredFields.length === 0) {
            return true;
        }

        // 条件2b: impl只有1个primitive字段
        if (filteredFields.length === 1 && analysis.allPrimitive) {
            return true;
        }

        // 条件2c: impl只有≤2个number字段
        if (filteredFields.length <= 2 &&
            filteredFields.length === analysis.numberCount &&
            analysis.allPrimitive) {
            return true;
        }

        // 条件2d: impl只有≤3个bool字段
        if (filteredFields.length <= 3 &&
            filteredFields.length === analysis.boolCount &&
            analysis.allPrimitive) {
            return true;
        }

        // 条件2e: impl只有1个bool和1个number字段
        if (filteredFields.length === 2 &&
            analysis.boolCount === 1 &&
            analysis.numberCount === 1 &&
            analysis.allPrimitive) {
            return true;
        }

        return false;
    }

    return false;
}

/**
 * 从struct中提取内嵌字段的值
 * @returns 返回内嵌字段的值数组，不可内嵌时返回null
 */
function getEmbeddedFieldValues(
    struct: SStruct,
    obj: JSONObject
): Array<{value: PrimitiveValue; type: PrimitiveType; name: string; comment?: string}> | null {
    // 先过滤空list字段
    const filteredFields = filterEmptyListFields(struct.fields, obj);

    if (filteredFields.length === 0) {
        // 条件1a/2a: 过滤后没有字段，返回空数组
        return [];
    }

    const analysis = analyzeFieldTypes(filteredFields);

    // 单字段情况（条件1b, 2b）
    if (filteredFields.length === 1 && analysis.allPrimitive) {
        const onlyField = filteredFields[0];
        const fieldValue = obj[onlyField.name];

        let v: PrimitiveValue;
        if (fieldValue !== undefined && fieldValue !== null) {
            v = fieldValue as PrimitiveValue;
        } else if (onlyField.type === 'bool') {
            v = false;
        } else if (onlyField.type === 'str' || onlyField.type === 'text') {
            v = '';
        } else {
            v = 0;
        }

        return [{
            value: v,
            type: onlyField.type as PrimitiveType,
            name: onlyField.name,
            comment: onlyField.comment,
        }];
    }

    // 多字段情况（条件1c, 1d, 1e, 2c, 2d, 2e）
    if (analysis.allPrimitive) {
        // struct的条件：≤3个number, ≤4个bool, 1个bool+1个number
        const isStructNumberFields = filteredFields.length === analysis.numberCount && filteredFields.length <= 3;
        const isStructBoolFields = filteredFields.length === analysis.boolCount && filteredFields.length <= 4;
        const isStructBoolAndNumber = filteredFields.length === 2 &&
                                      analysis.boolCount === 1 &&
                                      analysis.numberCount === 1;

        // interface的条件：≤2个number, ≤3个bool, 1个bool+1个number
        const isInterfaceNumberFields = filteredFields.length === analysis.numberCount && filteredFields.length <= 2;
        const isInterfaceBoolFields = filteredFields.length === analysis.boolCount && filteredFields.length <= 3;
        const isInterfaceBoolAndNumber = filteredFields.length === 2 &&
                                         analysis.boolCount === 1 &&
                                         analysis.numberCount === 1;

        // 满足任一条件即可内嵌
        if (isStructNumberFields || isStructBoolFields || isStructBoolAndNumber ||
            isInterfaceNumberFields || isInterfaceBoolFields || isInterfaceBoolAndNumber) {

            const fields: Array<{value: PrimitiveValue; type: PrimitiveType; name: string; comment?: string}> = [];

            for (const field of filteredFields) {
                const fieldValue = obj[field.name];

                let v: PrimitiveValue;
                if (fieldValue !== undefined && fieldValue !== null) {
                    v = fieldValue as PrimitiveValue;
                } else if (field.type === 'bool') {
                    v = false;
                } else if (field.type === 'str' || field.type === 'text') {
                    v = '';
                } else {
                    v = 0;
                }

                fields.push({
                    value: v,
                    type: field.type as PrimitiveType,
                    name: field.name,
                    comment: field.comment,
                });
            }

            return fields;
        }
    }

    return null;
}

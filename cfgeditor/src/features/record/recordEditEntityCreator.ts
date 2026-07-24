import {
    EditableEntity, Entity, PrimitiveValue,
    EntityEdgeType, EntityEdit,
    EntityEditField,
    EntityEditFieldOptions, EntityPosition,
    EntitySourceEdge,
    EntityType,
    PrimitiveEditField,
    StructRefEditField
} from "@/domain/entityModel.ts";
import {isPrimitiveType, PrimitiveType, SField, SInterface, SItem, SStruct, STable} from "@/api/schemaModel.ts";
import {JSONArray, JSONObject, JSONValue, RefId} from "@/api/recordModel.ts";
import {getId, getLabel, getLastName} from "./recordRefUtils.ts";
import {getField, getImpl, getMapEntryTypeName, isPkInteger, Schema} from "@/domain/schema.ts";
import {EditingSession} from "@/services/editingSession.ts";
import {canBeEmbeddedCheck, classifyListField, extractEmbeddingFields, getEmbedState} from "@/domain/embedding.ts";
import {getIdOptions} from "@/flow/edit/shared/idOptions.tsx";


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
                public session: EditingSession,
                public editingObject: JSONObject,
    ) {
        this.curRefId = {table: curTable.name, id: curId};
    }

    createThis() {
        const id = getId(this.curTable.name, this.curId);
        this.createEntity(id, this.curTable, this.editingObject, []);
    }

    createEntity(id: string,
                 sItem: SItem,
                 obj: JSONObject,
                 fieldChain: (string | number)[],
                 arrayItemParam?: ArrayItemParam): EditableEntity | null {

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
        const fold = this.getFoldState(obj);

        let hasChild: boolean = false;

        const editFields: EntityEditField[] = this.makeEditFields(sItem, obj, fieldChain);
        const sourceEdges: EntitySourceEdge[] = [];


        for (const fieldKey in obj) {
            if (fieldKey.startsWith("$")) {
                continue;
            }
            const fieldValue: JSONValue = obj[fieldKey];
            const ft = typeof fieldValue
            if (ft != 'object' || fieldValue === null) {  // typeof null === 'object'，需额外排除
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

                // list 字段 embed 分类（与 createListOrMapEditField 共用 domain.classifyListField）：
                // embedTag / summary 都不建子 entity、不 push sourceEdge（内嵌字段也算有子节点）
                if (classifyListField(fArr, itemType, getEmbedState(obj, fieldKey)) !== 'nodes') {
                    hasChild = true;
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
                    const onDeleteFunc = () => {
                        // 锚点取父节点（id = 当前 struct，即 list 父）：被删 item 正向已消失、undo 前不存在，
                        // 父节点两个方向都在 → 正向删除与 undo 都是 KeepStable 锚定父节点，其屏幕不动。
                        // itemType（list 元素类型）传入 session；删到恰剩 1 时 normalizeOnDelete 自调
                        // canBeEmbeddedCheck 判幸存元素可内嵌性（判定在 domain，不在 session/creator）
                        this.session.deleteArrayItem(arrayIndex, chain, id, itemType);
                    }

                    let onMoveUpFunc;
                    if (arrayIndex > 0) {
                        onMoveUpFunc = (position: EntityPosition) => {
                            this.session.swapArrayItem(arrayIndex, arrayIndex - 1, chain, position);
                        }
                    }

                    let onMoveDownFunc;
                    if (arrayIndex < fArrLen - 1) {
                        onMoveDownFunc = (position: EntityPosition) => {
                            this.session.swapArrayItem(arrayIndex, arrayIndex + 1, chain, position);
                        }
                    }


                    const childEntity = this.createEntity(
                        childId, itemType, itemObj,
                        [...fieldChain, fieldKey, arrayIndex],
                        {
                            onDeleteFunc,
                            onMoveUpFunc,
                            onMoveDownFunc,
                        }
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

                // 检查是否为内嵌模式（类 1：可内嵌且父对象上 `$embed_<fieldName> !== false`）
                const canEmbed = canBeEmbeddedCheck(fieldValue as JSONObject, fieldType);

                if (canEmbed) {
                    if (getEmbedState(obj, fieldKey) !== false) {
                        // 内嵌模式，不创建子节点
                        continue;
                    }
                    // $embed=false，展开模式，继续创建子节点
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


        const editOnUpdateValues = (changed: Record<string, unknown>, allValues: Record<string, unknown>) => {
                this.session.updateFormValues(this.schema, allValues, fieldChain, changed);
            }
        ;

        const editOnUpdateNote = (note?: string) => {
            this.session.updateNote(note, fieldChain);
        };

        const editOnUpdateFold = (fold: boolean, position: EntityPosition) => {
            // 节点级 fold：$fold 单义（折叠我自己的子节点）。updateFold 写/删 obj.$fold +
            // structureChange 驱动重渲读新 $fold。
            this.session.updateFold(fold, fieldChain, position);
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
            const impl = getImpl(sInterface, implName);
            fields.push({
                name: '$impl',
                comment: sItem.comment,
                type: 'interface',
                eleType: sInterface.name,
                value: implName,
                autoCompleteOptions: getImplNameOptions(sInterface),
                implFields: impl ? this.makeEditFields(impl, obj, fieldChain) : [],
                interfaceOnChangeImpl: (newImplName: string, position: EntityPosition) => {
                    let newObj: JSONObject;
                    if (newImplName == implName) {
                        newObj = obj;
                    } else {
                        const newImpl = getImpl(sInterface, newImplName);
                        if (!newImpl) {
                            return;  // 目标 impl 不存在，放弃切换
                        }
                        newObj = this.schema.defaultValueOfStructural(newImpl);
                    }
                    // embed 状态在父对象上、随 obj 替换天然存活；归一化（可内嵌→写 false 保持展开 /
                    // 不可内嵌→删残留键）由 normalizeOnImplSwitch 自调 canBeEmbeddedCheck，传 sInterface 即可
                    this.session.updateInterfaceValue(newObj, fieldChain, position, sInterface);
                },
            })

        } else {
            const structural = sItem as (SStruct | STable);
            this.makeStructuralEditFields(fields, structural, obj, fieldChain);

            const funcClear = () => {
                const defaultValue = this.schema.defaultValueOfStructural(structural);
                this.session.replaceEditingObject(defaultValue);
            };

            if ('pk' in structural) { // is STable
                fields.push({
                    name: '$submit',
                    comment: '',
                    type: 'funcSubmit',
                    eleType: 'bool',
                    value: {
                        funcSubmit: () => this.session.submit(),
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
            return this.createListOrMapEditField(sField, fieldValue, itemTypeId, obj, fieldChain, structural);
        } else {
            // struct or interface
            return this.createStructuralRefEditField(sField, fieldValue, obj, fieldChain);
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
            case 'bool':
                return false;
            case 'str':
            case 'text':
                return '';
            default:
                return 0;
        }
    }

    /**
     * 创建list或map类型编辑字段
     */
    private createListOrMapEditField(
        sField: SField,
        fieldValue: unknown,
        itemTypeId: string,
        obj: JSONObject,
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
            const v = (fieldValue ?? []) as JSONArray;
            const itemType = this.schema.itemIncludeImplMap.get(itemTypeId);

            // list 字段 embed 分类（与 createEntity 子节点循环共用 classifyListField）：
            // 类 1 → 内嵌 Tag；类 2 嵌入态 → 摘要行；否则 funcAdd 展开态
            const cls = itemType ? classifyListField(v, itemType, getEmbedState(obj, sField.name)) : 'nodes';
            if (itemType && cls === 'embedTag') {
                const embeddedField = this.createEmbeddedListField(sField, v, itemType, fieldChain);
                if (embeddedField) {
                    return embeddedField;
                }
            }
            const embedded = cls === 'summary';

            // 非单元素list或不可内嵌或已嵌入，按现有逻辑处理（显示添加按钮 + 嵌入口）
            return {
                name: sField.name,
                comment: sField.comment,
                type: 'funcAdd',
                eleType: itemTypeId,
                value: (position: EntityPosition) => {
                    const sFieldable = this.schema.itemIncludeImplMap.get(itemTypeId) as SStruct | SInterface;
                    const defaultValue = this.schema.defaultValue(sFieldable);
                    // 0→1 且可内嵌时 session 写 $embed=false（原 markNewItemExpanded 语义）：
                    // 新元素默认展开成节点，立即可编辑。可内嵌判定由 normalizeOnAdd 自调 canBeEmbeddedCheck，
                    // 传 sFieldable（itemType）即可
                    this.session.addArrayItem(defaultValue, [...fieldChain, sField.name], position, sFieldable);
                },
                listEmbed: {
                    embedded,
                    itemCount: v.length,
                    onUpdateListEmbed: (embed: boolean, position: EntityPosition) => {
                        if (embed) {
                            // 类 1（恰 1 元素可内嵌）的收起 = 默认内嵌 ⇒ 删键即可，不写 true 残留
                            //（不变式：键只存当前类的非默认值）
                            const class1 = itemType != null && v.length === 1
                                && canBeEmbeddedCheck(v[0] as JSONObject, itemType);
                            if (class1) {
                                this.session.deleteEmbed(sField.name, fieldChain, position);
                            } else {
                                this.session.updateEmbed(true, sField.name, fieldChain, position);
                            }
                        } else {
                            this.session.deleteEmbed(sField.name, fieldChain, position);
                        }
                    },
                },
            };
        }
    }

    /**
     * 创建单元素 list 的内嵌字段（类 1 渲染载体；调用前已由 classifyListField 判出 'embedTag'，
     * 此处只剩 extract 失败（resolveImpl 脏数据等）的兜底 null）
     */
    private createEmbeddedListField(
        sField: SField,
        v: JSONArray,
        itemType: SStruct | SInterface,
        fieldChain: (string | number)[]
    ): StructRefEditField | null {
        const embeddedData = this.extractEmbeddedFieldData(itemType, v[0] as JSONObject);
        if (!embeddedData) {
            return null;
        }

        return {
            name: sField.name,
            comment: sField.comment,
            type: 'structRef',
            eleType: sField.type,
            handleOut: true,
            embeddedField: embeddedData,
            expandEmbedded: (position: EntityPosition) =>
                this.session.updateEmbed(false, sField.name, fieldChain, position),
        };
    }

    /**
     * 创建struct或interface引用编辑字段
     */
    private createStructuralRefEditField(
        sField: SField,
        fieldValue: unknown,
        obj: JSONObject,
        fieldChain: (string | number)[]
    ): StructRefEditField {
        const base = {
            name: sField.name,
            comment: sField.comment,
            type: 'structRef',
            eleType: sField.type,
        } as const;

        const fieldType = this.schema.itemIncludeImplMap.get(sField.type);
        if (!fieldType) {
            return {...base};
        }

        const fieldObj = fieldValue as JSONObject;
        const canEmbed = canBeEmbeddedCheck(fieldObj, fieldType);

        // 可内嵌且未显式展开（父对象上 `$embed_<fieldName> !== false`）→ 内嵌成一行 Tag
        if (canEmbed && getEmbedState(obj, sField.name) !== false) {
            const embeddedData = this.extractEmbeddedFieldData(fieldType, fieldObj);
            if (embeddedData) {
                return {
                    ...base,
                    handleOut: true,
                    embeddedField: embeddedData,
                    expandEmbedded: (position: EntityPosition) =>
                        this.session.updateEmbed(false, sField.name, fieldChain, position),
                };
            }
        }

        // 正常模式: 创建独立子节点。可内嵌的子结构在占位行上挂回嵌入口（点击删 $embed 键回到内嵌态）
        return {
            ...base,
            reEmbed: canEmbed
                ? (position: EntityPosition) => this.session.deleteEmbed(sField.name, fieldChain, position)
                : undefined,
        };
    }

    /**
     * 提取内嵌字段数据
     */
    private extractEmbeddedFieldData(
        fieldType: SStruct | SInterface,
        obj: JSONObject
    ): {
        fields: Array<{ value: PrimitiveValue; type: PrimitiveType; name: string; comment?: string }>;
        note?: string;
        implName?: string
    } | null {
        const result = extractEmbeddingFields(fieldType, obj);
        if (!result) {
            return null;
        }

        const {embeddedFields, implNameToDisplay} = result;
        // 获取note
        const note = obj['$note'] as string | undefined;

        return {
            fields: embeddedFields,
            note: note,
            implName: implNameToDisplay,
        };
    }

    /**
     * 获取节点级 fold 状态：直接读 obj.$fold（持久化层，单义：折叠我自己的子节点）。
     * undo/redo 恢复 $fold 即恢复 fold。无 obj 或无 $fold → undefined（视为未折叠）。
     */
    private getFoldState(obj?: JSONObject): boolean | undefined {
        return obj?.['$fold'] as boolean | undefined;
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

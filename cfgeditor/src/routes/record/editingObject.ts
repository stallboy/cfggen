import {JSONArray, JSONObject, RecordResult} from "./recordModel.ts";
import {SItem, SStruct, STable} from "../table/schemaModel.ts";
import {getField, Schema} from "../table/schemaUtil.tsx";
import {EntityPosition} from "../../flow/entityModel.ts";
import {setEditingState} from "../../store/store.ts";

// import {doEdit} from "../setting/store.ts";

export enum EFitView {
    FitFull,
    FitId,
    FitNone,
}

export type EditState = {
    table: string;
    id: string;
    originalEditingObject: JSONObject;
    editingObject: JSONObject;
    fitView: EFitView;
    fitViewToIdPosition?: EntityPosition;

    update: () => void;
    submitEditingObject: () => void;
    isEdited: boolean;

    copiedObject: JSONObject;
}

const dummyFunc = () => {
};

export const editState: EditState = {
    table: '',
    id: '',
    originalEditingObject: {'$type': ''},
    editingObject: {'$type': ''},
    fitView: EFitView.FitFull,

    update: dummyFunc,
    submitEditingObject: dummyFunc,
    isEdited: false,

    copiedObject: {'$type': ''},
};

export type EditingObjectRes = {
    fitView: EFitView;
    fitViewToIdPosition?: EntityPosition;
    isEdited: boolean;   // 是否要重新计算节点的layout
}


export function startEditingObject(recordResult: RecordResult,
                                   update: () => void,
                                   submitEditingObject: () => void): EditingObjectRes {
    editState.update = function () {
        editState.isEdited = true;
        notifyEditingState()
        update();
    }
    editState.submitEditingObject = submitEditingObject;
    const {table, id, fitView, fitViewToIdPosition} = editState;
    const {table: newTable, id: newId} = recordResult;
    if (newTable == table && newId == id) {
        const newEditingObject = structuredClone(recordResult.object);
        delete$refInPlace(newEditingObject);

        if (isDeeplyEqual(editState.originalEditingObject, newEditingObject)){
            return {
                fitView,
                fitViewToIdPosition,
                isEdited: editState.isEdited
            };
        }
    }

    const newEditingObject = structuredClone(recordResult.object);
    delete$refInPlace(newEditingObject);
    // console.log("start editing new", newEditingObject)
    editState.table = newTable;
    editState.id = newId;
    editState.fitView = EFitView.FitFull;
    editState.fitViewToIdPosition = undefined;
    editState.originalEditingObject = structuredClone(newEditingObject);
    editState.editingObject = newEditingObject;
    editState.isEdited = false;
    notifyEditingState();
    return {
        fitView: editState.fitView,
        fitViewToIdPosition: editState.fitViewToIdPosition,
        isEdited: editState.isEdited
    };
}


export function notifyEditingState() {
    setEditingState(editState.table, editState.id, !isDeeplyEqual(editState.editingObject, editState.originalEditingObject));
}

export function onUpdateFormValues(schema: Schema,
                                   values: Record<string, unknown>,
                                   fieldChains: (string | number)[]) {
    // console.log('formChange', fieldChains, values);

    const obj = getFieldObj(editState.editingObject, fieldChains);
    const name = obj['$type'] as string;
    const sItem = schema.itemIncludeImplMap.get(name);

    for (const key in values) {
        if (key.startsWith("$")) { // $impl
            continue;
        }
        const conv = getFieldPrimitiveTypeConverter(key, sItem);

        const fieldValue = values[key];
        if (Array.isArray(fieldValue)) {
            // antd form 会返回[undefined, .. ], 这里忽略掉undefined 的item
            const fArr: JSONArray = fieldValue as JSONArray;
            const newArr = [];
            for (const fArrElement of fArr) {
                if (fArrElement != undefined) {
                    newArr.push(conv(fArrElement));
                }
            }
            obj[key] = newArr;
        } else {
            obj[key] = conv(fieldValue);
        }
    }
    // 当在单个node的form里更改时，因为ui已经更改，不需要再触发Record的更新。
    notifyEditingState();  // 触发'更改提示'
}

export function onUpdateNote(note: string | undefined,
                             fieldChains: (string | number)[]) {
    const obj = getFieldObj(editState.editingObject, fieldChains);
    obj['$note'] = note;
    notifyEditingState();
}


export function onUpdateFold(fold: boolean,
                             fieldChains: (string | number)[],
                             position: EntityPosition) {
    const obj = getFieldObj(editState.editingObject, fieldChains);
    obj['$fold'] = fold;

    editState.fitView = EFitView.FitId;
    editState.fitViewToIdPosition = position;
    editState.update();
}


export function onUpdateInterfaceValue(jsonObject: JSONObject,
                                       fieldChains: (string | number)[],
                                       position: EntityPosition) {
    // console.log('updateInterface', fieldChains, jsonObject);

    const obj = getFieldObj(editState.editingObject, fieldChains.slice(0, fieldChains.length - 1));
    obj[fieldChains[fieldChains.length - 1]] = jsonObject;

    editState.fitView = EFitView.FitId;
    editState.fitViewToIdPosition = position;
    editState.update();
}


export function onAddItemToArray(defaultItemJsonObject: JSONObject,
                                 arrayFieldChains: (string | number)[],
                                 position: EntityPosition) {
    // console.log('addItem', arrayFieldChains, defaultItemJsonObject);

    const obj = getFieldObj(editState.editingObject, arrayFieldChains) as JSONArray;
    obj.push(defaultItemJsonObject);


    editState.fitView = EFitView.FitId;
    editState.fitViewToIdPosition = position;
    editState.update();
}

// 前插入：让插入的节点和当前鼠标所在节点 位置不变
export function onAddItemToArrayIndex(defaultItemJsonObject: JSONObject,
                                      index: number,
                                      arrayFieldChains: (string | number)[],
                                      position: EntityPosition) {
    const obj = getFieldObj(editState.editingObject, arrayFieldChains) as JSONArray;
    obj.splice(index, 0, defaultItemJsonObject);

    editState.fitView = EFitView.FitId;
    editState.fitViewToIdPosition = position;
    editState.update();
}


export function onMoveItemInArray(curIndex: number,
                                  newIndex: number,
                                  arrayFieldChains: (string | number)[],
                                  position: EntityPosition) {
    const obj = getFieldObj(editState.editingObject, arrayFieldChains) as JSONArray;
    const o2 = obj[newIndex];
    obj[newIndex] = obj[curIndex]
    obj[curIndex] = o2;

    editState.fitView = EFitView.FitId;
    editState.fitViewToIdPosition = position;
    editState.update();
}


export function onDeleteItemFromArray(deleteIndex: number,
                                      arrayFieldChains: (string | number)[],
                                      position: EntityPosition) {
    // console.log('delItem', arrayFieldChains, deleteIndex);

    const obj = getFieldObj(editState.editingObject, arrayFieldChains) as JSONArray;
    obj.splice(deleteIndex, 1);

    editState.fitView = EFitView.FitId;
    editState.fitViewToIdPosition = position;
    editState.update();
}

export function applyNewEditingObject(newEditingObject: JSONObject) {
    editState.editingObject = newEditingObject;

    // editState.seq++;
    editState.fitView = EFitView.FitFull;
    editState.update();
}

export function onStructCopy(obj: JSONObject) {
    editState.copiedObject = structuredClone(obj);
}

export function isCopiedFitAllowedType(allowdType: string) {
    const type = editState.copiedObject.$type;
    if (type == allowdType) {
        return true;
    }

    if (type.startsWith(allowdType)) {
        return type[allowdType.length] == '.'  //简单判断，没有去查询interface和impl
    }

    return false;
}

export function onStructPaste(fieldChains: (string | number)[],
                              position: EntityPosition) {
    let obj = editState.editingObject;
    const copied = editState.copiedObject;

    let i = 0;
    const len = fieldChains.length;
    for (const field of fieldChains) {

        if (i == len - 1) {
            obj[field] = structuredClone(copied);
        } else {
            obj = obj[field] as JSONObject;  // as 只是为了跳过ts类型检查
        }
        i++;
    }

    editState.fitView = EFitView.FitId;
    editState.fitViewToIdPosition = position;
    editState.update();
}

function getFieldObj(editingObject: JSONObject, fieldChains: (string | number)[]): any {
    let obj: any = editingObject;
    for (const field of fieldChains) {
        obj = obj[field];  // as 只是为了跳过ts类型检查
    }
    return obj;
}

function getFieldPrimitiveTypeConverter(fieldName: string, sItem?: SItem): ((value: any) => any) {
    if (sItem) {
        const structural = sItem as SStruct | STable;
        const field = getField(structural, fieldName);
        if (field) {
            const ft = field.type;
            if (ft == 'int') {
                return toInt;
            } else if (ft == 'long' || ft == 'float') {
                return toFloat;
            } else if (ft.startsWith('list<')) {
                const itemType = ft.slice(5, ft.length - 1);
                if (itemType == 'int') {
                    return toInt;
                } else if (itemType == 'long' || itemType == 'float') {
                    return toFloat;
                }
            }
        }
    }
    return same;
}

function same(value: any) {
    return value;
}

function toInt(value: any) {
    if (typeof value == 'string') {
        return parseInt(value);
    } else {
        return value;
    }
}

function toFloat(value: any) {
    if (typeof value == 'string') {
        return parseFloat(value);
    } else {
        return value;
    }
}

function isDeeplyEqual(obj1: any, obj2: any): boolean {
    if (obj1 === obj2) return true;

    if (Array.isArray(obj1) && Array.isArray(obj2)) {
        if (obj1.length !== obj2.length) return false;
        return obj1.every((elem, index) => {
            return isDeeplyEqual(elem, obj2[index]);
        })
    }

    if (typeof obj1 === "object" && typeof obj2 === "object" && obj1 !== null && obj2 !== null) {
        if (Array.isArray(obj1) || Array.isArray(obj2)) return false;
        const keys1 = Object.keys(obj1)
        const keys2 = Object.keys(obj2)
        if (keys1.length !== keys2.length || !keys1.every(key => keys2.includes(key))) return false;

        for (let key in obj1) {
            let isEqual = isDeeplyEqual(obj1[key], obj2[key])
            if (!isEqual) {
                return false;
            }
        }
        return true;
    }

    return false;
}

function delete$refInPlace(obj: any) {
    if (typeof obj === "object") {
        delete obj['$refs'];
        for (const k in obj) {
            delete$refInPlace(obj[k]);
        }
    } else if (Array.isArray(obj)) {
        for (const item of obj.values()) {
            delete$refInPlace(item);
        }
    }
}
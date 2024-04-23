import {JSONArray, JSONObject, RecordResult} from "./recordModel.ts";
import {SItem, SStruct, STable} from "../table/schemaModel.ts";
import {getField, Schema} from "../table/schemaUtil.ts";

export type EditState = {
    table: string;
    id: string;
    editingObject: JSONObject;
    seq: number;  // 触发layout
    fitView: boolean;

    afterEditStateChanged: () => void;
    submitEditingObject: () => void;
}

export const editState: EditState = {
    table: '',
    id: '',
    editingObject: {'$type': ''},
    seq: 0,
    fitView: true,
    afterEditStateChanged: () => {
    },
    submitEditingObject: () => {
    },
};


export function startEditingObject(recordResult: RecordResult,
                                   afterEditStateChanged: () => void,
                                   submitEditingObject: () => void): [number, boolean] {
    editState.afterEditStateChanged = afterEditStateChanged;
    editState.submitEditingObject = submitEditingObject;
    const {table, id, seq, fitView} = editState;
    const {table: newTable, id: newId} = recordResult;
    if (newTable == table && newId == id) {
        return [seq, fitView];
    }

    const clone: JSONObject = {...recordResult.object}
    delete clone['$refs'];  // inner $ref not deleted
    const newEditingObject = structuredClone(clone);
    editState.table = newTable;
    editState.id = newId;
    editState.seq = 0;
    editState.fitView = true;
    editState.editingObject = newEditingObject;
    return [0, true];
}


export function onUpdateFormValues(schema: Schema,
                                   values: any,
                                   fieldChains: (string | number)[]) {
    // console.log('formChange', fieldChains, values);

    let obj = getFieldObj(editState.editingObject, fieldChains);
    let name = obj['$type'] as string;
    let sItem = schema.itemIncludeImplMap.get(name);

    for (let key in values) {
        if (key.startsWith("$")) { // $impl
            continue;
        }
        let conv = getFieldPrimitiveTypeConverter(key, sItem);

        let fieldValue = values[key];
        if (Array.isArray(fieldValue)) {
            // antd form 会返回[undefined, .. ], 这里忽略掉undefined 的item
            let fArr: JSONArray = fieldValue as JSONArray;
            let newArr = [];
            for (let fArrElement of fArr) {
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
}


export function onUpdateInterfaceValue(jsonObject: JSONObject,
                                       fieldChains: (string | number)[]) {
    // console.log('updateInterface', fieldChains, jsonObject);

    let obj = getFieldObj(editState.editingObject, fieldChains.slice(0, fieldChains.length - 1));
    obj[fieldChains[fieldChains.length - 1]] = jsonObject;

    editState.seq++;
    editState.fitView = false;
    editState.afterEditStateChanged();
}


export function onAddItemForArray(defaultItemJsonObject: JSONObject,
                                  arrayFieldChains: (string | number)[]) {
    // console.log('addItem', arrayFieldChains, defaultItemJsonObject);

    let obj = getFieldObj(editState.editingObject, arrayFieldChains) as JSONArray;
    obj.push(defaultItemJsonObject);

    editState.seq++;
    editState.fitView = false;
    editState.afterEditStateChanged();
}


export function onMoveItemFromArray(curIndex: number,
                                    newIndex: number,
                                    arrayFieldChains: (string | number)[]) {
    // console.log('delItem', arrayFieldChains, deleteIndex);

    let obj = getFieldObj(editState.editingObject, arrayFieldChains) as JSONArray;
    let o2 = obj[newIndex];
    obj[newIndex] = obj[curIndex]
    obj[curIndex] = o2;

    editState.seq++;
    editState.fitView = false;
    editState.afterEditStateChanged();
}


export function onDeleteItemFromArray(deleteIndex: number,
                                      arrayFieldChains: (string | number)[]) {
    // console.log('delItem', arrayFieldChains, deleteIndex);

    let obj = getFieldObj(editState.editingObject, arrayFieldChains) as JSONArray;
    obj.splice(deleteIndex, 1);

    editState.seq++;
    editState.fitView = false;
    editState.afterEditStateChanged();
}

export function applyNewEditingObject(newEditingObject: JSONObject) {
    editState.editingObject = newEditingObject;

    editState.seq++;
    editState.fitView = true;
    editState.afterEditStateChanged();
}

function getFieldObj(editingObject: JSONObject, fieldChains: (string | number)[]): any {
    let obj: any = editingObject;
    for (let field of fieldChains) {
        obj = obj[field];  // as 只是为了跳过ts类型检查
    }
    return obj;
}

function getFieldPrimitiveTypeConverter(fieldName: string, sItem?: SItem): ((value: any) => any) {
    if (sItem) {
        let structural = sItem as SStruct | STable;
        let field = getField(structural, fieldName);
        if (field) {
            let ft = field.type;
            if (ft == 'int') {
                return toInt;
            } else if (ft == 'long' || ft == 'float') {
                return toFloat;
            } else if (ft.startsWith('list<')) {
                let itemType = ft.slice(5, ft.length - 1);
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


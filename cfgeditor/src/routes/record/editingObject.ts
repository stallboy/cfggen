import {JSONArray, JSONObject, RecordResult} from "./recordModel.ts";
import {SItem, SStruct, STable} from "../table/schemaModel.ts";
import {getField, Schema} from "../table/schemaUtil.ts";
import {produce} from "immer";


export interface EditingObject {
    table: string;
    id: string;
    editingJson: JSONObject;
}

export interface EditCtx {
    editingObject: EditingObject;
    setEditingObject: (eo: EditingObject) => void;

}

export function startEditingObject(recordResult: RecordResult,
                                   oldEditingObject: EditingObject | undefined): EditingObject {

    const {table, id} = recordResult;
    if (oldEditingObject && oldEditingObject.table == table && oldEditingObject.id == id) {
        return oldEditingObject;  //cache一个state，从edit切换到view再回来会保留。
    }


    const clone: JSONObject = {...recordResult.object}
    delete clone['$refs'];  // inner $ref not deleted
    const editingJson = structuredClone(clone);
    return {table, id, editingJson}
}

export function onDeleteItemFromArray(ctx: EditCtx,
                                      deleteIndex: number,
                                      arrayFieldChains: (string | number)[]) {
    // console.log('delItem', arrayFieldChains, deleteIndex);

    applyMutate(ctx, (draft: EditingObject) => {
        let obj = getFieldObj(draft, arrayFieldChains) as JSONArray;
        obj.splice(deleteIndex, 1);
    });
}

export function onUpdateFormValues(ctx: EditCtx,
                                   schema: Schema,
                                   values: any,
                                   fieldChains: (string | number)[]) {

    // console.log('formChange', fieldChains, values);
    applyMutate(ctx, (draft: EditingObject) => {
        let obj = getFieldObj(draft, fieldChains);
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
    });
}


export function onUpdateInterfaceValue(ctx: EditCtx,
                                       jsonObject: JSONObject,
                                       fieldChains: (string | number)[]) {
    // console.log('updateInterface', fieldChains, jsonObject);
    applyMutate(ctx, (draft: EditingObject) => {
        let obj = getFieldObj(draft, fieldChains.slice(0, fieldChains.length - 1));
        obj[fieldChains[fieldChains.length - 1]] = jsonObject;
    });
}


export function onAddItemForArray(ctx: EditCtx,
                                  defaultItemJsonObject: JSONObject,
                                  arrayFieldChains: (string | number)[]) {
    // console.log('addItem', arrayFieldChains, defaultItemJsonObject);
    applyMutate(ctx, (draft: EditingObject) => {
        let obj = getFieldObj(draft, arrayFieldChains) as JSONArray;
        obj.push(defaultItemJsonObject);
    });
}

export function onClearToDefault(ctx: EditCtx,
                                 defaultValue: JSONObject) {
    applyMutate(ctx, (draft: EditingObject) => {
        draft.editingJson = defaultValue;
    });
}

function applyMutate(ctx: EditCtx, mutate: (draft: EditingObject) => void) {
    const newEditingObject = produce<EditingObject>(ctx.editingObject, mutate);
    ctx.setEditingObject(newEditingObject);
}

function getFieldObj(editingObject: EditingObject, fieldChains: (string | number)[]): any {
    let obj: any = editingObject.editingJson;
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


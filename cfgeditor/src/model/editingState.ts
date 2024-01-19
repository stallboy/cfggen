import {JSONArray, JSONObject, RecordResult} from "./recordModel.ts";
import {SItem, SStruct, STable} from "./schemaModel.ts";
import {getField, Schema} from "./schemaUtil.ts";


export class EditingState {
    schema?: Schema;
    editingObject: JSONObject;
    table?: string;
    id?: string;
    setForceUpdate: () => void;


    constructor() {
        this.editingObject = {
            $type: "",
        }
        this.setForceUpdate = () => {
        };
    }

    startEditingObject(schema: Schema, recordResult: RecordResult, setForceUpdate: () => void) {
        this.schema = schema;
        this.setForceUpdate = setForceUpdate;
        if (this.table && this.id) {
            if (this.table == recordResult.table && this.id == recordResult.id) {
                return;
            }
        }

        this.table = recordResult.table;
        this.id = recordResult.id;

        let clone: JSONObject = {...recordResult.object}
        delete clone['$refs'];  // inner $ref not deleted

        this.editingObject = structuredClone(clone);
        console.log(this.editingObject);
    }

    clear() {
        this.id = undefined;
    }

    onClearToDefault(defaultValue: JSONObject) {
        this.editingObject = defaultValue;
        this.setForceUpdate();
    }

    onUpdateFormValues(values: any, fieldChains: (string | number)[]) {
        // console.log('formChange', fieldChains, values);
        let obj = this.getFieldObj(fieldChains);
        let name = obj['$type'] as string;
        let sItem = this.schema?.itemIncludeImplMap.get(name);

        for (let key in values) {
            if (key.startsWith("$")) { // $impl
                continue;
            }
            let conv = this.getFieldPrimitiveTypeConverter(key, sItem);

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
    }

    getFieldObj(fieldChains: (string | number)[]): any {
        let obj: any = this.editingObject;
        for (let field of fieldChains) {
            obj = obj[field];  // as 只是为了跳过ts类型检查
        }
        return obj;
    }

    onUpdateInterfaceValue(jsonObject: JSONObject, fieldChains: (string | number)[]) {
        // console.log('updateInterface', fieldChains, jsonObject);
        let obj = this.getFieldObj(fieldChains.slice(0, fieldChains.length - 1));
        obj[fieldChains[fieldChains.length - 1]] = jsonObject;
        this.setForceUpdate();
    }

    onDeleteItemFromArray(deleteIndex: number, arrayFieldChains: (string | number)[]) {
        // console.log('delItem', arrayFieldChains, deleteIndex);
        let obj = this.getFieldObj(arrayFieldChains) as JSONArray;
        obj.splice(deleteIndex, 1);
        this.setForceUpdate();
    }

    onAddItemForArray(defaultItemJsonObject: JSONObject, arrayFieldChains: (string | number)[]) {
        // console.log('addItem', arrayFieldChains, defaultItemJsonObject);
        let obj = this.getFieldObj(arrayFieldChains) as JSONArray;
        obj.push(defaultItemJsonObject);
        this.setForceUpdate();
    }

    getFieldPrimitiveTypeConverter(fieldName: string, sItem?: SItem): ((value: any) => any) {
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


export let editingState: EditingState = new EditingState();

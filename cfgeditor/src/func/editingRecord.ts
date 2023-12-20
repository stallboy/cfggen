import {JSONArray, JSONObject, RecordResult} from "../model/recordModel.ts";


export class EditingState {
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

    startEditingObject(recordResult: RecordResult, setForceUpdate: () => void) {
        this.setForceUpdate = setForceUpdate;
        if (this.table && this.id) {
            if (recordResult.table == this.table && recordResult.id == this.id) {
                return;
            }
        }

        this.table = recordResult.table;
        this.id = recordResult.id;

        let clone: JSONObject = {...recordResult.object}
        delete clone['$refs'];

        this.editingObject = structuredClone(clone);
    }


    onUpdateFormValues(values: any, fieldChains: (string | number)[]) {
        console.log('formChange', fieldChains, values);

        let obj = this.getFieldObj(fieldChains);
        for (let key in values) {
            if (key.startsWith("$")) { // $impl
                continue;
            }
            let fieldValue = values[key];
            if (Array.isArray(fieldValue)) {
                // antd form 会返回[undefined, .. ], 这里忽略掉undefined 的item
                let fArr: JSONArray = fieldValue as JSONArray;
                let newArr = [];
                for (let fArrElement of fArr) {
                    if (fArrElement != undefined) {
                        newArr.push(fArrElement);
                    }
                }
                obj[key] = newArr;
            } else {
                obj[key] = fieldValue;
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
        console.log('updateInterface', fieldChains, jsonObject);
        let obj = this.getFieldObj(fieldChains.slice(0, fieldChains.length - 1));
        obj[fieldChains[fieldChains.length - 1]] = jsonObject;
        this.setForceUpdate();
    }

    onDeleteItemFromArray(deleteIndex: number, arrayFieldChains: (string | number)[]) {
        console.log('delItem', arrayFieldChains, deleteIndex);
        let obj = this.getFieldObj(arrayFieldChains) as JSONArray;
        obj.splice(deleteIndex, 1);
        this.setForceUpdate();
    }

    onAddItemForArray(defaultItemJsonObject: JSONObject, arrayFieldChains: (string | number)[]) {
        console.log('addItem', arrayFieldChains, defaultItemJsonObject);
        let obj = this.getFieldObj(arrayFieldChains) as JSONArray;
        obj.push(defaultItemJsonObject);
        this.setForceUpdate();
    }


}

export let editingState: EditingState = new EditingState();

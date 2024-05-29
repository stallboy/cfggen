import {Entity, EntityBaseField, EntityEditField, EntityType} from "./entityModel.ts";
import {NodeShowType} from "../routes/setting/storageJson.ts";


export function getNodeBackgroundColor(entity: Entity): string {
    const nodeShow = entity.sharedSetting?.nodeShow;
    if (nodeShow && nodeShow.tableHideAndColors.length > 0) {
        for (const tableColor of nodeShow.tableHideAndColors) {
            if (entity.label.includes(tableColor.keyword)) {
                return tableColor.color;
            }
        }
    }

    if (nodeShow && nodeShow.keywordColors.length > 0) {
        const value = getEntityValueStr(entity)
        if (value && value.length > 0){
            for (const keywordColor of nodeShow.keywordColors) {
                if (value.includes(keywordColor.keyword)) {
                    return keywordColor.color;
                }
            }
        }
    }

    switch (entity.entityType) {
        case EntityType.Ref:
            return '#207b4a'; // '#237804';
        case EntityType.Ref2:
            return '#006d75';
        case EntityType.RefIn:
            return '#003eb3';
        default:
            return '#0898b5';//'#005bbb', '#3271ae';
    }
}

function getEntityValueStr({brief, fields, edit}: Entity): string | undefined {
    let value;
    if (brief) {
        value = brief.value
    } else if (fields) {
        value = fields.map(f => f.value).join(',')
    } else if (edit) {
        const vec:string[] = [];
        fillEditFieldsVec(vec, edit.editFields);
        value = vec.join(',')
    }
    return value;
}

function fillEditFieldsVec(vec: string[], editFields: EntityEditField[]) {
    for (let {type, value, implFields} of editFields) {
        if (type == 'primitive'){
            vec.push(value.toString());
        }else if (type == 'arrayOfPrimitive'){
            vec.push(value.toString());
        }else if (type == 'interface' && implFields) {
            fillEditFieldsVec(vec, implFields);
        }
    }
}

export function getFieldBackgroundColor(field: EntityBaseField, nodeShow?: NodeShowType): string | undefined {
    if (nodeShow && nodeShow.fieldColors.length > 0) {
        for (const keywordColor of nodeShow.fieldColors) {
            if (field.name == keywordColor.keyword) {
                return keywordColor.color;
            }
        }
    }
}

export const edgeStorkColor = '#0898b5';//'#1677ff';

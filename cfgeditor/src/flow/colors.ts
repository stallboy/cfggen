import {Entity, EntityBaseField, EntityEditField, EntityType} from "./entityModel.ts";
import {NodeShowType} from "../routes/setting/storageJson.ts";


export function getNodeBackgroundColor(entity: Entity): string {
    const nodeShow = entity.sharedSetting?.nodeShow;
    if (nodeShow && nodeShow.nodeColorsByValue.length > 0) {
        const value = getEntityValueStr(entity)
        if (value && value.length > 0){
            for (const keywordColor of nodeShow.nodeColorsByValue) {
                if (value.includes(keywordColor.keyword)) {
                    return keywordColor.color;
                }
            }
        }
    }

    if (nodeShow && nodeShow.nodeColorsByLabel.length > 0) {
        for (const tableColor of nodeShow.nodeColorsByLabel) {
            if (entity.label.includes(tableColor.keyword)) {
                return tableColor.color;
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
            vec.push(value.toString());
            fillEditFieldsVec(vec, implFields);
        }
    }
}

export function getFieldBackgroundColor(field: EntityBaseField, nodeShow?: NodeShowType): string | undefined {
    if (nodeShow && nodeShow.fieldColorsByName.length > 0) {
        for (const keywordColor of nodeShow.fieldColorsByName) {
            if (field.name == keywordColor.keyword) {
                return keywordColor.color;
            }
        }
    }
}

export function getEdgeColor(nodeShow?: NodeShowType): string {
    return nodeShow?.edgeColor ?? '#0898b5';
}

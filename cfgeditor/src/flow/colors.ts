import {Entity, EntityBaseField, EntityEditField, EntityType} from "./entityModel.ts";
import {NodeShowType} from "../store/storageJson.ts";


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
            return nodeShow?.nodeRefColor ?? '#207b4a';
        case EntityType.Ref2:
            return nodeShow?.nodeRef2Color ?? '#006d75';
        case EntityType.RefIn:
            return nodeShow?.nodeRefInColor ?? '#003eb3';
        default:
            return nodeShow?.nodeColor ?? '#0898b5';
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
    for (const {type, value, implFields} of editFields) {
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

import {Entity, DisplayField, EntityEditField, EntityType, isReadOnlyEntity, isEditableEntity, isCardEntity} from "./entityModel.ts";
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

function getEntityValueStr(entity: Entity): string | undefined {
    if (isCardEntity(entity)) {
        return entity.brief.value;
    }

    if (isReadOnlyEntity(entity)) {
        return entity.fields.map(f => f.value).join(',');
    }

    if (isEditableEntity(entity)) {
        const vec: string[] = [];
        fillEditFieldsVec(vec, entity.edit.fields);
        return vec.join(',');
    }

    return undefined;
}

function fillEditFieldsVec(vec: string[], editFields: EntityEditField[]) {
    for (const editField of editFields) {
        if (editField.type == 'primitive'){
            vec.push(editField.value.toString());
        }else if (editField.type == 'arrayOfPrimitive'){
            vec.push(editField.value.toString());
        }else if (editField.type == 'interface') {
            vec.push(editField.value.toString());
            fillEditFieldsVec(vec, editField.implFields);
        }
    }
}

export function getFieldBackgroundColor(field: DisplayField | EntityEditField, nodeShow?: NodeShowType): string | undefined {
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

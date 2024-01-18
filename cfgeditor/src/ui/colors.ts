import {Entity, EntityType} from "../model/entityModel.ts";


export function getNodeBackgroundColor(entity: Entity): string {
    let nodeShow = entity.nodeShow;

    if (nodeShow && nodeShow.tableColors.length > 0) {
        for (let tableColor of nodeShow.tableColors) {
            if (entity.label.includes(tableColor.keyword)) {
                return tableColor.color;
            }
        }
    }

    if (entity.brief && nodeShow && nodeShow.keywordColors.length > 0) {
        for (let keywordColor of nodeShow.keywordColors) {
            if (entity.brief.value.includes(keywordColor.keyword)) {
                return keywordColor.color;
                break;
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


export const edgeStorkColor = '#0898b5';//'#1677ff';

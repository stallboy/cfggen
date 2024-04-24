import {Entity, EntityType} from "./entityModel.ts";


export function getNodeBackgroundColor(entity: Entity): string {
    const nodeShow = entity.sharedSetting?.nodeShow;

    if (nodeShow && nodeShow.tableHideAndColors.length > 0) {
        for (const tableColor of nodeShow.tableHideAndColors) {
            if (entity.label.includes(tableColor.keyword)) {
                return tableColor.color;
            }
        }
    }

    if (entity.brief && nodeShow && nodeShow.keywordColors.length > 0) {
        for (const keywordColor of nodeShow.keywordColors) {
            if (entity.brief.value.includes(keywordColor.keyword)) {
                return keywordColor.color;
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

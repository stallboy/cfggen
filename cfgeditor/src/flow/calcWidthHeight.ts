import {Entity} from "./entityModel.ts";
import {NodeShowType} from "../routes/setting/storageJson.ts";
import {getDsLenAndDesc} from "./EntityCard.tsx";


// 在一次又一次尝试了等待node准备好，直接用node的computed理的width，height后，增加这一个异步，太容易有闪烁和被代码绕晕了。
// 放弃放弃，还是预先估算好。
export function calcWidthHeight(entity: Entity, nodeShow: NodeShowType) {
    const {fields, brief, edit} = entity;
    const width = edit ? 280 : 240;
    let height = 40;

    if (fields) {
        height += 41 * fields.length;

    } else if (brief) {
        height += 48 + (brief.title ? 32 : 0);
        let [showDsLen, desc] = getDsLenAndDesc(brief, nodeShow);
        height += showDsLen * 38;
        if (desc) {
            height += 22 * desc.length / 13;
        }

    } else if (edit) {
        let cnt = 0;
        let extra = 0;
        for (let editField of edit.editFields) {
            switch (editField.type) {
                case "arrayOfPrimitive":
                    const len = (editField.value as any[]).length
                    cnt += len + 1;
                    break;

                case "interface":
                    cnt++;
                    if (editField.implFields) {
                        cnt += editField.implFields.length;
                    }
                    break;
                case 'primitive':
                    if (editField.eleType == 'text' || editField.eleType == 'str') {
                        let row = (editField.value as string).length / 10;
                        if (row > 10) {
                            row = 10;
                        }
                        if (row > 1) {
                            extra += row * 22 + 10;
                        } else {
                            cnt++;
                        }
                    } else {
                        cnt++;
                    }
                    break;
                default:
                    cnt++;
                    break;
            }
        }
        height += 20 + 40 * cnt + extra;
    }

    return [width, height];

}

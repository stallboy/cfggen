import {Entity} from "./entityModel.ts";
import {getDsLenAndDesc} from "./EntityCard.tsx";
import {ResInfo} from "../res/resInfo.ts";


// 在一次又一次尝试了等待node准备好，直接用node的computed理的width，height后，增加这一个异步，太容易有闪烁和被代码绕晕了。
// 放弃放弃，还是预先估算好。
export function calcWidthHeight(entity: Entity) {
    const {id, label, fields, brief, edit} = entity;
    const width = edit ? 280 : 240;
    let height = 40;

    if (fields) {
        height += 41 * fields.length;

    } else if (brief) {
        height += 48 + (brief.title ? 32 : 0);
        let [showDsLen, desc] = getDsLenAndDesc(brief, entity.sharedSetting?.nodeShow);
        height += showDsLen * 38;
        if (desc) {
            height += 22 * simpleStrlen(desc) / 30;
        }
        if (findFirstImage(entity.assets)) {
            height += 200;
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

    const notes = entity.sharedSetting?.notes;
    if (notes && label.includes('_')) {
        const note = notes.get(id);
        if (note) {
            let row = note.length / 15;
            if (row > 10) {
                row = 10;
            }
            if (row < 2) {
                row = 2;
            }
            height += row * 22 + 22;
        }
    }

    return [width, height];

}


function simpleStrlen(str: string) {
    let len = 0;
    let l = str.length
    for (let i = 0; i < l; i++) {
        if (str.charCodeAt(i) > 255) //如果是汉字，则字符串长度加2
            len += 2;
        else
            len++;
    }
    return len;
}

export function findFirstImage(assets: ResInfo[] | undefined): string | undefined {
    if (assets) {
        for (let r of assets) {
            if (r.type == 'image') {
                return r.path;
            }
        }
    }
}
import {Entity, EntityEditField} from "./entityModel.ts";
import {ResInfo} from "../res/resInfo.ts";
import {getDsLenAndDesc} from "./getDsLenAndDesc.tsx";


// 在一次又一次尝试了等待node准备好，直接用node的computed理的width，height后，增加这一个异步，太容易有闪烁和被代码绕晕了。
// 放弃放弃，还是预先估算好。
export function calcWidthHeight(entity: Entity) {
    const {id, label, fields, brief, edit} = entity;
    const width = edit ? (entity.sharedSetting?.nodeShow?.editNodeWidth ?? 280) : (entity.sharedSetting?.nodeShow?.nodeWidth ?? 240);
    let height = 40;

    if (fields) {
        height += 41 * fields.length;

    } else if (brief) {
        height += 48 + (brief.title ? 32 : 0);
        const [showDsLen, desc] = getDsLenAndDesc(brief, entity.sharedSetting?.nodeShow);
        height += showDsLen * 38;
        if (desc) {
            height += 22 * simpleStrRowCount(desc);
        }
        if (findFirstImage(entity.assets)) {
            height += 200;
        }

    } else if (edit) {
        const [cnt, extra] = calcEditFieldsCntAndExtra(edit.editFields)
        height += 20 + 40 * cnt + extra;
        if (edit.fold) {
            height += 16;
        }
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

function calcEditFieldsCntAndExtra(editFields: EntityEditField[]) {
    let cnt = 0;
    let extra = 0;
    for (const editField of editFields) {
        switch (editField.type) {
            case "arrayOfPrimitive":
                const len = (editField.value as never[]).length
                cnt += len + 1;
                extra += len * 8
                break;

            case "interface":
                cnt++;
                if (editField.implFields) {
                    const [implCnt, implExtra] = calcEditFieldsCntAndExtra(editField.implFields)
                    cnt += implCnt
                    extra += implExtra
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
    return [cnt, extra]
}


function simpleStrRowCount(str: string) {
    let len = 0;
    let row = 0;
    const ln = '\n'.charCodeAt(0)
    const l = str.length
    for (let i = 0; i < l; i++) {
        const code = str.charCodeAt(i)
        if (code == ln) {
            row++;
            len = 0;
        } else if (code > 255) { //如果是汉字，则字符串长度加2
            len += 2;
        } else {
            len++;
        }
        if (len >= 30) {
            row++;
            len = 0;
        }
    }
    return row;
}

export function findFirstImage(assets: ResInfo[] | undefined): string | undefined {
    if (assets) {
        for (const r of assets) {
            if (r.type == 'image') {
                return r.path;
            }
        }
    }
}

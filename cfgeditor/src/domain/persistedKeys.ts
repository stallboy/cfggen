import {JSONObject} from '@/api/recordModel';

// 持久化在数据对象上的 '$' 前缀元数据键的读写访问器（'$embed_<field>' 家族见 embedding.ts）。
// 共同约定：键只存非默认值——空串 note / false fold 一律删键，不留 inert 残留键
//（残留键会让键数与 baseline 不等，脏标记永远消不掉）。

/** 读 obj.$note（节点备注）。 */
export function getNote(obj: JSONObject | undefined): string | undefined {
    return obj?.['$note'] as string | undefined;
}

/** 写 obj.$note：空串/undefined 删键而非赋值。 */
export function setNote(obj: JSONObject, note: string | undefined): void {
    if (note === undefined || note === '') {
        delete obj['$note'];
    } else {
        obj['$note'] = note;
    }
}

/** 读 obj.$fold（节点级 fold，单义：折叠我自己的子节点）。无 obj 或无键 → undefined（视为未折叠）。 */
export function getFoldState(obj: JSONObject | undefined): boolean | undefined {
    return obj?.['$fold'] as boolean | undefined;
}

/** 写 obj.$fold：true 写键，false 删键（不残留 inert 的 false 值）。 */
export function setFold(obj: JSONObject, fold: boolean): void {
    if (fold) {
        obj['$fold'] = true;
    } else {
        delete obj['$fold'];
    }
}

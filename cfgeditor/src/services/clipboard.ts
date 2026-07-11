import {JSONObject} from "@/api/recordModel";

// app 级剪贴板：跨记录复制粘贴是现有能力（record A 复制 → 切到 record B → 同类型字段右键粘贴），
// 因此剪贴板独立于 EditingSession（会话级），生命周期是 app 级。
let copiedObject: JSONObject = {'$type': ''};

export function structCopy(obj: JSONObject): void {
    copiedObject = structuredClone(obj);
}

export function getCopiedObject(): JSONObject {
    return copiedObject;
}

export function isCopiedFitAllowedType(allowedType: string): boolean {
    const type = copiedObject.$type;
    if (type == allowedType) {
        return true;
    }

    if (type.startsWith(allowedType)) {
        return type[allowedType.length] == '.'  //简单判断，没有去查询interface和impl
    }

    return false;
}

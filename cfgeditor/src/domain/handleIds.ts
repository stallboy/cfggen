// reactflow handle id 约定——domain→reactflow 的唯一约定泄漏点，全仓单源：
// - '@in' / '@out'：节点级入/出 handle（挂在节点左右侧）
// - '@in_<fieldName>'：字段级入 handle；字段级出 handle 直接用字段名，无前缀
// 写侧（各 creator / refUtils）、渲染侧（FlowNode / EntityProperties）、解析侧
//（fillHandles）统一走本文件的常量与函数，禁止散落字面量。

export const HANDLE_IN = '@in';
export const HANDLE_OUT = '@out';

const IN_PREFIX = '@in_';

/** 字段级入 handle id：'@in_<fieldName>'。 */
export function inHandleForField(fieldName: string): string {
    return IN_PREFIX + fieldName;
}

/** 解析字段级入 handle：'@in_xxx' → 'xxx'；不匹配（含节点级 '@in'）返回 null。 */
export function parseInHandle(handle: string): string | null {
    return handle.startsWith(IN_PREFIX) ? handle.substring(IN_PREFIX.length) : null;
}

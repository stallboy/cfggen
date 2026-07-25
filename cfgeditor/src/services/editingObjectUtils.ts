import {JSONArray, JSONObject, JSONValue} from "@/api/recordModel";
import {parseFieldTypeId, SItem} from "@/api/schemaModel";
import {getField} from "@/domain/schema";
import {EntityPosition} from "@/domain/entityModel";
import type {EditingSession} from "./editingSession.ts";

// 纯 JSON 工具 + 会话注册表（从 editingSession.ts 尾部迁出，自包含）。
// 注：embedKey / getEmbedState / classifyListField / normalizeOnX 现统一驻 domain/embedding.ts
// （embed 概念的唯一家）。session 经 import 取用 embedKey + normalizeOnX；判定仍由调用方注入。

// ============ 模块级活动会话指针（跨路由寻址）============
// RecordWithResult 创建 session 后注册为"当前活动会话"；Chat/AddJson（Splitter 兄弟，非 Record 子树）
// 通过它寻址当前编辑会话。不是 React state，变异发生在 mount/unmount effect + 事件回调，不在 render。
let currentEditingSession: EditingSession | null = null;

export function getCurrentEditingSession(): EditingSession | null {
    return currentEditingSession;
}

export function setCurrentEditingSession(session: EditingSession | null): void {
    currentEditingSession = session;
}

// ============ 纯函数工具 ============

/** KeepStable 锚点位置：pickViewportAction 的 KeepStable 分支只读 id，x/y 为满足 EntityPosition 形状的占位。
 *  undo/redo 与正向删除三处共用，避免各处手搓 {id, x: 0, y: 0}。 */
export function anchorPosition(id: string): EntityPosition {
    return {id, x: 0, y: 0};
}

export function prepareEditingObject(rawObj: JSONObject): JSONObject {
    const cloned = structuredClone(rawObj);
    deleteRefsInPlace(cloned);
    return cloned;
}

export function getFieldObj(editingObject: JSONObject, fieldChains: (string | number)[]): JSONObject | JSONArray {
    let obj: JSONObject | JSONArray = editingObject;
    for (const field of fieldChains) {
        // 动态路径访问，中间节点都是容器（object/array），断言不可避免
        obj = (obj as JSONObject)[field as string] as JSONObject | JSONArray;
    }
    return obj;
}

export function getFieldPrimitiveTypeConverter(fieldName: string, sItem: SItem) {
    // 字段类型转换只查 struct/table 的字段（interface 无 fields）；interface 传入视作无匹配转换
    if (sItem.type === 'interface') return null;
    const field = getField(sItem, fieldName);
    if (field == null) {
        return null;
    }
    // 原始类型或 list<原始> 的元素类型：int→toInt、long|float→toFloat，其余→same
    const parsed = parseFieldTypeId(field.type);
    const elemType = parsed.kind === 'list' ? parsed.item
        : parsed.kind === 'primitive' ? parsed.name : null;
    if (elemType === 'int') return toInt;
    if (elemType === 'long' || elemType === 'float') return toFloat;
    return same;
}

function same(value: unknown): JSONValue {
    return value as JSONValue;
}

function toInt(value: unknown): JSONValue {
    // parseInt 对非法输入返回 NaN 且不抛异常；NaN 会被静默写回提交
    if (typeof value == 'string') {
        const n = parseInt(value);
        return Number.isNaN(n) ? 0 : n;
    }
    return value as JSONValue;
}

function toFloat(value: unknown): JSONValue {
    // parseFloat 同上
    if (typeof value == 'string') {
        const n = parseFloat(value);
        return Number.isNaN(n) ? 0 : n;
    }
    return value as JSONValue;
}

export function isDeeplyEqual(obj1: unknown, obj2: unknown): boolean {
    if (obj1 === obj2) return true;

    if (Array.isArray(obj1) && Array.isArray(obj2)) {
        if (obj1.length !== obj2.length) return false;
        return obj1.every((elem, index) => {
            return isDeeplyEqual(elem, obj2[index]);
        })
    }

    if (typeof obj1 === "object" && typeof obj2 === "object" && obj1 !== null && obj2 !== null) {
        if (Array.isArray(obj1) || Array.isArray(obj2)) return false;
        const keys1 = Object.keys(obj1)
        const keys2 = Object.keys(obj2)
        if (keys1.length !== keys2.length) return false;
        const keys2Set = new Set(keys2);
        if (!keys1.every(key => keys2Set.has(key))) return false;

        const o1 = obj1 as Record<string, unknown>;
        const o2 = obj2 as Record<string, unknown>;
        for (const key in o1) {
            const isEqual = isDeeplyEqual(o1[key], o2[key])
            if (!isEqual) {
                return false;
            }
        }
        return true;
    }

    return false;
}

/**
 * 就地删除后端附加的 `$refs`（FieldRef[]，"哪些记录引用了本记录"的展示元数据，见 recordModel.ts 的 Refs）。
 * 净化目的：`$refs` 是运行时引用关系、非可编辑数据，剥离后避免它进入 editingObject、污染提交载荷与
 * isEdited 脏比较基准。注意删的是本项目的 `$refs`（复数），不是 JSON Schema 的 `$ref`（引用指针）。
 */
export function deleteRefsInPlace(obj: unknown) {
    if (Array.isArray(obj)) {
        for (const item of obj) {
            deleteRefsInPlace(item);
        }
    } else if (typeof obj === "object" && obj !== null) {
        const o = obj as Record<string, unknown>;
        delete o['$refs'];
        for (const k in o) {
            deleteRefsInPlace(o[k]);
        }
    }
}

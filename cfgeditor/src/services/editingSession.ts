import {JSONArray, JSONObject, JSONValue, RecordResult} from "@/api/recordModel";
import {SItem, SStruct, STable} from "@/api/schemaModel";
import {getField, Schema} from "@/domain/schema";
import {EntityPosition, EFitView, EditingObjectRes} from "@/domain/entityModel";
import {setEditingState} from "@/store/store";
import {getCopiedObject} from "./clipboard";

/**
 * EditingSession —— 一个编辑会话的可变 store 实例（每条 record 编辑态一个实例）。
 *
 * 设计要点：
 *
 * 1. 取代旧的模块级全局单例 editState。副作用移出 React render：本类的 mutation 都发生在
 *    事件回调（UI 触发的编辑方法）或 effect（maybeReset）里，绝不在 render 期。
 *
 * 2. 用 useSyncExternalStore 接入 React：组件订阅 getStructureVersion（基本类型 number）。
 *    - 值类编辑（updateFormValues/updateNote）：就地改 editingObject + notifyEditingState（刷新
 *      HeaderBar 脏标记），但**不 bump structureVersion、不 emit** → Record 不重渲、entityMap 不
 *      重建（性能契约1：几十个表单输入零重渲）。
 *    - 结构类编辑（增删/swap/fold/impl/paste/replace）：就地改 + bump structureVersion + emit →
 *      Record 重渲 → entityMap 重算。重算时读 getEditingObject()（共享引用，性能契约2：entity
 *      闭包自动见最新值）。
 *
 * 3. 就地变异 + 共享引用语义完整保留：editingObject 是各 onUpdateXxx 就地改的同一对象，
 *    RecordEditEntityCreator.createThis 把它的子对象引用塞进 entity.edit.editObj，值类改后闭包
 *    直接见最新值，提交时 submit() 读到全量最新。
 *
 * 4. getSnapshot 永远返回基本类型（structureVersion）。绝不返回 editingObject 引用——就地变异下
 *    顶层引用不变，返回引用会让 React 永远跳过更新。
 */
export interface EditingSessionCallbacks {
    /** 结构类编辑时同步触发（事件期），用于清 layout 缓存。不能挪到 effect，否则布局用旧 cache 多渲染一帧。 */
    onStructureChange?: () => void;
    /** 提交回调（mutateRecord，引用稳定）。 */
    mutate?: (obj: JSONObject) => void;
}

export class EditingSession {
    private table: string;
    private id: string;
    private originalEditingObject: JSONObject;
    private editingObject: JSONObject;
    private fitView: EFitView;
    private fitViewToIdPosition?: EntityPosition;

    private structureVersion = 0;
    private readonly listeners = new Set<() => void>();
    private readonly onStructureChange?: () => void;
    private readonly mutate?: (obj: JSONObject) => void;

    constructor(recordResult: RecordResult, cbs: EditingSessionCallbacks = {}) {
        this.onStructureChange = cbs.onStructureChange;
        this.mutate = cbs.mutate;
        this.fitView = EFitView.FitFull;
        this.fitViewToIdPosition = undefined;
        // 首次初始化（构造期，纯字段赋值，不 bump/emit/onStructureChange：首帧 useMemo 本就会跑，
        // 此时还没有订阅者）。notify 由 Record 的 effect 负责。
        const obj = prepareEditingObject(recordResult.object);
        this.table = recordResult.table;
        this.id = recordResult.id;
        this.originalEditingObject = structuredClone(obj);
        this.editingObject = obj;
    }

    // ============ useSyncExternalStore 接口 ============

    subscribe = (listener: () => void): (() => void) => {
        this.listeners.add(listener);
        return () => {
            this.listeners.delete(listener);
        };
    };

    getStructureVersion = (): number => this.structureVersion;

    getEditingObject = (): JSONObject => this.editingObject;

    getIsEdited = (): boolean => !isDeeplyEqual(this.editingObject, this.originalEditingObject);

    /** 供 useMemo 返回的 editingObjectRes（喂 useEntityToGraph 的 layout queryKey/staleTime 通道）。 */
    getEditingObjectRes = (): EditingObjectRes => ({
        fitView: this.fitView,
        fitViewToIdPosition: this.fitViewToIdPosition,
        // 注意：此处 isEdited 随 entityMap 重算而更新。值类编辑不重算 entityMap → 此值不刷新
        // （layout 仍走 5min 缓存），这是现有 quirk，有意保留，勿当 bug 修。
        isEdited: this.getIsEdited(),
    });

    // ============ reset（recordResult 变化时由 effect 调，幂等）============

    /**
     * 同表同 id 且内容未变 → 保留当前编辑态（早退）；否则重置。
     * 在 effect 里调用，因此 reset 后直接 notifyEditingState 合法（render 期不行）。
     * 幂等：同 recordResult 第二次调走早退分支（StrictMode effect 双调安全）。
     */
    maybeReset(recordResult: RecordResult): void {
        const newTable = recordResult.table;
        const newId = recordResult.id;
        const newObj = prepareEditingObject(recordResult.object);

        if (newTable === this.table && newId === this.id
            && isDeeplyEqual(this.originalEditingObject, newObj)) {
            return; // 内容未变：保留当前编辑态
        }

        this.table = newTable;
        this.id = newId;
        this.originalEditingObject = structuredClone(newObj);
        this.editingObject = newObj;
        this.bumpStructure({fitView: EFitView.FitFull});
    }

    // ============ 值类编辑（不 bump、不 emit → 不重渲 entityMap）============

    /** primitive 表单值变更。就地改 editingObject，仅 notifyEditingState 刷新 HeaderBar 脏标记。 */
    updateFormValues(schema: Schema, values: Record<string, unknown>, fieldChains: (string | number)[]): void {
        const obj = getFieldObj(this.editingObject, fieldChains) as JSONObject;
        const name = obj['$type'] as string;
        if (name == undefined) {
            console.log("type undefined", obj, fieldChains, values);
            return;
        }

        if ("$impl" in values) {
            const impl = values["$impl"] as string;
            const idx = name.lastIndexOf(".");
            let typeName = name;
            if (idx != -1) {
                typeName = name.substring(idx + 1);
            }
            if (impl != typeName) {
                return; // impl变化由updateInterfaceValue处理，这里不处理
            }
        }
        const sItem = schema.itemIncludeImplMap.get(name);
        if (sItem == undefined) {
            console.log("%s not found", name);
            return;
        }
        for (const key in values) {
            if (key.startsWith("$")) { // $impl
                continue;
            }
            const conv = getFieldPrimitiveTypeConverter(key, sItem);
            if (conv == null) {
                // antd 会保留上一个form的值，用于下一个，所以这里忽略掉这些field
                continue;
            }

            const fieldValue = values[key];
            if (Array.isArray(fieldValue)) {
                // antd form 会返回[undefined, .. ], 这里忽略掉undefined 的item
                const fArr: JSONArray = fieldValue as JSONArray;
                const newArr = [];
                for (const fArrElement of fArr) {
                    if (fArrElement != undefined) {
                        newArr.push(conv(fArrElement));
                    }
                }
                obj[key] = newArr;
            } else {
                obj[key] = conv(fieldValue);
            }
        }
        // 值类：就地改完成。不 bump structureVersion、不 emit（契约1）。仅刷新脏标记。
        this.notifyEditingState();
    }

    updateNote(note: string | undefined, fieldChains: (string | number)[]): void {
        const obj = getFieldObj(this.editingObject, fieldChains) as JSONObject;
        obj['$note'] = note as JSONValue;
        this.notifyEditingState();
    }

    // ============ 结构类编辑（bump + emit → 重渲 entityMap）============

    addArrayItem(defaultItemJsonObject: JSONObject, arrayFieldChains: (string | number)[], position: EntityPosition): void {
        const obj = getFieldObj(this.editingObject, arrayFieldChains) as JSONArray;
        obj.push(defaultItemJsonObject);
        this.structureChange(position);
    }

    /** 前插入：让插入的节点和当前鼠标所在节点位置不变 */
    addArrayItemAtIndex(defaultItemJsonObject: JSONObject, index: number,
                        arrayFieldChains: (string | number)[], position: EntityPosition): void {
        const obj = getFieldObj(this.editingObject, arrayFieldChains) as JSONArray;
        obj.splice(index, 0, defaultItemJsonObject);
        this.structureChange(position);
    }

    deleteArrayItem(deleteIndex: number, arrayFieldChains: (string | number)[], position: EntityPosition): void {
        const obj = getFieldObj(this.editingObject, arrayFieldChains) as JSONArray;
        obj.splice(deleteIndex, 1);
        this.structureChange(position);
    }

    /** 相邻索引的上/下移，语义是 swap 而非 move（命名沿用旧 onSwapItemInArray）。 */
    swapArrayItem(indexA: number, indexB: number, arrayFieldChains: (string | number)[], position: EntityPosition): void {
        const obj = getFieldObj(this.editingObject, arrayFieldChains) as JSONArray;
        const o2 = obj[indexB];
        obj[indexB] = obj[indexA];
        obj[indexA] = o2;
        this.structureChange(position);
    }

    updateFold(fold: boolean, fieldChains: (string | number)[], position: EntityPosition): void {
        const obj = getFieldObj(this.editingObject, fieldChains) as JSONObject;
        obj['$fold'] = fold;
        this.structureChange(position);
    }

    updateInterfaceValue(jsonObject: JSONObject, fieldChains: (string | number)[], position: EntityPosition): void {
        const obj = getFieldObj(this.editingObject, fieldChains.slice(0, fieldChains.length - 1)) as JSONObject;
        obj[fieldChains[fieldChains.length - 1] as string] = jsonObject;
        this.structureChange(position);
    }

    /**
     * 整体替换编辑对象（Chat/AddJson 写入 / funcClear）。不改 originalEditingObject（保留脏比较基准）。用 FitFull。
     * 入参就地剥离 $refs，与 prepareEditingObject 对齐：Chat/AddJson 的外部 JSON 可能带后端附加的 $refs（引用元数据），
     * 不剥离会污染提交载荷、并让 getIsEdited 误判 dirty（基准 originalEditingObject 构造期已净化过）。
     * 入参均为调用方 fresh 构造（JSON.parse / defaultValueOfStructural，无共享引用），故就地净化而非 clone。
     */
    replaceEditingObject(newEditingObject: JSONObject): void {
        deleteRefsInPlace(newEditingObject);
        this.editingObject = newEditingObject;
        this.bumpStructure({fitView: EFitView.FitFull});
    }

    /** 把剪贴板内容粘贴到 fieldChains 位置。 */
    pasteStruct(fieldChains: (string | number)[], position: EntityPosition): void {
        const copied = getCopiedObject();
        let obj: JSONObject | JSONArray = this.editingObject;
        let i = 0;
        const len = fieldChains.length;
        for (const field of fieldChains) {
            if (i == len - 1) {
                (obj as JSONObject)[field as string] = structuredClone(copied);
            } else {
                obj = (obj as JSONObject)[field as string] as JSONObject;  // as 只是为了跳过ts类型检查
            }
            i++;
        }
        this.structureChange(position);
    }

    // ============ submit / notify ============

    submit(): void {
        this.mutate?.(this.editingObject);
    }

    /** 把 table/id/isEdited 同步到 resso store（HeaderBar 唯一订阅者，显示 unsaved）。 */
    notifyEditingState(): void {
        setEditingState(this.table, this.id, this.getIsEdited());
    }

    // ============ 内部 ============

    private structureChange(position: EntityPosition): void {
        this.bumpStructure({fitView: EFitView.FitId, position});
    }

    /** 结构变更通用收尾：写 fitView 契约 → bump 版本（触发订阅者重渲）→ 同步清 layout 缓存 → 刷新脏标记 → emit。
     *  参量化 fitView：undo/redo 传 {fitView: NoChange} 不跳视口；结构操作传 FitId；整体替换/reset 传 FitFull。 */
    private bumpStructure(opts: {fitView: EFitView; position?: EntityPosition}): void {
        this.fitView = opts.fitView;
        this.fitViewToIdPosition = opts.position;
        this.structureVersion++;
        this.onStructureChange?.();
        this.notifyEditingState();
        this.emit();
    }

    private emit(): void {
        this.listeners.forEach(l => l());
    }
}

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

// ============ 纯函数工具（从 editingObject.ts 搬入，自包含）============

function prepareEditingObject(rawObj: JSONObject): JSONObject {
    const cloned = structuredClone(rawObj);
    deleteRefsInPlace(cloned);
    return cloned;
}

function getFieldObj(editingObject: JSONObject, fieldChains: (string | number)[]): JSONObject | JSONArray {
    let obj: JSONObject | JSONArray = editingObject;
    for (const field of fieldChains) {
        // 动态路径访问，中间节点都是容器（object/array），断言不可避免
        obj = (obj as JSONObject)[field as string] as JSONObject | JSONArray;
    }
    return obj;
}

function getFieldPrimitiveTypeConverter(fieldName: string, sItem: SItem) {
    const structural = sItem as SStruct | STable;
    const field = getField(structural, fieldName);
    if (field == null) {
        return null;
    }
    const ft = field.type;
    if (ft == 'int') {
        return toInt;
    } else if (ft == 'long' || ft == 'float') {
        return toFloat;
    } else if (ft.startsWith('list<')) {
        const itemType = ft.slice(5, ft.length - 1);
        if (itemType == 'int') {
            return toInt;
        } else if (itemType == 'long' || itemType == 'float') {
            return toFloat;
        }
    }
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

function isDeeplyEqual(obj1: unknown, obj2: unknown): boolean {
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
        if (keys1.length !== keys2.length || !keys1.every(key => keys2.includes(key))) return false;

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
function deleteRefsInPlace(obj: unknown) {
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

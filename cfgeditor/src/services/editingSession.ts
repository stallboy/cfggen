import {JSONArray, JSONObject, JSONValue, RecordResult} from "@/api/recordModel";
import {SItem, SStruct, STable} from "@/api/schemaModel";
import {getField, Schema} from "@/domain/schema";
import {EntityPosition, EFitView, EditingObjectRes} from "@/domain/entityModel";
import {getCopiedObject} from "./clipboard";
import {UndoStack} from "@/domain/undoStack";

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
    /** 编辑态变化通知（table/id/isEdited）——由创建方注入写 store，使本类不依赖 store 层。 */
    onEditingStateChange?: (table: string, id: string, isEdited: boolean) => void;
}

export class EditingSession {
    private table: string;
    private id: string;
    private originalEditingObject: JSONObject;
    private editingObject: JSONObject;
    private fitView: EFitView;
    private fitViewToIdPosition?: EntityPosition;

    /** 编辑态脏标记缓存，让 getIsEdited 在 clean 路径 O(1)、dirty 路径惰性精确化：
     *  - dirty=false：必然精确 clean（reset/commit/recomputeDirty/getIsEdited 算出），且任何 mutation 经
     *    markDirty 置 true，故 dirty=false 期间不可能有"未计入"的变更 → 可直接采信。
     *  - dirty=true：可能是 markDirty 的乐观置位（尚未精确算），用 mutationSeq/mutationSeqCached 判缓存有效性：
     *    seq 相同 → 自上次精确计算后无新 mutation，采信 true；seq 不同 → 有新 mutation，重新精确算。
     *  这样既保留缓存收益，又修正纯 dirty 标记会误报的场景（updateNote 加一字符再减一字符 = 实际相等却报 dirty）。 */
    private dirty = false;
    private mutationSeq = 0;          // 每次 mutation（markDirty）递增，标记"对象可能又变了"
    private mutationSeqCached = 0;   // 上次精确计算 dirty 时的 mutationSeq，缓存有效性的基准

    private structureVersion = 0;
    private readonly listeners = new Set<() => void>();
    private readonly onStructureChange?: () => void;
    private readonly mutate?: (obj: JSONObject) => void;
    private readonly onEditingStateChange?: (table: string, id: string, isEdited: boolean) => void;

    // undo/redo 快照栈（per-session，随实例生灭）。capture 时机：结构操作后 / 值类 coalescing 组关闭。
    // 命名 undoStack 而非 undo：避免与 undo() 方法同名（TS 不允许同名的属性与方法）。
    private readonly undoStack = new UndoStack();

    // 值类 coalescing：同字段连续键入合并为一步 undo（500ms 窗口 + 换字段/blur/结构 op 关闭组）。
    // per-key O(1) 不变量：只做"字段标识比较 + clearTimeout/setTimeout"，严禁每键 clone/遍历（啃掉契约1）。
    private valueCoalesceTimer: ReturnType<typeof setTimeout> | undefined;
    private valueCoalesceKey: string | undefined;

    constructor(recordResult: RecordResult, cbs: EditingSessionCallbacks = {}) {
        this.onStructureChange = cbs.onStructureChange;
        this.mutate = cbs.mutate;
        this.onEditingStateChange = cbs.onEditingStateChange;
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

    getIsEdited = (): boolean => {
        if (!this.dirty) return false;                               // 精确 clean：采信（任何 mutation 会经 markDirty 置 true）
        if (this.mutationSeq === this.mutationSeqCached) return true;     // 自上次精确计算后无新 mutation：采信 true
        // 有新 mutation 且尚未精确算：重新深比较并缓存（O(n)，仅在"mutation 后首次读"发生）
        this.dirty = !isDeeplyEqual(this.editingObject, this.originalEditingObject);
        this.mutationSeqCached = this.mutationSeq;
        return this.dirty;
    };

    /** mutation 通用置脏：乐观标 dirty=true + 递增 mutationSeq（让 getIsEdited 缓存失效，下次精确重算）。 */
    private markDirty(): void {
        this.dirty = true;
        this.mutationSeq++;
    }

    /** 全量重算 dirty（undo/redo 后调：可能恰好回到 baseline 变 clean，不能简单翻转）。
     *  算后对齐 mutationSeqCached=mutationSeq——相当于"在当前 seq 精确算过一次"，下次 getIsEdited 可直接采信缓存。 */
    private recomputeDirty(): void {
        this.dirty = !isDeeplyEqual(this.editingObject, this.originalEditingObject);
        this.mutationSeqCached = this.mutationSeq;
    }

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
        this.editingObject = newObj;
        this.dirty = false;   // 切到新 record：新基准即 newObj，初始无编辑（bumpStructure 的 notify 需读到 false）
        this.bumpStructure({fitView: EFitView.FitFull});
        this.resetBaselines();   // 新数据为新基准（脏比较 + undo baseline），清栈
    }

    // ============ 值类编辑（不 bump、不 emit → 不重渲 entityMap）============

    /** primitive 表单值变更。就地改 editingObject，仅 notifyEditingState 刷新 HeaderBar 脏标记。
     *  changed（可选）= antd changedValues，用于值类 coalescing 合并 key + Form.List 长度 diff。
     *  不传 changed（如单测直接调）→ 只写回，不 coalescing。 */
    updateFormValues(schema: Schema,
                     values: Record<string, unknown>,
                     fieldChains: (string | number)[],
                     changed?: Record<string, unknown>): void {

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
            const isChanged = changed !== undefined && key in changed;
            if (Array.isArray(fieldValue)) {
                // Form.List 长度 diff（写回前比较旧长度）
                const oldArr = obj[key];
                const oldLen = Array.isArray(oldArr) ? oldArr.length : 0;
                // antd form 会返回[undefined, .. ], 这里忽略掉undefined 的item
                const fArr: JSONArray = fieldValue as JSONArray;
                const newArr = [];
                for (const fArrElement of fArr) {
                    if (fArrElement != undefined) {
                        newArr.push(conv(fArrElement));
                    }
                }
                if (isChanged && newArr.length !== oldLen) {
                    // 长度变 = 结构步：flush 旧组（capture 旧态，不含本次写入）+ 写回 + capture 新态
                    // Form.List 行增删不建实体（primitive list 非节点），布局变化极小 → undo 用 NoChange（不动）
                    this.flushValueCoalesce();
                    obj[key] = newArr;
                    this.capture(EFitView.NoChange);
                } else {
                    // 长度同 = 值类合并：touch（写回前，换字段 flush 旧组 capture 旧态）+ 写回
                    if (isChanged) {
                        this.touchValueCoalesce(this.coalesceKey(fieldChains, key));
                    }
                    obj[key] = newArr;
                }
            } else {
                // primitive：touch（写回前，换字段 flush 旧组 capture 旧态，不含本次写入）+ 写回
                if (isChanged) {
                    this.touchValueCoalesce(this.coalesceKey(fieldChains, key));
                }
                obj[key] = conv(fieldValue);
            }
        }
        // 值类：就地改完成。不 bump structureVersion、不 emit（契约1）。置脏 + 刷新 HeaderBar 标记。
        this.markDirty();
        this.notifyEditingState();
    }

    updateNote(note: string | undefined, fieldChains: (string | number)[]): void {
        const obj = getFieldObj(this.editingObject, fieldChains) as JSONObject;
        obj['$note'] = note as JSONValue;
        this.touchValueCoalesce(this.coalesceKey(fieldChains, '$note'));
        this.markDirty();
        this.notifyEditingState();
    }

    // ============ 结构类编辑（bump + emit → 重渲 entityMap）============

    addArrayItem(defaultItemJsonObject: JSONObject, arrayFieldChains: (string | number)[], position: EntityPosition,
                 markExpanded?: boolean): void {
        this.beforeStructuralChange();
        const obj = getFieldObj(this.editingObject, arrayFieldChains) as JSONArray;
        obj.push(defaultItemJsonObject);
        this.normalizeEmbedKeyOnAdd(arrayFieldChains, markExpanded);
        this.structureChange(position);
    }

    /** 前插入：让插入的节点和当前鼠标所在节点位置不变 */
    addArrayItemAtIndex(defaultItemJsonObject: JSONObject, index: number,
                        arrayFieldChains: (string | number)[], position: EntityPosition,
                        markExpanded?: boolean): void {
        this.beforeStructuralChange();
        const obj = getFieldObj(this.editingObject, arrayFieldChains) as JSONArray;
        obj.splice(index, 0, defaultItemJsonObject);
        this.normalizeEmbedKeyOnAdd(arrayFieldChains, markExpanded);   // 同 addArrayItem
        this.structureChange(position);
    }

    /** anchorId：锚点节点 id，必传父节点 id（被删节点锚不住：正向已消失、undo 前不存在）。
     *  正向删除与 undo/redo 都是 KeepStable 锚定父节点——父节点屏幕不动，其余节点相对它重排。
     *  embeddableWhenSingle：删除后恰剩 1 元素时，该元素的可内嵌判定（由调用方经 canBeEmbeddedCheck 算好，
     *  session 不反向依赖 domain 的 embedding 判定）。 */
    deleteArrayItem(deleteIndex: number, arrayFieldChains: (string | number)[], anchorId: string,
                    embeddableWhenSingle?: boolean): void {
        this.beforeStructuralChange();
        const obj = getFieldObj(this.editingObject, arrayFieldChains) as JSONArray;
        obj.splice(deleteIndex, 1);
        this.normalizeEmbedKeyOnDelete(arrayFieldChains, embeddableWhenSingle);
        this.structureChange(anchorPosition(anchorId), anchorId, EFitView.KeepStable);
    }

    /** 相邻索引的上/下移，语义是 swap 而非 move（命名沿用旧 onSwapItemInArray）。 */
    swapArrayItem(indexA: number, indexB: number, arrayFieldChains: (string | number)[], position: EntityPosition): void {
        this.beforeStructuralChange();
        const obj = getFieldObj(this.editingObject, arrayFieldChains) as JSONArray;
        const o2 = obj[indexB];
        obj[indexB] = obj[indexA];
        obj[indexA] = o2;
        this.structureChange(position);
    }

    /** 节点级 fold：`$fold` 单义——折叠我自己的子节点。true 写键，false 删键（不残留 inert 的 false 值）。 */
    updateFold(fold: boolean, fieldChains: (string | number)[], position: EntityPosition): void {
        this.beforeStructuralChange();
        const obj = getFieldObj(this.editingObject, fieldChains) as JSONObject;
        if (fold) {
            obj['$fold'] = true;
        } else {
            delete obj['$fold'];
        }
        this.structureChange(position);
    }

    /** 字段级 embed：状态写在父对象的 `$embed_<fieldName>` 键上（数组挂不了属性，与 $fold 同约定持久化）。
     *  语义「true=收起，false=展开，键只存当前类的非默认值」——本方法写字面布尔值：
     *  类 1（自动嵌入）展开时传 false；类 2（list 嵌入）折叠时传 true。回到默认态走 deleteEmbed。
     *  position 锚点取父节点（两个方向上父节点都在）。 */
    updateEmbed(embed: boolean, fieldName: string, parentChain: (string | number)[], position: EntityPosition): void {
        this.beforeStructuralChange();
        const obj = getFieldObj(this.editingObject, parentChain) as JSONObject;
        obj[embedKey(fieldName)] = embed;
        this.structureChange(position);
    }

    /** 回到字段当前类的默认 embed 态：删 `$embed_<fieldName>` 键（回嵌 / 展开 list 共用）。
     *  不写与默认值同义的键，避免载荷残留。 */
    deleteEmbed(fieldName: string, parentChain: (string | number)[], position: EntityPosition): void {
        this.beforeStructuralChange();
        const obj = getFieldObj(this.editingObject, parentChain) as JSONObject;
        delete obj[embedKey(fieldName)];
        this.structureChange(position);
    }

    /** 定位字段级 embed 槽位：父对象 + 键 + list 本体（add / delete / impl 切换三个归一化执行点共用）。 */
    private resolveEmbedSlot(arrayFieldChains: (string | number)[]): { parent: JSONObject; key: string; arr: JSONArray } {
        const parent = getFieldObj(this.editingObject, arrayFieldChains.slice(0, -1)) as JSONObject;
        const fieldName = arrayFieldChains[arrayFieldChains.length - 1] as string;
        return {parent, key: embedKey(fieldName), arr: parent[fieldName] as JSONArray};
    }

    /** add 后的 `$embed_` 键归一化（不变式：键必须存当前类的非默认值）：
     *  - 0→1 且新元素可内嵌（markExpanded）：写 false——新元素默认展开、立即可编辑（原 markNewItemExpanded 语义）；
     *  - 其余（≥2，或单元素不可内嵌）：删键——覆盖「折叠中加元素自动展开」与「1→2 后 false 键变残留」两种情形。 */
    private normalizeEmbedKeyOnAdd(arrayFieldChains: (string | number)[], markExpanded?: boolean): void {
        const {parent, key, arr} = this.resolveEmbedSlot(arrayFieldChains);
        if (arr.length === 1 && markExpanded) {
            parent[key] = false;
        } else {
            delete parent[key];
        }
    }

    /** delete 后的 `$embed_` 键归一化（与用户操作同一步 undo）：
     *  - 删到空：嵌入失去意义，删键；
     *  - 删到恰剩 1 且元素可内嵌（跨入类 1）：true（折叠）→ 删键（嵌入 Tag 延续收起意图）；否则写 false（保持展开，
     *    含「展开的多元素 list 删到 1」——不把用户正看着的展开节点压回 Tag）；
     *  - 删到恰剩 1 且元素不可内嵌（类 2）：删键（默认展开）。 */
    private normalizeEmbedKeyOnDelete(arrayFieldChains: (string | number)[], embeddableWhenSingle?: boolean): void {
        const {parent, key, arr} = this.resolveEmbedSlot(arrayFieldChains);
        if (arr.length === 0) {
            delete parent[key];
        } else if (arr.length === 1) {
            if (embeddableWhenSingle) {
                if (parent[key] === true) {
                    delete parent[key];
                } else {
                    parent[key] = false;
                }
            } else {
                delete parent[key];
            }
        }
    }

    /** embeddable：切换 impl 后的 `$embed_` 键归一化（双向，不变式 + 保持展开意图——切换入口只在展开态）：
     *  新 impl 可内嵌 → 确保 `$embed_<fieldName>=false`（无则补写）；不可内嵌 → 删残留键。
     *  判定由调用方经 canBeEmbeddedCheck 算好传入，session 不反向依赖 domain。
     *  注意 fieldChains 可能以数组索引结尾（list 元素换 impl）：此时 embed 键属于祖父对象的
     *  `$embed_<listName>`，且仅当 list 恰剩 1 元素（类 1 候选）时才可能写 false，多元素一律删键。 */
    updateInterfaceValue(jsonObject: JSONObject, fieldChains: (string | number)[], position: EntityPosition,
                         embeddable?: boolean): void {
        this.beforeStructuralChange();
        const last = fieldChains[fieldChains.length - 1];
        const parent = getFieldObj(this.editingObject, fieldChains.slice(0, fieldChains.length - 1)) as JSONObject;
        parent[last as string] = jsonObject;
        if (embeddable !== undefined) {
            if (typeof last === 'number') {
                // list 元素换 impl：键在祖父对象上，键名取 list 字段名
                const slot = this.resolveEmbedSlot(fieldChains.slice(0, -1));
                if (slot.arr.length === 1 && embeddable) {
                    slot.parent[slot.key] = false;
                } else {
                    delete slot.parent[slot.key];
                }
            } else {
                const key = embedKey(last);
                if (embeddable) {
                    parent[key] = false;
                } else {
                    delete parent[key];
                }
            }
        }
        this.structureChange(position);
    }

    /**
     * 整体替换编辑对象（Chat/AddJson 写入 / funcClear）。不改 originalEditingObject（保留脏比较基准）。用 FitFull。
     * 入参就地剥离 $refs，与 prepareEditingObject 对齐：Chat/AddJson 的外部 JSON 可能带后端附加的 $refs（引用元数据），
     * 不剥离会污染提交载荷、并让 getIsEdited 误判 dirty（基准 originalEditingObject 构造期已净化过）。
     * 入参均为调用方 fresh 构造（JSON.parse / defaultValueOfStructural，无共享引用），故就地净化而非 clone。
     */
    replaceEditingObject(newEditingObject: JSONObject): void {
        this.beforeStructuralChange();
        deleteRefsInPlace(newEditingObject);
        this.editingObject = newEditingObject;
        this.markDirty();
        this.bumpStructure({fitView: EFitView.FitFull});
        this.capture(EFitView.FitFull);   // 整体替换 undo 时也 FitFull（重新认识图）
    }

    /** 把剪贴板内容粘贴到 fieldChains 位置。 */
    pasteStruct(fieldChains: (string | number)[], position: EntityPosition): void {
        this.beforeStructuralChange();
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

    /** 提交成功后重置基准（submit 异步、成败要等网络，故重基准挂在 onSuccess 而非 submit 调用时——
     *  否则提交失败会丢 undo 历史、脏标记还误报"无未保存"）。
     *  重 originalEditingObject（脏比较归 false）+ undo.setBaseline（清栈+新基准 = 当前已提交状态）。 */
    onCommitSuccess(): void {
        // 提交后保持当前视图：重置视口语义为 NoChange。onSuccess 还会 invalidateAllQueries → record refetch
        // → recordResult 新引用触发 useMemo 重算，读到此处设的 fitView，Effect 2 走 noop 不跳视口。
        // （onCommitSuccess 本就不 emit；fitView 靠 recordResult 重取驱动的那次重渲生效，不依赖本次 bump。）
        this.fitView = EFitView.NoChange;
        this.fitViewToIdPosition = undefined;
        this.resetBaselines();
    }

    /** 把 table/id/isEdited 通知订阅者（HeaderBar 显示 unsaved）。实现由创建方注入，本类不依赖 store。 */
    notifyEditingState(): void {
        this.onEditingStateChange?.(this.table, this.id, this.getIsEdited());
    }

    // ============ undo/redo ============

    /** 初始 undo 基准：mount effect 调一次（构造函数在 render 期，structuredClone 是副作用，挪到 effect）。
     *  幂等：StrictMode 双调安全（setBaseline 清栈+设基准，二次调同值无害）。 */
    initUndoBaseline(): void {
        this.undoStack.setBaseline({data: this.captureUndoPoint(), undoFitView: EFitView.FitFull});
    }

    undo(): void {
        this.flushValueCoalesce();   // 先固化未 capture 的键入（否则丢失）
        if (!this.undoStack.canUndo()) return;
        const {target, undoFitView, anchorId} = this.undoStack.popUndo();
        this.applyUndoPoint(target.data);
        this.recomputeDirty();   // undo 可能恰好回到 baseline（变 clean），重算而非简单翻转
        // 按被撤销操作的视口语义驱动：结构→KeepStable(锚点屏幕不动)；整体替换→FitFull；值类→NoChange(不动)
        this.bumpStructure({fitView: undoFitView, position: anchorId ? anchorPosition(anchorId) : undefined});
    }

    redo(): void {
        this.flushValueCoalesce();
        if (!this.undoStack.canRedo()) return;
        const {target, undoFitView, anchorId} = this.undoStack.popRedo();
        this.applyUndoPoint(target.data);
        this.recomputeDirty();
        this.bumpStructure({fitView: undoFitView, position: anchorId ? anchorPosition(anchorId) : undefined});
    }

    // 箭头字段：作为引用传给 useSyncExternalStore 时 this 不丢（与 subscribe/getStructureVersion 等读取器同约定）。
    canUndo = (): boolean => this.undoStack.canUndo();

    canRedo = (): boolean => this.undoStack.canRedo();

    /** unmount 清理：flush 值类组（不丢最后一次键入）+ 清 timer 防 setTimeout 闭包持 session 泄漏 + 清 listeners。 */
    dispose(): void {
        this.flushValueCoalesce();
        clearTimeout(this.valueCoalesceTimer);
        this.listeners.clear();
    }

    // ============ 内部 ============

    private structureChange(position: EntityPosition, anchorId?: string, forwardFitView?: EFitView): void {
        this.markDirty();
        this.bumpStructure({fitView: forwardFitView ?? EFitView.FitId, position});
        // undo 锚点默认 = 操作节点 id（用户视觉焦点）；delete 传父 id（被删节点锚不住，父稳定）
        this.capture(EFitView.KeepStable, anchorId ?? position.id);
    }

    /** 结构变更通用收尾：写 fitView 契约 → bump 版本（触发订阅者重渲）→ 同步清 layout 缓存 → 刷新脏标记 → emit。
     *  参量化 fitView：结构操作传 FitId；整体替换/reset 传 FitFull；undo/redo 按被撤销操作快照的语义传 KeepStable/NoChange/FitFull。 */
    private bumpStructure(opts: { fitView: EFitView; position?: EntityPosition }): void {
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

    /** editingObject 的一份独立深拷（snapshot）。升级契约点：将来换 JSON Patch 只动这两个方法。 */
    private captureUndoPoint(): JSONObject {
        return structuredClone(this.editingObject);
    }

    /** 入栈一份带 undo 视口语义的快照（data=captureUndoPoint 深拷）。
     *  结构步→KeepStable+锚点（undo 时锚点屏幕不动）；整体替换→FitFull；值类/Form.List→NoChange（undo 不动）。 */
    private capture(undoFitView: EFitView, anchorId?: string): void {
        this.undoStack.capture({data: this.captureUndoPoint(), undoFitView, anchorId});
    }

    /** 把 snapshot 恢复成 editingObject（clone 入参，避免栈里 snapshot 被后续就地变异污染）。 */
    private applyUndoPoint(s: JSONObject): void {
        this.editingObject = structuredClone(s);
    }

    /** 重置脏比较基准 + undo 基准为当前 editingObject（提交/reset 后调，清栈）。
     *  originalEditingObject 与 undo.baseline 共享同一 clone：两者都只读不被 mutate（popUndo 返回 baseline
     *  引用但 applyUndoPoint 会 clone，不污染），共享安全且省一次 structuredClone。 */
    private resetBaselines(): void {
        // 清 coalesce timer（提交/reset 后旧键入已落库或被新数据覆盖，不保留 undo；timer 不清会 fire 污染新栈）
        clearTimeout(this.valueCoalesceTimer);
        this.valueCoalesceTimer = undefined;
        this.valueCoalesceKey = undefined;
        const snap = this.captureUndoPoint();
        this.originalEditingObject = snap;
        this.dirty = false;   // 新基准 = 当前已提交/重置态，脏比较归零
        this.undoStack.setBaseline({data: snap, undoFitView: EFitView.FitFull});
    }

    // ---- 值类 coalescing ----

    /** 字段标识：fieldChain + 字段名 → 唯一 string（同字段连续键入合并）。 */
    private coalesceKey(fieldChains: (string | number)[], fieldKey: string): string {
        return [...fieldChains, fieldKey].join('/');
    }

    /** 值类编辑每键调：同字段 + 500ms 窗口内 → 重置定时器不入栈；换字段 → 关闭旧组开新组。
     *  per-key O(1)：只比较字段标识 + clearTimeout/setTimeout，不 clone 不遍历。 */
    private touchValueCoalesce(fieldKey: string): void {
        if (this.valueCoalesceKey !== fieldKey) {
            this.flushValueCoalesce();
            this.valueCoalesceKey = fieldKey;
        }
        if (this.valueCoalesceTimer !== undefined) clearTimeout(this.valueCoalesceTimer);
        this.valueCoalesceTimer = setTimeout(() => this.flushValueCoalesce(), 500);
    }

    /** 关闭当前值类组：capture 一份快照入栈 + emit 通知订阅者（canUndo/canRedo 已变）。无活跃组则 no-op。 */
    private flushValueCoalesce(): void {
        if (this.valueCoalesceTimer === undefined) return;
        clearTimeout(this.valueCoalesceTimer);
        this.valueCoalesceTimer = undefined;
        this.capture(EFitView.NoChange);
        this.valueCoalesceKey = undefined;
        this.emit();   // capture 不 bump structureVersion；canUndo/canRedo 已变，emit 通知潜在订阅者（Record 现不订阅，hotkey 回调实时判）
    }

    /** 结构操作前置：关闭值类组（固化未 capture 的键入，避免与结构操作混在一个快照）。 */
    private beforeStructuralChange(): void {
        this.flushValueCoalesce();
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

/** 字段级 embed 状态键：状态寄存在父对象的 `$embed_<fieldName>` 上（数组挂不了属性）。
 *  语义「true=收起，false=展开，键只存当前类的非默认值」：类 1（自动嵌入）默认收起、写 false 展开；
 *  类 2（list 嵌入）默认展开、写 true 收起。session 写入侧与 creator 读取侧共用此函数，避免键格式漂移。 */
export function embedKey(fieldName: string): string {
    return `$embed_${fieldName}`;
}

/** KeepStable 锚点位置：pickViewportAction 的 KeepStable 分支只读 id，x/y 为满足 EntityPosition 形状的占位。
 *  undo/redo 与正向删除三处共用，避免各处手搓 {id, x: 0, y: 0}。 */
function anchorPosition(id: string): EntityPosition {
    return {id, x: 0, y: 0};
}

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

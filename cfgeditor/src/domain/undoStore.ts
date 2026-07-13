import {JSONObject} from '@/api/recordModel';

/**
 * Snapshot —— editingObject 的一份独立深拷贝。
 *
 * 必须独立（structuredClone），不能存 editingObject / editObj 的引用——它们会被后续就地变异污染，
 * 存引用等于存一个会被改的活对象，undo 时早已面目全非。clone 由 session 的 captureUndoPoint 负责，
 * UndoStore 只存调用方传入的已 clone 对象（不二次 clone）。
 */
type Snapshot = JSONObject;

/**
 * UndoStore —— undo/redo 的纯数据栈（快照栈模式）。
 *
 * 不依赖 session、不调 React——只管 baseline / done / undone 三段。session 负责 capture/apply 时机
 * 与 bumpStructure 驱动 React。这样栈语义可独立单测（栈深、分叉、baseline 栈底）。
 *
 * 三段语义：
 * - baseline：初始 / 最近一次提交后的状态。undo 到栈底恢复成它（显式存住，无 off-by-one）。
 * - done：操作后快照；done[末] = 最近。capture 入栈、popUndo 弹出。
 * - undone：已 undo、可 redo。popRedo 弹出。
 *
 * 分叉：capture 清空 undone（undo 后又新编辑，redo 历史作废，与所有编辑器一致）。
 * maxDepth：栈深硬上限，超限丢弃最旧（大 record 的内存兜底）。
 */
export class UndoStore {
    private baseline!: Snapshot;
    private done: Snapshot[] = [];
    private undone: Snapshot[] = [];
    private readonly maxDepth = 50;

    /** 初始 / 提交后调：重置基准，清栈。session 构造后由 mount effect 调一次、提交成功后调一次。 */
    setBaseline(s: Snapshot): void {
        this.baseline = s;
        this.done = [];
        this.undone = [];
    }

    /** 每次编辑后调：入栈新快照，丢弃 redo 历史（分叉）。超 maxDepth 丢最旧。 */
    capture(s: Snapshot): void {
        this.undone = [];
        this.done.push(s);
        if (this.done.length > this.maxDepth) {
            this.done.shift();
        }
    }

    canUndo(): boolean {
        return this.done.length > 0;
    }

    canRedo(): boolean {
        return this.undone.length > 0;
    }

    /** undo：弹出最近快照，返回"要恢复成的状态"（前一个快照或 baseline）。调用前应 canUndo() 守卫。 */
    popUndo(): Snapshot {
        const s = this.done.pop()!;
        this.undone.push(s);
        return this.done.length > 0 ? this.done[this.done.length - 1] : this.baseline;
    }

    /** redo：返回"要恢复成的状态"（刚 redo 的快照）。调用前应 canRedo() 守卫。 */
    popRedo(): Snapshot {
        const s = this.undone.pop()!;
        this.done.push(s);
        return s;
    }
}

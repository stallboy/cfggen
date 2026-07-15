import {JSONObject} from '@/api/recordModel';
import {EFitView} from '@/domain/entityModel';

/**
 * Snapshot —— editingObject 的一份独立深拷贝 + undo/redo 视口语义。
 *
 * data 必须独立（structuredClone），不能存 editingObject / editObj 的引用——它们会被后续就地变异污染，
 * 存引用等于存一个会被改的活对象。clone 由 session 的 captureUndoPoint 负责，UndoStack 只存调用方传入
 * 的已 clone 对象（不二次 clone）。
 *
 * undoFitView / anchorId 记录「产生此快照的操作」的视口语义，供 undo/redo 弹出该快照时按其语义驱动视口
 * （结构操作 → KeepStable + 锚点；整体替换 → FitFull；值类 → NoChange）。详见 docs/undo-redo.md 第四部分。
 */
export type Snapshot = {
    data: JSONObject;
    /** undo/redo 到此快照后该用的 fitView。*/
    undoFitView: EFitView;
    /** KeepStable 时的锚点节点 id（= 产生此快照的操作的视觉焦点；delete 取父）。*/
    anchorId?: string;
};

/**
 * UndoStack —— undo/redo 的纯数据栈（快照栈模式）。
 *
 * 不依赖 session、不调 React——只管 baseline / done / undone 三段。session 负责 capture/apply 时机
 * 与 bumpStructure 驱动 React。这样栈语义可独立单测（栈深、分叉、baseline 栈底、视口语义随快照）。
 *
 * 三段语义：
 * - baseline：初始 / 最近一次提交后的状态。undo 到栈底恢复成它（显式存住，无 off-by-one）。
 * - done：操作后快照；done[末] = 最近。capture 入栈、popUndo 弹出。
 * - undone：已 undo、可 redo。popRedo 弹出。
 *
 * 分叉：capture 清空 undone（undo 后又新编辑，redo 历史作废，与所有编辑器一致）。
 * maxDepth：栈深硬上限，超限丢弃最旧（大 record 的内存兜底）。
 */
export class UndoStack {
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

    /** undo：弹出栈顶快照（被撤销操作的快照），返回"要恢复成的状态"（target=前一个/baseline）
     *  + 被撤销操作的视口语义（undoFitView/anchorId 取自被弹出的栈顶，即被撤销操作的焦点）。调用前应 canUndo() 守卫。 */
    popUndo(): { target: Snapshot; undoFitView: EFitView; anchorId?: string } {
        const s = this.done.pop()!;
        this.undone.push(s);
        const target = this.done.length > 0 ? this.done[this.done.length - 1] : this.baseline;
        return {target, undoFitView: s.undoFitView, anchorId: s.anchorId};
    }

    /** redo：返回"要恢复成的状态"（target=刚 redo 的快照） + 其视口语义。调用前应 canRedo() 守卫。 */
    popRedo(): { target: Snapshot; undoFitView: EFitView; anchorId?: string } {
        const s = this.undone.pop()!;
        this.done.push(s);
        return {target: s, undoFitView: s.undoFitView, anchorId: s.anchorId};
    }
}

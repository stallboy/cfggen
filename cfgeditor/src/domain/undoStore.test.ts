import {describe, it, expect} from 'vitest'
import {UndoStore, Snapshot} from './undoStore.ts'
import {EFitView} from '@/domain/entityModel'
import {JSONObject} from '@/api/recordModel'

// snap：构造快照。undoFitView 默认 NoChange（值类），anchorId 可选（KeepStable 用）。
const snap = (o: Record<string, unknown>, undoFitView: EFitView = EFitView.NoChange, anchorId?: string): Snapshot =>
    ({data: o as JSONObject, undoFitView, anchorId});

describe('UndoStore 栈语义', () => {
    it('setBaseline 后 canUndo/canRedo 均 false', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        expect(u.canUndo()).toBe(false)
        expect(u.canRedo()).toBe(false)
    })

    it('capture 后 canUndo true、canRedo false', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        u.capture(snap({a: 2}))
        expect(u.canUndo()).toBe(true)
        expect(u.canRedo()).toBe(false)
    })

    it('undo：target 恢复成前一个快照（栈底回 baseline）；之后 canRedo true', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        u.capture(snap({a: 2}))
        expect(u.popUndo().target.data).toEqual({a: 1})   // 回到 baseline
        expect(u.canUndo()).toBe(false)
        expect(u.canRedo()).toBe(true)
    })

    it('redo：target 恢复成刚 undo 的快照；之后 canUndo true', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        u.capture(snap({a: 2}))
        u.popUndo()
        expect(u.popRedo().target.data).toEqual({a: 2})
        expect(u.canUndo()).toBe(true)
        expect(u.canRedo()).toBe(false)
    })

    it('多步 undo 逐级回退到 baseline', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        u.capture(snap({a: 2}))
        u.capture(snap({a: 3}))
        expect(u.popUndo().target.data).toEqual({a: 2})
        expect(u.popUndo().target.data).toEqual({a: 1})   // baseline
        expect(u.canUndo()).toBe(false)
    })

    it('多步 redo 逐级前进到最近', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        u.capture(snap({a: 2}))
        u.capture(snap({a: 3}))
        u.popUndo()
        u.popUndo()
        expect(u.popRedo().target.data).toEqual({a: 2})
        expect(u.popRedo().target.data).toEqual({a: 3})
        expect(u.canRedo()).toBe(false)
    })
})

describe('UndoStore 视口语义（undoFitView/anchorId 随被弹出快照）', () => {
    it('popUndo 返回被撤销操作快照的 undoFitView/anchorId（结构→KeepStable+锚点）', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}, EFitView.FitFull))
        u.capture(snap({a: 2}, EFitView.KeepStable, 'nodeX'))   // 结构操作
        const r = u.popUndo()
        expect(r.undoFitView).toBe(EFitView.KeepStable)   // 被撤销的是结构操作
        expect(r.anchorId).toBe('nodeX')
        expect(r.target.data).toEqual({a: 1})              // 恢复成 baseline
    })

    it('popRedo 返回重做操作快照的 undoFitView/anchorId', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}, EFitView.FitFull))
        u.capture(snap({a: 2}, EFitView.KeepStable, 'nodeX'))
        u.popUndo()
        const r = u.popRedo()
        expect(r.undoFitView).toBe(EFitView.KeepStable)
        expect(r.anchorId).toBe('nodeX')
        expect(r.target.data).toEqual({a: 2})
    })
})

describe('UndoStore 分叉（capture 清 undone）', () => {
    it('undo 后新 capture 丢弃 redo 历史', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        u.capture(snap({a: 2}))
        u.capture(snap({a: 3}))
        u.popUndo()                  // 回到 {a:2}
        expect(u.canRedo()).toBe(true)
        u.capture(snap({a: 9}))      // 分叉：新编辑
        expect(u.canRedo()).toBe(false)   // redo 历史作废
        expect(u.popUndo().target.data).toEqual({a: 2})   // 回到 {a:2}
    })

    it('redo 后新 capture 也丢弃 redo 历史', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        u.capture(snap({a: 2}))
        u.popUndo()
        u.popRedo()                  // 前进到 {a:2}
        expect(u.canRedo()).toBe(false)
        u.capture(snap({a: 9}))
        expect(u.canRedo()).toBe(false)
        expect(u.popUndo().target.data).toEqual({a: 2})
    })
})

describe('UndoStore maxDepth 封顶', () => {
    it('超 maxDepth 丢弃最旧，undo 到栈底仍回 baseline', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 0}))
        for (let i = 1; i <= 60; i++) u.capture(snap({a: i}))
        // done=[a11..a60]（maxDepth=50 封顶），baseline=a0
        expect(u.popUndo().target.data).toEqual({a: 59})   // 弹 a60 返 a59（最近未丢）
        let last: JSONObject = snap({}).data
        while (u.canUndo()) last = u.popUndo().target.data
        expect(last).toEqual({a: 0})   // 栈底回到 baseline
    })

    it('setBaseline 清栈（提交后 redo 历史也清）', () => {
        const u = new UndoStore()
        u.setBaseline(snap({a: 1}))
        u.capture(snap({a: 2}))
        u.popUndo()
        expect(u.canRedo()).toBe(true)
        u.setBaseline(snap({a: 5}))   // 提交后重基准
        expect(u.canUndo()).toBe(false)
        expect(u.canRedo()).toBe(false)
    })
})

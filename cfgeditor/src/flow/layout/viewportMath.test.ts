import {describe, it, expect} from 'vitest'
import {pickViewportAction, Viewport, Point} from './viewportMath.ts'
import {EFitView, EditingObjectRes} from '@/domain/entityModel'

describe('pickViewportAction', () => {
    const vp: Viewport = {x: 50, y: 60, zoom: 1.5}
    const rectAt = (x: number, y: number) => ({x, y, width: 10, height: 10})
    const makeRes = (fitView: EFitView, position?: { id: string; x: number; y: number }): EditingObjectRes => ({
        fitView,
        fitViewToIdPosition: position,
        isEdited: true,
    })
    // screenOf 已内联（screen = world * zoom + vp），用于断言锚点屏幕不变量
    const screenOf = (p: Point, v: Viewport) => ({x: p.x * v.zoom + v.x, y: p.y * v.zoom + v.y})

    it('undefined editingObjectRes → fitFull', () => {
        const map = new Map([['a', rectAt(0, 0)]])
        expect(pickViewportAction(undefined, map, vp)).toEqual({kind: 'fitFull'})
    })

    it('FitFull → fitFull', () => {
        const map = new Map([['a', rectAt(0, 0)]])
        expect(pickViewportAction(makeRes(EFitView.FitFull), map, vp)).toEqual({kind: 'fitFull'})
    })

    it('FitId + 命中 id → fitId，且锚点屏幕坐标不变、缩放保持（多组形状）', () => {
        const cases: Array<{ anchorOld: Point; anchorNew: Point; vp: Viewport }> = [
            {anchorOld: {x: 100, y: 200}, anchorNew: {x: 300, y: 400}, vp: {x: 50, y: 60, zoom: 1.5}},
            {anchorOld: {x: 0, y: 0}, anchorNew: {x: 0, y: 0}, vp: {x: 0, y: 0, zoom: 1}},       // 未移动 → 视口不变
            {anchorOld: {x: -50, y: -50}, anchorNew: {x: 50, y: 50}, vp: {x: 12, y: 34, zoom: 3}},
            {anchorOld: {x: 1000, y: -200}, anchorNew: {x: -300, y: 800}, vp: {x: -99, y: 7, zoom: 0.25}},
        ]
        for (const {anchorOld, anchorNew, vp: caseVp} of cases) {
            const map = new Map([['n1', rectAt(anchorNew.x, anchorNew.y)]])
            const action = pickViewportAction(
                makeRes(EFitView.FitId, {id: 'n1', x: anchorOld.x, y: anchorOld.y}),
                map,
                caseVp,
            )
            expect(action.kind).toBe('fitId')
            if (action.kind !== 'fitId') continue
            // 不变量：screenOf(anchorOld, vp) === screenOf(anchorNew, action.viewport)
            const before = screenOf(anchorOld, caseVp)
            const after = screenOf(anchorNew, action.viewport)
            expect(after.x).toBeCloseTo(before.x, 10)
            expect(after.y).toBeCloseTo(before.y, 10)
            expect(action.viewport.zoom).toBe(caseVp.zoom)   // 缩放保持
        }
    })

    it('FitId + id 不存在 → noop（删除节点场景）', () => {
        const map = new Map([['other', rectAt(0, 0)]])
        expect(pickViewportAction(makeRes(EFitView.FitId, {id: 'gone', x: 0, y: 0}), map, vp))
            .toEqual({kind: 'noop'})
    })

    it('FitId + 无 position → noop', () => {
        const map = new Map([['a', rectAt(0, 0)]])
        expect(pickViewportAction(makeRes(EFitView.FitId), map, vp)).toEqual({kind: 'noop'})
    })

    it('NoChange → noop（undo/redo、只读、固定页）', () => {
        const map = new Map([['a', rectAt(0, 0)]])
        expect(pickViewportAction(makeRes(EFitView.NoChange), map, vp)).toEqual({kind: 'noop'})
    })

    it('KeepStable + 命中 prev/new → fitId，且锚点屏幕不变、缩放保持（anchorOld 取自 prevMap）', () => {
        const prevMap = new Map([['n1', rectAt(100, 200)]])
        const newMap = new Map([['n1', rectAt(300, 400)]])
        const action = pickViewportAction(
            makeRes(EFitView.KeepStable, {id: 'n1', x: 0, y: 0}),   // x/y 不用（KeepStable 用 prevMap 坐标）
            newMap, vp, {prevId2RectMap: prevMap},
        )
        expect(action.kind).toBe('fitId')
        if (action.kind !== 'fitId') return
        const before = screenOf({x: 100, y: 200}, vp)
        const after = screenOf({x: 300, y: 400}, action.viewport)
        expect(after.x).toBeCloseTo(before.x, 10)
        expect(after.y).toBeCloseTo(before.y, 10)
        expect(action.viewport.zoom).toBe(vp.zoom)
    })

    it('KeepStable + 锚点在新布局缺失 → noop', () => {
        const prevMap = new Map([['n1', rectAt(0, 0)]])
        const newMap = new Map([['other', rectAt(0, 0)]])
        expect(pickViewportAction(makeRes(EFitView.KeepStable, {id: 'n1', x: 0, y: 0}), newMap, vp, {prevId2RectMap: prevMap}))
            .toEqual({kind: 'noop'})
    })

    it('KeepStable + 缺 prevId2RectMap → noop（防御）', () => {
        const newMap = new Map([['n1', rectAt(0, 0)]])
        expect(pickViewportAction(makeRes(EFitView.KeepStable, {id: 'n1', x: 0, y: 0}), newMap, vp))
            .toEqual({kind: 'noop'})
    })

    it('KeepStable + 无 fitViewToIdPosition → noop（防御）', () => {
        const prevMap = new Map([['n1', rectAt(0, 0)]])
        const newMap = new Map([['n1', rectAt(0, 0)]])
        expect(pickViewportAction(makeRes(EFitView.KeepStable), newMap, vp, {prevId2RectMap: prevMap}))
            .toEqual({kind: 'noop'})
    })

    it('KeepStable + 锚点在 prev 缺失（仅 new 命中）→ noop', () => {
        const prevMap = new Map([['other', rectAt(0, 0)]])
        const newMap = new Map([['n1', rectAt(0, 0)]])
        expect(pickViewportAction(makeRes(EFitView.KeepStable, {id: 'n1', x: 0, y: 0}), newMap, vp, {prevId2RectMap: prevMap}))
            .toEqual({kind: 'noop'})
    })

    it('KeepStable + prev/new 逐位相同（值类等价）→ 补偿视口 == currentVp（不动）', () => {
        const prevMap = new Map([['n1', rectAt(50, 50)]])
        const newMap = new Map([['n1', rectAt(50, 50)]])
        const action = pickViewportAction(makeRes(EFitView.KeepStable, {id: 'n1', x: 0, y: 0}), newMap, vp, {prevId2RectMap: prevMap})
        expect(action.kind).toBe('fitId')
        if (action.kind !== 'fitId') return
        expect(action.viewport).toEqual(vp)
    })
})

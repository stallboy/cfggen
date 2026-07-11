import {describe, it, expect} from 'vitest'
import {computeStableViewport, screenOf, Viewport, Point} from './viewportMath.ts'

// 不变量：relayout 前后锚点屏幕坐标相等。这是 computeStableViewport 的核心契约。
function expectAnchorStableOnScreen(anchorOld: Point, anchorNew: Point, vp: Viewport) {
    const newVp = computeStableViewport(anchorOld, anchorNew, vp)
    const before = screenOf(anchorOld, vp)
    const after = screenOf(anchorNew, newVp)
    expect(after.x).toBeCloseTo(before.x, 10)
    expect(after.y).toBeCloseTo(before.y, 10)
}

describe('screenOf', () => {
    it('原点世界坐标 = 视口平移量', () => {
        expect(screenOf({x: 0, y: 0}, {x: 100, y: 50, zoom: 2})).toEqual({x: 100, y: 50})
    })

    it('world*zoom + translation', () => {
        // 3*2+10=16, 4*2+20=28
        expect(screenOf({x: 3, y: 4}, {x: 10, y: 20, zoom: 2})).toEqual({x: 16, y: 28})
    })

    it('zoom=1 退化为纯平移', () => {
        expect(screenOf({x: 5, y: 7}, {x: -3, y: 2, zoom: 1})).toEqual({x: 2, y: 9})
    })
})

describe('computeStableViewport', () => {
    it('锚点 relayout 后屏幕坐标不变（一般情形）', () => {
        expectAnchorStableOnScreen(
            {x: 100, y: 200},
            {x: 300, y: 400},
            {x: 50, y: 60, zoom: 1.5},
        )
    })

    it('锚点未移动时视口不变', () => {
        const vp = {x: 50, y: 60, zoom: 2}
        const same = {x: 100, y: 200}
        expect(computeStableViewport(same, same, vp)).toEqual(vp)
    })

    it('缩放保持不变', () => {
        const vp = {x: 0, y: 0, zoom: 0.7}
        const newVp = computeStableViewport({x: 10, y: 10}, {x: 90, y: 90}, vp)
        expect(newVp.zoom).toBe(vp.zoom)
    })

    it('放大位移补偿：锚点右移则视口左移以抵消', () => {
        // anchorNew.x > anchorOld.x，为保持屏幕不动，newVp.x 必须减小
        const vp = {x: 0, y: 0, zoom: 1}
        const newVp = computeStableViewport({x: 0, y: 0}, {x: 100, y: 0}, vp)
        expect(newVp.x).toBe(-100)
    })

    it('多组随机形状均满足不变量', () => {
        const cases: Array<[Point, Point, Viewport]> = [
            [{x: 0, y: 0}, {x: 0, y: 0}, {x: 0, y: 0, zoom: 1}],
            [{x: -50, y: -50}, {x: 50, y: 50}, {x: 12, y: 34, zoom: 3}],
            [{x: 1000, y: -200}, {x: -300, y: 800}, {x: -99, y: 7, zoom: 0.25}],
        ]
        for (const [oldP, newP, vp] of cases) {
            expectAnchorStableOnScreen(oldP, newP, vp)
        }
    })
})

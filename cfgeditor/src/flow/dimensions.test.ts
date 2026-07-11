import {describe, it, expect} from 'vitest'
import {DEFAULT_NODE_WIDTH, DEFAULT_EDIT_NODE_WIDTH, getReadNodeWidth, getEditNodeWidth, getNodeWidth} from './dimensions.ts'
import {makeNodeShow, makeReadOnly, makeEditable, makeCard, editWith, withShared} from '@/test/fixtures'

describe('常量', () => {
    it('默认宽度 240/280 与全项目一致', () => {
        expect(DEFAULT_NODE_WIDTH).toBe(240)
        expect(DEFAULT_EDIT_NODE_WIDTH).toBe(280)
    })
})

describe('getReadNodeWidth', () => {
    it('nodeShow 缺失时兜底 240', () => {
        expect(getReadNodeWidth(undefined)).toBe(240)
    })

    it('nodeShow.nodeWidth 缺失时兜底 240', () => {
        expect(getReadNodeWidth(makeNodeShow({} as never))).toBe(240)
    })

    it('nodeShow.nodeWidth 覆盖默认', () => {
        expect(getReadNodeWidth(makeNodeShow({nodeWidth: 300}))).toBe(300)
    })
})

describe('getEditNodeWidth', () => {
    it('nodeShow 缺失时兜底 280', () => {
        expect(getEditNodeWidth(undefined)).toBe(280)
    })

    it('nodeShow.editNodeWidth 覆盖默认', () => {
        expect(getEditNodeWidth(makeNodeShow({editNodeWidth: 400}))).toBe(400)
    })
})

describe('getNodeWidth', () => {
    // 4 组合矩阵：edit×{缺省,覆盖} × 非edit×{缺省,覆盖}
    it('editable + 缺省 nodeShow → 280', () => {
        expect(getNodeWidth(makeEditable({id: '1', label: 'x', edit: editWith([])}))).toBe(280)
    })

    it('editable + 覆盖 editNodeWidth → 覆盖值', () => {
        const e = withShared(makeEditable({id: '1', label: 'x', edit: editWith([])}), makeNodeShow({editNodeWidth: 500}))
        expect(getNodeWidth(e)).toBe(500)
    })

    it('非 edit(readonly) + 缺省 nodeShow → 240', () => {
        expect(getNodeWidth(makeReadOnly({id: '1', label: 'x', fields: []}))).toBe(240)
    })

    it('非 edit(readonly) + 覆盖 nodeWidth → 覆盖值', () => {
        const e = withShared(makeReadOnly({id: '1', label: 'x', fields: []}), makeNodeShow({nodeWidth: 320}))
        expect(getNodeWidth(e)).toBe(320)
    })

    it('card 实体（非 edit）走 read 宽度', () => {
        const e = withShared(
            makeCard({id: '1', label: 'x', brief: {value: ''}}),
            makeNodeShow({nodeWidth: 199}),
        )
        expect(getNodeWidth(e)).toBe(199)
    })
})

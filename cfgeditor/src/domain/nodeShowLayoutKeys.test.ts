import {describe, it, expect} from 'vitest'
import {NODESHOW_LAYOUT_KEYS, pickLayoutKeys, layoutKeysChanged} from './nodeShowLayoutKeys'
import {makeNodeShow} from '@/test/fixtures'

describe('NODESHOW_LAYOUT_KEYS', () => {
    it('包含布局/拓扑字段，排除纯颜色/显示字段', () => {
        // 布局相关
        expect(NODESHOW_LAYOUT_KEYS).toContain('recordLayout')
        expect(NODESHOW_LAYOUT_KEYS).toContain('mrtreeSpacing')
        expect(NODESHOW_LAYOUT_KEYS).toContain('nodeWidth')
        expect(NODESHOW_LAYOUT_KEYS).toContain('refShowDescription')
        // 拓扑相关（影响建哪些实体）
        expect(NODESHOW_LAYOUT_KEYS).toContain('refTableHides')
        expect(NODESHOW_LAYOUT_KEYS).toContain('refContainEnum')
        // 纯颜色/显示——不应在此
        expect(NODESHOW_LAYOUT_KEYS).not.toContain('nodeColor')
        expect(NODESHOW_LAYOUT_KEYS).not.toContain('edgeColor')
        expect(NODESHOW_LAYOUT_KEYS).not.toContain('editFoldColor')
        expect(NODESHOW_LAYOUT_KEYS).not.toContain('refIsShowCopyable')
    })
})

describe('pickLayoutKeys', () => {
    it('只挑出布局相关字段（颜色字段被排除）', () => {
        const ns = makeNodeShow({nodeColor: '#ABC', recordLayout: 'mrtree', nodeWidth: 300})
        const picked = pickLayoutKeys(ns)
        expect(picked.recordLayout).toBe('mrtree')
        expect(picked.nodeWidth).toBe(300)
        expect((picked as Record<string, unknown>).nodeColor).toBeUndefined()
    })
})

describe('layoutKeysChanged', () => {
    it('纯颜色字段变化 → false（不应清缓存/重布局）', () => {
        const a = makeNodeShow({nodeColor: '#111'})
        const b = makeNodeShow({nodeColor: '#222'})
        expect(layoutKeysChanged(a, b)).toBe(false)
    })

    it('布局算法字段变化 → true', () => {
        expect(layoutKeysChanged(
            makeNodeShow({recordLayout: 'SIMPLE'}),
            makeNodeShow({recordLayout: 'mrtree'}),
        )).toBe(true)
    })

    it('节点宽度变化 → true（影响 ELK 边界框）', () => {
        expect(layoutKeysChanged(
            makeNodeShow({nodeWidth: 240}),
            makeNodeShow({nodeWidth: 300}),
        )).toBe(true)
    })

    it('refShowDescription 变化 → true（影响 card 高度）', () => {
        expect(layoutKeysChanged(
            makeNodeShow({refShowDescription: 'show'}),
            makeNodeShow({refShowDescription: 'none'}),
        )).toBe(true)
    })

    it('拓扑字段 refTableHides 变化 → true', () => {
        expect(layoutKeysChanged(
            makeNodeShow({refTableHides: []}),
            makeNodeShow({refTableHides: ['item']}),
        )).toBe(true)
    })

    it('仅颜色字段不同的浅拷贝 → false', () => {
        const a = makeNodeShow({nodeColor: '#x', edgeColor: '#y', editFoldColor: '#z'})
        expect(layoutKeysChanged(a, {...a})).toBe(false)
    })

    it('refTableHides 同内容不同引用（表单典型）→ false（按值比，不被误判）', () => {
        // makeNodeShow 每次新建 [] —— 两个独立对象但 refTableHides 内容相同
        const a = makeNodeShow({nodeColor: '#1', refTableHides: ['x', 'y']})
        const b = makeNodeShow({nodeColor: '#2', refTableHides: ['x', 'y']})
        // 颜色不同 + refTableHides 同内容不同引用 → 仍 false（仅颜色变了）
        expect(a.refTableHides).not.toBe(b.refTableHides) // 引用确不同
        expect(layoutKeysChanged(a, b)).toBe(false)
    })
})

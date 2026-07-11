import {describe, it, expect} from 'vitest'
import {NODESHOW_LAYOUT_KEYS, pickLayoutKeys} from './nodeShowLayoutKeys'
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

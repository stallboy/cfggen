import {describe, it, expect} from 'vitest'
import {fixColor, fixColors} from './colorUtils'
import {NODE_SHOW_DEFAULTS} from '@/flow/colors'

describe('fixColor', () => {
    it('string 入参 → 原样返回（不走默认色）', () => {
        expect(fixColor('#ff0000')).toBe('#ff0000')
        expect(fixColor('red')).toBe('red')
    })

    it('antd ColorPicker 的 {toHexString} 对象 → 调用其 toHexString()', () => {
        expect(fixColor({toHexString: () => '#00ff00'})).toBe('#00ff00')
    })

    it('null → 默认色 NODE_SHOW_DEFAULTS.nodeColor', () => {
        expect(fixColor(null)).toBe(NODE_SHOW_DEFAULTS.nodeColor)
    })

    it('undefined → 默认色', () => {
        expect(fixColor(undefined)).toBe(NODE_SHOW_DEFAULTS.nodeColor)
    })

    it('自定义 defaultColor：仅在 null/undefined 时生效，string 仍优先', () => {
        expect(fixColor(null, '#abc')).toBe('#abc')
        expect(fixColor(undefined, '#abc')).toBe('#abc')
        expect(fixColor('red', '#abc')).toBe('red')
    })
})

describe('fixColors', () => {
    it('映射数组：每项 color 经 fixColor 规整（混合三种入参形态）', () => {
        const r = fixColors([
            {keyword: 'a', color: '#111'},
            {keyword: 'b', color: {toHexString: () => '#222'}},
            {keyword: 'c', color: null},
        ])
        expect(r).toEqual([
            {keyword: 'a', color: '#111'},
            {keyword: 'b', color: '#222'},
            {keyword: 'c', color: NODE_SHOW_DEFAULTS.nodeColor},
        ])
    })

    it('空数组 → 空数组', () => {
        expect(fixColors([])).toEqual([])
    })

    it('透传自定义 defaultColor 给每项', () => {
        expect(fixColors([{keyword: 'x', color: null}], '#def')).toEqual([{keyword: 'x', color: '#def'}])
    })
})

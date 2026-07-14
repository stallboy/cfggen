import {describe, it, expect} from 'vitest'
import {simpleStrRowCount} from './calcWidthHeight.ts'

// simpleStrRowCount：card desc 占用行数估算。
// 行数仅在「宽度累计 >= charsPerRow 换行」或「遇到 \n」时 +1（与历史行为一致，不计末尾半行）。

describe('simpleStrRowCount - ASCII（窄字符 = 1 宽）', () => {
    it('charsPerRow=30：恰好 30 个 ASCII 换行 → 1 行', () => {
        expect(simpleStrRowCount('a'.repeat(30), 30)).toBe(1)
    })
    it('29 个 ASCII 不换行 → 0 行', () => {
        expect(simpleStrRowCount('a'.repeat(29), 30)).toBe(0)
    })
    it('60 个 ASCII → 2 行', () => {
        expect(simpleStrRowCount('a'.repeat(60), 30)).toBe(2)
    })
    it('不传 charsPerRow 时默认 30', () => {
        expect(simpleStrRowCount('a'.repeat(30))).toBe(1)
    })
})

describe('simpleStrRowCount - CJK（宽字符 = 2 宽）', () => {
    it('15 个中文 = 30 宽 → 1 行（与 30 个 ASCII 等宽）', () => {
        expect(simpleStrRowCount('中'.repeat(15), 30)).toBe(1)
    })
    it('30 个中文 = 60 宽 → 2 行', () => {
        expect(simpleStrRowCount('中'.repeat(30), 30)).toBe(2)
    })
    it('中文与 ASCII 混排按各自宽度累计', () => {
        // 10 中文(20) + 10 ASCII(10) = 30 宽 → 1 行
        expect(simpleStrRowCount('中'.repeat(10) + 'a'.repeat(10), 30)).toBe(1)
    })
})

describe('simpleStrRowCount - emoji（1 grapheme = 2 宽，不再被计成 4）', () => {
    it('15 个 emoji = 30 宽 → 1 行（旧 charCodeAt 实现会算成 2 行）', () => {
        // 😀 = U+1F600，1 个 grapheme、2 宽。旧实现：2 个 code unit × 2 宽 = 4 宽 → 15 个 = 60 宽 = 2 行
        expect(simpleStrRowCount('😀'.repeat(15), 30)).toBe(1)
    })
})

describe('simpleStrRowCount - 换行符', () => {
    it('显式 \\n 增加行数', () => {
        expect(simpleStrRowCount('a\nb', 30)).toBe(1) // 'a' + \n(row1) + 'b'
    })
    it('多个 \\n', () => {
        expect(simpleStrRowCount('a\nb\nc', 30)).toBe(2)
    })
    it('CRLF(\\r\\n) 与单 \\n 等价（Intl.Segmenter 把 \\r\\n 合并成 1 个 grapheme）', () => {
        // Segmenter 路径下 \r\n 是一个 grapheme，串为 '\r\n'；includes('\n') 兜住，与 \n 同计一行
        expect(simpleStrRowCount('a\r\nb', 30)).toBe(1)
        expect(simpleStrRowCount('a\r\nb\r\nc', 30)).toBe(2)
        // 与纯 \n 结果一致
        expect(simpleStrRowCount('a\r\nb', 30)).toBe(simpleStrRowCount('a\nb', 30))
    })
})

describe('simpleStrRowCount - East Asian Width 边界', () => {
    it('Latin Extended（ą = U+0105，>255 但非 CJK）按 1 宽（旧 >255 误计为 2）', () => {
        // 旧实现 30 个 ą = 60 宽 = 2 行；新实现 = 30 宽 = 1 行
        expect(simpleStrRowCount('ą'.repeat(30), 30)).toBe(1)
    })
    it('全角字符（Ａ = U+FF21）按 2 宽', () => {
        expect(simpleStrRowCount('Ａ'.repeat(15), 30)).toBe(1) // 15 × 2 = 30 宽
        expect(simpleStrRowCount('Ａ'.repeat(30), 30)).toBe(2)
    })
})

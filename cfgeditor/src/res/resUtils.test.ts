import {describe, it, expect} from 'vitest'
import {findKeyEndIndex, sepParentDirAndFilename, joinPath} from './resUtils.ts'

describe('findKeyEndIndex', () => {
    // 找「第二个分隔符」的下标：'_' 标记首个分隔已出现，其后遇到的 '_' 或 '.' 即返回。
    it('无任何分隔符返回 -1', () => {
        expect(findKeyEndIndex('abc')).toBe(-1)
        expect(findKeyEndIndex('')).toBe(-1)
    })

    it('单个 _ 不触发返回（仅标记 foundFirst）', () => {
        expect(findKeyEndIndex('a_b')).toBe(-1)
        expect(findKeyEndIndex('_a')).toBe(-1)
    })

    it('两个 _ 时返回第二个 _ 的下标', () => {
        // a _ b _ c → 下标 0:a 1:_ 2:b 3:_ 4:c
        expect(findKeyEndIndex('a_b_c')).toBe(3)
    })

    it('_ 之后的 . 触发返回该 . 的下标', () => {
        // a _ b . c → 下标 0:a 1:_ 2:b 3:. 4:c
        expect(findKeyEndIndex('a_b.c')).toBe(3)
    })

    it('. 若未先出现 _ 则被忽略，不触发返回', () => {
        expect(findKeyEndIndex('a.b')).toBe(-1)
        expect(findKeyEndIndex('a.b.c')).toBe(-1)
    })

    it('. 之后才出现 _ 时仍需第二个分隔符才返回', () => {
        // a . b _ c → '.' 忽略，'_' 仅标记，无第二个分隔符
        expect(findKeyEndIndex('a.b_c')).toBe(-1)
    })
})

describe('sepParentDirAndFilename', () => {
    it('以 / 分隔时拆出父目录与文件名', () => {
        expect(sepParentDirAndFilename('a/b/c')).toEqual([true, 'a/b', 'c'])
    })

    it('以 \\ 分隔时同样拆分', () => {
        expect(sepParentDirAndFilename('a\\b\\c')).toEqual([true, 'a\\b', 'c'])
    })

    it('混合分隔符时取最后一个分隔符（/ 或 \\）', () => {
        // a/b\c → 最后分隔符为下标 3 的 '\'
        expect(sepParentDirAndFilename('a/b\\c')).toEqual([true, 'a/b', 'c'])
    })

    it('无分隔符时 hasParent=false，parentDir 为空，整体作为文件名', () => {
        expect(sepParentDirAndFilename('filename')).toEqual([false, '', 'filename'])
    })

    it('根路径 /abs 拆出空父目录', () => {
        expect(sepParentDirAndFilename('/abs')).toEqual([true, '', 'abs'])
    })

    it('以分隔符结尾时文件名为空字符串', () => {
        expect(sepParentDirAndFilename('a/b/')).toEqual([true, 'a/b', ''])
    })
})

describe('joinPath', () => {
    // path.sep() 由 src/test/setup.ts 的 Tauri shim 固定为 '/'
    it('简单拼接：baseDir + sep + path', () => {
        expect(joinPath('a/b', 'c')).toEqual([true, 'a/b/c'])
    })

    it('剥离 baseDir 末尾的 /', () => {
        expect(joinPath('a/b/', 'c')).toEqual([true, 'a/b/c'])
    })

    it('剥离 baseDir 末尾的 \\', () => {
        expect(joinPath('a/b\\', 'c')).toEqual([true, 'a/b/c'])
    })

    it('../ 消解一级父目录', () => {
        expect(joinPath('a/b', '../c')).toEqual([true, 'a/c'])
    })

    it('多个 ../ 逐级消解', () => {
        expect(joinPath('a/b/c', '../../d')).toEqual([true, 'a/d'])
    })

    it('..\\ (反斜杠) 同样消解父目录', () => {
        expect(joinPath('a/b', '..\\c')).toEqual([true, 'a/c'])
    })

    it('../ 超过根目录时返回 [false, 剥离后的 path]', () => {
        // a 的父目录不存在 → 失败，selfPath 此时为 "b"
        expect(joinPath('a', '../b')).toEqual([false, 'b'])
    })

    it('baseDir 为空字符串时结果以 / 开头', () => {
        expect(joinPath('', 'x')).toEqual([true, '/x'])
    })
})

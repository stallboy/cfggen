import {describe, it, expect} from 'vitest'
import {structCopy, getCopiedObject, isCopiedFitAllowedType} from './clipboard'

// 模块级 copiedObject 跨用例共享：每个 it 先 structCopy 重置，避免顺序耦合（FIRST 的 Independent）
function setCopiedType($type: string) {
    structCopy({$type})
}

describe('structCopy / getCopiedObject', () => {
    it('structCopy 后 getCopiedObject 返回等值深拷贝（引用 ≠ 原对象）', () => {
        const obj = {$type: 'Dog', hp: 100}
        structCopy(obj)
        const got = getCopiedObject()
        expect(got).toEqual(obj)
        expect(got).not.toBe(obj)  // structuredClone 深拷贝
    })
})

describe('isCopiedFitAllowedType', () => {
    it('完全相等 → true（第一条 == 命中）', () => {
        setCopiedType('IPet')
        expect(isCopiedFitAllowedType('IPet')).toBe(true)
        setCopiedType('IPet.Dog')
        expect(isCopiedFitAllowedType('IPet.Dog')).toBe(true)
    })

    it('前缀 + "." 边界 → true（allowed=IPet, type=IPet.Dog：下一位 type[4]="."）', () => {
        setCopiedType('IPet.Dog')
        expect(isCopiedFitAllowedType('IPet')).toBe(true)
    })

    it('前缀但下一位非 "." → false（off-by-one：allowed=IP, type=IPet，type[2]="t"）', () => {
        setCopiedType('IPet')
        expect(isCopiedFitAllowedType('IP')).toBe(false)
    })

    it('前缀子串但无 "." 分隔 → false（allowed=IPet, type=IPetXYZ：type[5]="X"）', () => {
        setCopiedType('IPetXYZ')
        expect(isCopiedFitAllowedType('IPet')).toBe(false)
    })

    it('trailing dot：type 恰为 "IPet." 也算命中 allowed=IPet（type[4]="."）', () => {
        setCopiedType('IPet.')
        expect(isCopiedFitAllowedType('IPet')).toBe(true)
    })

    it('无关类型 → false', () => {
        setCopiedType('Dog')
        expect(isCopiedFitAllowedType('Cat')).toBe(false)
    })

    it('空 $type：仅当 allowed 也为空串才 true', () => {
        setCopiedType('')
        expect(isCopiedFitAllowedType('')).toBe(true)
        expect(isCopiedFitAllowedType('Dog')).toBe(false)
    })
})

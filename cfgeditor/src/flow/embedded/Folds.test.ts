import {describe, it, expect} from 'vitest'
import {Folds} from './Folds.ts'

describe('Folds', () => {
    // ---------------------------------------------------------------------------
    // isFold
    // ---------------------------------------------------------------------------
    describe('isFold', () => {
        it('空 Folds 对任意链路返回 undefined', () => {
            expect(new Folds([]).isFold(['a'])).toBeUndefined()
        })

        it('返回已记录链路的折叠状态', () => {
            const f = new Folds([{chain: ['a'], fold: true}])
            expect(f.isFold(['a'])).toBe(true)
        })

        it('不同链路互不影响', () => {
            const f = new Folds([
                {chain: ['a'], fold: true},
                {chain: ['b'], fold: false},
            ])
            expect(f.isFold(['a'])).toBe(true)
            expect(f.isFold(['b'])).toBe(false)
            expect(f.isFold(['c'])).toBeUndefined()
        })

        it('链路长度不同视为不相等', () => {
            const f = new Folds([{chain: ['a'], fold: true}])
            expect(f.isFold(['a', 'b'])).toBeUndefined()
        })

        it('含数字索引的链路可正确匹配', () => {
            const f = new Folds([{chain: ['list', 0, 'field'], fold: false}])
            expect(f.isFold(['list', 0, 'field'])).toBe(false)
            expect(f.isFold(['list', 1, 'field'])).toBeUndefined()
        })
    })

    // ---------------------------------------------------------------------------
    // setFold
    // ---------------------------------------------------------------------------
    describe('setFold', () => {
        it('添加新链路的折叠状态，原 Folds 不变（不可变）', () => {
            const f = new Folds([])
            const f2 = f.setFold(['a'], true)

            expect(f.isFold(['a'])).toBeUndefined() // 原实例不变
            expect(f2.isFold(['a'])).toBe(true)
            expect(f2).not.toBe(f)
        })

        it('切换已有链路为相反值时返回新实例', () => {
            const f = new Folds([{chain: ['a'], fold: true}])
            const f2 = f.setFold(['a'], false)

            expect(f.isFold(['a'])).toBe(true) // 原实例保持
            expect(f2.isFold(['a'])).toBe(false)
            expect(f2).not.toBe(f)
        })

        it('设为与当前相同值时为无操作，返回同一实例', () => {
            const f = new Folds([{chain: ['a'], fold: true}])
            expect(f.setFold(['a'], true)).toBe(f)
        })

        it('替换时保留其它链路条目', () => {
            const f = new Folds([
                {chain: ['a'], fold: true},
                {chain: ['b'], fold: false},
            ])
            const f2 = f.setFold(['a'], false)

            expect(f2.isFold(['a'])).toBe(false)
            expect(f2.isFold(['b'])).toBe(false) // b 保留
        })

        it('可链式累积多个链路', () => {
            const f = new Folds([])
                .setFold(['a'], true)
                .setFold(['b'], false)
                .setFold(['c'], true)

            expect(f.isFold(['a'])).toBe(true)
            expect(f.isFold(['b'])).toBe(false)
            expect(f.isFold(['c'])).toBe(true)
        })
    })
})

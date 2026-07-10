import {describe, it, expect} from 'vitest'
import {FoldStateHelper} from './FoldStateHelper.ts'
import {Folds} from './Folds.ts'
import {JSONObject} from '@/api/recordModel'

// 构造带 $fold 的最小 JSONObject（类型要求 $type，运行时仅读 $fold）
function objWithFold(fold: boolean | undefined): JSONObject {
    const o: JSONObject = {$type: 'X'}
    if (fold !== undefined) o['$fold'] = fold
    return o
}

describe('FoldStateHelper', () => {
    // ---------------------------------------------------------------------------
    // getFoldState
    // ---------------------------------------------------------------------------
    describe('getFoldState', () => {
        it('优先返回本地 Folds 状态', () => {
            const folds = new Folds([{chain: ['a'], fold: true}])
            // 本地 true，对象 $fold=false → 仍取本地 true
            expect(FoldStateHelper.getFoldState(folds, ['a'], objWithFold(false))).toBe(true)
        })

        it('本地未设时回退到 obj.$fold', () => {
            const folds = new Folds([])
            expect(FoldStateHelper.getFoldState(folds, ['a'], objWithFold(true))).toBe(true)
            expect(FoldStateHelper.getFoldState(folds, ['a'], objWithFold(false))).toBe(false)
        })

        it('本地未设且 obj 无 $fold 时返回 undefined', () => {
            const folds = new Folds([])
            expect(FoldStateHelper.getFoldState(folds, ['a'], objWithFold(undefined))).toBeUndefined()
        })

        it('本地未设且未传 obj 时返回 undefined', () => {
            const folds = new Folds([])
            expect(FoldStateHelper.getFoldState(folds, ['a'])).toBeUndefined()
        })
    })

    // ---------------------------------------------------------------------------
    // shouldEmbed
    // ---------------------------------------------------------------------------
    describe('shouldEmbed', () => {
        it('状态为 false 时不内嵌', () => {
            const folds = new Folds([{chain: ['a'], fold: false}])
            const obj = objWithFold(undefined)
            expect(FoldStateHelper.shouldEmbed(folds, ['a'], obj)).toBe(false)
        })

        it('状态为 true 时内嵌', () => {
            const folds = new Folds([{chain: ['a'], fold: true}])
            const obj = objWithFold(undefined)
            expect(FoldStateHelper.shouldEmbed(folds, ['a'], obj)).toBe(true)
        })

        it('状态为 undefined（本地+对象均未设）时默认内嵌', () => {
            const folds = new Folds([])
            const obj = objWithFold(undefined)
            expect(FoldStateHelper.shouldEmbed(folds, ['a'], obj)).toBe(true)
        })

        it('本地未设但 obj.$fold=false 时不内嵌', () => {
            const folds = new Folds([])
            const obj = objWithFold(false)
            expect(FoldStateHelper.shouldEmbed(folds, ['a'], obj)).toBe(false)
        })
    })
})

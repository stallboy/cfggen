import {describe, it, expect} from 'vitest'
import {getDsLenAndDesc} from './getDsLenAndDesc.ts'
import {EntityBrief} from '@/domain/entityModel'
import {makeNodeShow} from '@/test/fixtures'

function brief(over: Partial<EntityBrief>): EntityBrief {
    return {value: 'V', ...over}
}

const ds2 = [
    {field: 'f1', value: 'v1', comment: ''},
    {field: 'f2', value: 'v2', comment: ''},
]

describe('getDsLenAndDesc', () => {
    // -----------------------------------------------------------------------
    // nodeShow=undefined：不进入 switch
    // -----------------------------------------------------------------------
    describe('nodeShow=undefined', () => {
        it('无 nodeShow 时返回 [0, null]', () => {
            expect(getDsLenAndDesc(brief({descriptions: ds2}), undefined)).toEqual([0, null])
        })
    })

    // -----------------------------------------------------------------------
    // 4 路 switch + ds 形态矩阵
    // -----------------------------------------------------------------------
    describe('refShowDescription=show', () => {
        const ns = makeNodeShow({refShowDescription: 'show'})
        it('ds 多条：showDsLen=length-1（末条当 desc），desc=末条 value', () => {
            const [len, desc] = getDsLenAndDesc(brief({descriptions: ds2}), ns)
            expect(len).toBe(1)       // 2-1
            expect(desc).toBe('v2')   // ds[末].value
        })
        it('ds 单条：showDsLen=0（唯一条降级为纯 desc），desc=该条 value', () => {
            const [len, desc] = getDsLenAndDesc(brief({descriptions: [ds2[0]]}), ns)
            expect(len).toBe(0)       // 1-1
            expect(desc).toBe('v1')
        })
        it('无 ds：showDsLen=0, desc=null', () => {
            expect(getDsLenAndDesc(brief({}), ns)).toEqual([0, null])
        })
    })

    describe('refShowDescription=showFallbackValue', () => {
        const ns = makeNodeShow({refShowDescription: 'showFallbackValue'})
        it('有 ds：与 show 相同（showDsLen=length-1, desc=末条 value）', () => {
            const [len, desc] = getDsLenAndDesc(brief({descriptions: ds2}), ns)
            expect(len).toBe(1)
            expect(desc).toBe('v2')
        })
        it('无 ds：fallback 到 brief.value，showDsLen=0', () => {
            const [len, desc] = getDsLenAndDesc(brief({value: 'FALL', descriptions: undefined}), ns)
            expect(len).toBe(0)
            expect(desc).toBe('FALL')
        })
    })

    describe('refShowDescription=showValue', () => {
        const ns = makeNodeShow({refShowDescription: 'showValue'})
        it('desc=brief.value，showDsLen=ds.length（含全部）', () => {
            const [len, desc] = getDsLenAndDesc(brief({value: 'V', descriptions: ds2}), ns)
            expect(len).toBe(2)       // ds.length 全部
            expect(desc).toBe('V')    // brief.value
        })
        it('无 ds：desc=brief.value, showDsLen=0', () => {
            const [len, desc] = getDsLenAndDesc(brief({value: 'V'}), ns)
            expect(len).toBe(0)
            expect(desc).toBe('V')
        })
    })

    describe('refShowDescription=none', () => {
        const ns = makeNodeShow({refShowDescription: 'none'})
        it('始终返回 [0, null]（有 ds 也是）', () => {
            expect(getDsLenAndDesc(brief({descriptions: ds2}), ns)).toEqual([0, null])
            expect(getDsLenAndDesc(brief({}), ns)).toEqual([0, null])
        })
    })

    // =====================================================================
    // ±1 契约：show/showFallbackValue 用 ds.length-1，showValue 用 ds.length——
    // 同名变量 showDsLen 在两路含义不同，恰差 1，最易在重构中误改。
    // 错误会同时导致 (a) 布局高度偏差 CARD_DS_H + (b) EntityCard 多/少渲染一条 desc。
    // =====================================================================
    describe('±1 契约', () => {
        it('show 的 showDsLen 比 showValue 少 1（ds.length=2）', () => {
            const showLen = getDsLenAndDesc(
                brief({value: 'V', descriptions: ds2}),
                makeNodeShow({refShowDescription: 'show'}),
            )[0]
            const showValueLen = getDsLenAndDesc(
                brief({value: 'V', descriptions: ds2}),
                makeNodeShow({refShowDescription: 'showValue'}),
            )[0]
            expect(showLen).toBe(1)
            expect(showValueLen).toBe(2)
            expect(showValueLen - showLen).toBe(1)
        })

        it('show 与 showFallbackValue 的 showDsLen 相同（都用 length-1）', () => {
            const showLen = getDsLenAndDesc(brief({descriptions: ds2}), makeNodeShow({refShowDescription: 'show'}))[0]
            const fbLen = getDsLenAndDesc(brief({descriptions: ds2}), makeNodeShow({refShowDescription: 'showFallbackValue'}))[0]
            expect(showLen).toBe(fbLen)
            expect(showLen).toBe(1)
        })
    })
})

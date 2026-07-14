import {describe, it, expect} from 'vitest'
import {calcWidthHeight, findFirstImage} from './calcWidthHeight.ts'
import {EntityEditField} from '@/domain/entityModel'
import {BriefDescription} from '@/api/recordModel'
import {ResInfo} from '@/domain/resInfo'
import {
    makeNodeShow, makeReadOnly, makeCard, makeEditable, editWith,
} from '@/test/fixtures'

// calcWidthHeight(entity, nodeShow?, notes?)：nodeShow/notes 由调用方显式传入
// （不再读 entity.sharedSetting）。

describe('calcWidthHeight', () => {
    // -----------------------------------------------------------------------
    // readonly 实体
    // -----------------------------------------------------------------------
    describe('readonly 实体', () => {
        it('高度 = 40 + 41 * 字段数，宽度默认 240', () => {
            const e = makeReadOnly({
                id: '1', label: 'x',
                fields: [
                    {key: 'a', name: 'a', value: '1'},
                    {key: 'b', name: 'b', value: '2'},
                ],
            })
            // 40 + 41*2 = 122
            expect(calcWidthHeight(e)).toEqual([240, 122])
        })

        it('nodeShow.nodeWidth 覆盖默认宽度', () => {
            const e = makeReadOnly({id: '1', label: 'x', fields: []})
            expect(calcWidthHeight(e, makeNodeShow({nodeWidth: 300}))).toEqual([300, 40])
        })
    })

    // -----------------------------------------------------------------------
    // card 实体
    // -----------------------------------------------------------------------
    describe('card 实体', () => {
        it('基础高度 = 40 + 48，无 title/desc/image', () => {
            const e = makeCard({id: '1', label: 'x', brief: {value: 'v'}})
            expect(calcWidthHeight(e)).toEqual([240, 88])
        })

        it('有 title 时 +32', () => {
            const e = makeCard({id: '1', label: 'x', brief: {title: 'T', value: 'v'}})
            expect(calcWidthHeight(e)).toEqual([240, 120])
        })

        it('有图片资源时 +200', () => {
            const e = makeCard({
                id: '1', label: 'x', brief: {value: 'v'},
                assets: [{type: 'image', name: 'p', path: 'p.png'}],
            })
            expect(calcWidthHeight(e)).toEqual([240, 288])
        })

        it('refShowDescription=show 时按 descriptions 计算高度', () => {
            const desc: BriefDescription = {field: 'b', value: 'line1\nline2', comment: ''}
            const ns = makeNodeShow({refShowDescription: 'show'})
            const e = makeCard({
                id: '1', label: 'x',
                brief: {
                    value: 'v',
                    descriptions: [{field: 'a', value: 'first', comment: ''}, desc],
                },
            })
            // getDsLenAndDesc: showDsLen = 2-1 = 1（+38），desc='line1\nline2' 行数=1（+22）
            // 40 + 48 + 38 + 22 = 148
            expect(calcWidthHeight(e, ns)).toEqual([240, 148])
        })
    })

    // -----------------------------------------------------------------------
    // editable 实体
    // -----------------------------------------------------------------------
    describe('editable 实体', () => {
        it('单个 int primitive：高度 = 40 + 20 + 40*1，宽度默认 280', () => {
            const fields: EntityEditField[] = [
                {name: 'hp', type: 'primitive', eleType: 'int', value: 0} as EntityEditField,
            ]
            const e = makeEditable({id: '1', label: 'x', edit: editWith(fields)})
            // 40 + 20 + 40 = 100
            expect(calcWidthHeight(e)).toEqual([280, 100])
        })

        it('nodeShow.editNodeWidth 覆盖默认宽度', () => {
            const e = makeEditable({id: '1', label: 'x', edit: editWith([])})
            // 40 + 20 + 40*0 = 60
            expect(calcWidthHeight(e, makeNodeShow({editNodeWidth: 400}))).toEqual([400, 60])
        })

        it('fold=true 时 +16', () => {
            const e = makeEditable({id: '1', label: 'x', edit: editWith([], {fold: true})})
            // 40 + 20 + 40*0 + 16 = 76
            expect(calcWidthHeight(e)).toEqual([280, 76])
        })

        it('text primitive 固定 4 行（rows={4}，去 autoSize 后不再按内容长度）', () => {
            const fields: EntityEditField[] = [
                {name: 'desc', type: 'primitive', eleType: 'text', value: '01234567890123456789'} as EntityEditField,
            ]
            const e = makeEditable({id: '1', label: 'x', edit: editWith(fields)})
            // text 固定 4 行 → extra = 4*22+10 = 98；cnt 不增
            // 40 + 20 + 40*0 + 98 = 158
            expect(calcWidthHeight(e)).toEqual([280, 158])
        })

        it('arrayOfPrimitive 计入 cnt(len+1) 与 extra(len*8)', () => {
            const fields: EntityEditField[] = [
                {name: 'tags', type: 'arrayOfPrimitive', eleType: 'str', value: ['a', 'b', 'c']} as EntityEditField,
            ]
            const e = makeEditable({id: '1', label: 'x', edit: editWith(fields)})
            // cnt = 3+1 = 4；extra = 3*8 = 24
            // 40 + 20 + 40*4 + 24 = 244
            expect(calcWidthHeight(e)).toEqual([280, 244])
        })
    })

    // -----------------------------------------------------------------------
    // notes（备注）高度
    // -----------------------------------------------------------------------
    describe('notes 高度', () => {
        it('label 含 _ 且 id 有 note 时按行数增加高度（行数下限 2）', () => {
            const e = makeReadOnly({id: '1', label: 'a_b', fields: []})
            // length 5 → row=5/15≈0.33 → clamp 2；40 + (2*22+22) = 40 + 66 = 106
            expect(calcWidthHeight(e, undefined, new Map([['1', 'short']]))).toEqual([240, 106])
        })

        it('note 较长时按 length/15 计行', () => {
            const e = makeReadOnly({id: '1', label: 'a_b', fields: []})
            // 30/15 = 2 行；40 + (2*22+22) = 106
            expect(calcWidthHeight(e, undefined, new Map([['1', 'x'.repeat(30)]]))).toEqual([240, 106])
        })

        it('note 超长时行数上限 10', () => {
            const e = makeReadOnly({id: '1', label: 'a_b', fields: []})
            // 300/15=20 → clamp 10；40 + (10*22+22) = 40 + 242 = 282
            expect(calcWidthHeight(e, undefined, new Map([['1', 'x'.repeat(300)]]))).toEqual([240, 282])
        })

        it('label 不含 _ 时不计 note 高度', () => {
            const e = makeReadOnly({id: '1', label: 'abc', fields: []})
            expect(calcWidthHeight(e, undefined, new Map([['1', 'x'.repeat(100)]]))).toEqual([240, 40])
        })

        it('id 无对应 note 时不计高度', () => {
            const e = makeReadOnly({id: '1', label: 'a_b', fields: []})
            expect(calcWidthHeight(e, undefined, new Map([['other', 'x'.repeat(100)]]))).toEqual([240, 40])
        })
    })
})

describe('findFirstImage', () => {
    it('返回第一个 image 资源的 path', () => {
        const assets: ResInfo[] = [
            {type: 'audio', name: 'a', path: 'a.mp3'},
            {type: 'image', name: 'b', path: 'b.png'},
            {type: 'image', name: 'c', path: 'c.jpg'},
        ]
        expect(findFirstImage(assets)).toBe('b.png')
    })

    it('无 image 时返回 undefined', () => {
        const assets: ResInfo[] = [{type: 'audio', name: 'a', path: 'a.mp3'}]
        expect(findFirstImage(assets)).toBeUndefined()
    })

    it('assets 为 undefined 时返回 undefined', () => {
        expect(findFirstImage(undefined)).toBeUndefined()
    })
})

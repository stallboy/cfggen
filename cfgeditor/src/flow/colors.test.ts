import {describe, it, expect} from 'vitest'
import {getNodeBackgroundColor, getFieldBackgroundColor, getEdgeColor} from './colors.ts'
import {EntityType, EntityEditField} from './entityModel.ts'
import {makeNodeShow, makeCard, makeReadOnly, makeEditable, withShared} from '../test/fixtures.ts'

describe('getNodeBackgroundColor', () => {
    // -----------------------------------------------------------------------
    // 按类型着色（最低优先级）
    // -----------------------------------------------------------------------
    describe('按类型着色', () => {
        it('默认实体使用 nodeColor 默认值 #0898b5', () => {
            const e = makeCard({id: '1', label: 'x', brief: {value: ''}})
            expect(getNodeBackgroundColor(e)).toBe('#0898b5')
        })

        it('Ref/Ref2/RefIn 各自的默认色', () => {
            expect(getNodeBackgroundColor(makeCard({id: '1', label: 'x', entityType: EntityType.Ref, brief: {value: ''}}))).toBe('#207b4a')
            expect(getNodeBackgroundColor(makeCard({id: '2', label: 'x', entityType: EntityType.Ref2, brief: {value: ''}}))).toBe('#006d75')
            expect(getNodeBackgroundColor(makeCard({id: '3', label: 'x', entityType: EntityType.RefIn, brief: {value: ''}}))).toBe('#003eb3')
        })

        it('nodeShow 中自定义类型色优先于默认色', () => {
            const ns = makeNodeShow({nodeColor: '#AAAAAA', nodeRefColor: '#BBBBBB'})
            const e = withShared(makeCard({id: '1', label: 'x', brief: {value: ''}}), ns)
            expect(getNodeBackgroundColor(e)).toBe('#AAAAAA')

            const ref = withShared(makeCard({id: '2', label: 'x', entityType: EntityType.Ref, brief: {value: ''}}), ns)
            expect(getNodeBackgroundColor(ref)).toBe('#BBBBBB')
        })
    })

    // -----------------------------------------------------------------------
    // 按值着色（最高优先级）
    // -----------------------------------------------------------------------
    describe('按值着色', () => {
        it('card 实体按 brief.value 命中关键字', () => {
            const ns = makeNodeShow({nodeColorsByValue: [{keyword: 'BOSS', color: '#FF0000'}]})
            const e = withShared(makeCard({id: '1', label: 'x', brief: {value: 'BOSS dragon'}}), ns)
            expect(getNodeBackgroundColor(e)).toBe('#FF0000')
        })

        it('readonly 实体按字段值拼接串命中关键字', () => {
            const ns = makeNodeShow({nodeColorsByValue: [{keyword: 'rare', color: '#GOLD'}]})
            const e = withShared(makeReadOnly({
                id: '1', label: 'x',
                fields: [
                    {key: 'a', name: 'a', value: 'normal'},
                    {key: 'b', name: 'b', value: 'rare'},
                ],
            }), ns)
            // 拼接串 "normal,rare" 命中 "rare"
            expect(getNodeBackgroundColor(e)).toBe('#GOLD')
        })

        it('editable 实体递归收集 primitive/array/interface 字段值', () => {
            const ns = makeNodeShow({nodeColorsByValue: [{keyword: 'Dog', color: '#PET'}]})
            const fields: EntityEditField[] = [
                {name: 'hp', type: 'primitive', eleType: 'int', value: 100} as EntityEditField,
                {name: 'tags', type: 'arrayOfPrimitive', eleType: 'str', value: ['a', 'b']} as EntityEditField,
                {
                    name: 'pet', type: 'interface', eleType: 'IPet', value: 'Dog',
                    autoCompleteOptions: {options: [], isValueInteger: false, isEnum: false},
                    implFields: [{name: 'lv', type: 'primitive', eleType: 'int', value: 3} as EntityEditField],
                    interfaceOnChangeImpl: () => {},
                } as EntityEditField,
            ]
            const e = withShared(makeEditable({id: '1', label: 'x', edit: {fields, hasChild: false} as never}), ns)
            // 收集到 "100,a,b,Dog,3"，命中 "Dog"
            expect(getNodeBackgroundColor(e)).toBe('#PET')
        })

        it('值关键字未命中时落到下一优先级', () => {
            const ns = makeNodeShow({nodeColorsByValue: [{keyword: 'zzz', color: '#NO'}], nodeColor: '#FALL'})
            const e = withShared(makeCard({id: '1', label: 'x', brief: {value: 'hello'}}), ns)
            expect(getNodeBackgroundColor(e)).toBe('#FALL')
        })

        it('value 为空串时不参与值着色', () => {
            const ns = makeNodeShow({nodeColorsByValue: [{keyword: '', color: '#EMPTY'}], nodeColor: '#DEF'})
            const e = withShared(makeCard({id: '1', label: 'x', brief: {value: ''}}), ns)
            // getEntityValueString 返回 ''（falsy），跳过值着色
            expect(getNodeBackgroundColor(e)).toBe('#DEF')
        })
    })

    // -----------------------------------------------------------------------
    // 按标签着色（中等优先级）
    // -----------------------------------------------------------------------
    describe('按标签着色', () => {
        it('label 包含关键字时命中', () => {
            const ns = makeNodeShow({nodeColorsByLabel: [{keyword: 'Boss', color: '#LBL'}]})
            const e = withShared(makeCard({id: '1', label: 'FinalBoss', brief: {value: ''}}), ns)
            expect(getNodeBackgroundColor(e)).toBe('#LBL')
        })

        it('值着色优先于标签着色', () => {
            const ns = makeNodeShow({
                nodeColorsByValue: [{keyword: 'v', color: '#VAL'}],
                nodeColorsByLabel: [{keyword: 'A', color: '#LBL'}],
            })
            const e = withShared(makeCard({id: '1', label: 'A', brief: {value: 'v'}}), ns)
            expect(getNodeBackgroundColor(e)).toBe('#VAL')
        })
    })
})

describe('getEdgeColor', () => {
    it('无 nodeShow 时返回默认边色 #0898b5', () => {
        expect(getEdgeColor(undefined)).toBe('#0898b5')
    })

    it('nodeShow.edgeColor 优先', () => {
        expect(getEdgeColor(makeNodeShow({edgeColor: '#EDGE'}))).toBe('#EDGE')
    })
})

describe('getFieldBackgroundColor', () => {
    it('无 nodeShow 时返回 undefined', () => {
        expect(getFieldBackgroundColor({name: 'hp'} as never, undefined)).toBeUndefined()
    })

    it('字段名精确匹配关键字时返回对应色', () => {
        const ns = makeNodeShow({fieldColorsByName: [{keyword: 'hp', color: '#HP'}]})
        expect(getFieldBackgroundColor({name: 'hp'} as never, ns)).toBe('#HP')
    })

    it('字段名不匹配时返回 undefined', () => {
        const ns = makeNodeShow({fieldColorsByName: [{keyword: 'hp', color: '#HP'}]})
        expect(getFieldBackgroundColor({name: 'mp'} as never, ns)).toBeUndefined()
    })
})

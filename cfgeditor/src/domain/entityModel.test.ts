import {describe, it, expect} from 'vitest'
import {classifyJsonValue, Entity, EntityEdgeType, isCardEntity, isEditableEntity, isReadOnlyEntity, makeChildEdge} from './entityModel'
import {makeCard, makeEditable, makeReadOnly} from '@/test/fixtures'

describe('isReadOnlyEntity / isEditableEntity / isCardEntity', () => {
    it('readonly 实体：仅 isReadOnlyEntity 为 true', () => {
        const e = makeReadOnly({id: 'r', label: 'R'})
        expect(isReadOnlyEntity(e)).toBe(true)
        expect(isEditableEntity(e)).toBe(false)
        expect(isCardEntity(e)).toBe(false)
    })

    it('editable 实体：仅 isEditableEntity 为 true', () => {
        const e = makeEditable({id: 'e', label: 'E'})
        expect(isEditableEntity(e)).toBe(true)
        expect(isReadOnlyEntity(e)).toBe(false)
        expect(isCardEntity(e)).toBe(false)
    })

    it('card 实体：仅 isCardEntity 为 true', () => {
        const e = makeCard({id: 'c', label: 'C'})
        expect(isCardEntity(e)).toBe(true)
        expect(isReadOnlyEntity(e)).toBe(false)
        expect(isEditableEntity(e)).toBe(false)
    })

    it('不变量：三个 type guard 互斥——任意实体恰有一个为 true', () => {
        const entities: Entity[] = [
            makeReadOnly({id: '1', label: 'a'}),
            makeEditable({id: '2', label: 'b'}),
            makeCard({id: '3', label: 'c'}),
        ]
        for (const e of entities) {
            const hits = [isReadOnlyEntity(e), isEditableEntity(e), isCardEntity(e)].filter(Boolean).length
            expect(hits).toBe(1)
        }
    })
})

describe('classifyJsonValue 四分类', () => {
    it('原始值（string/number/boolean）→ primitive', () => {
        expect(classifyJsonValue('x')).toBe('primitive')
        expect(classifyJsonValue(1)).toBe('primitive')
        expect(classifyJsonValue(true)).toBe('primitive')
    })

    it('null → primitive（typeof null === "object"，需额外排除）', () => {
        expect(classifyJsonValue(null as never)).toBe('primitive')
    })

    it('原始值数组（含空数组）→ primitiveList', () => {
        expect(classifyJsonValue([1, 2])).toBe('primitiveList')
        expect(classifyJsonValue(['a'])).toBe('primitiveList')
        expect(classifyJsonValue([])).toBe('primitiveList')
    })

    it('首元素为对象的数组 → objectList', () => {
        expect(classifyJsonValue([{$type: 'A'}])).toBe('objectList')
    })

    it('对象 → object', () => {
        expect(classifyJsonValue({$type: 'A'} as never)).toBe('object')
    })
})

describe('makeChildEdge', () => {
    it('生成父→子 Normal 入边（@in 单点化）', () => {
        expect(makeChildEdge('weapon', 'Hero_1-weapon')).toEqual({
            sourceHandle: 'weapon',
            target: 'Hero_1-weapon',
            targetHandle: '@in',
            type: EntityEdgeType.Normal,
        })
    })
})

import {describe, it, expect} from 'vitest'
import {Entity, isCardEntity, isEditableEntity, isReadOnlyEntity} from './entityModel'
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

import {describe, it, expect} from 'vitest'
import {mayHaveResOrNote} from './entityPredicates'

describe('mayHaveResOrNote', () => {
    it('record label（表名_记录ID，含下划线）→ true', () => {
        expect(mayHaveResOrNote('item_1001')).toBe(true)
        expect(mayHaveResOrNote('skill_5')).toBe(true)
    })

    it('表结构 label（不含下划线）→ false', () => {
        expect(mayHaveResOrNote('item')).toBe(false)
        expect(mayHaveResOrNote('SkillTable')).toBe(false)
    })

    it('空串 → false', () => {
        expect(mayHaveResOrNote('')).toBe(false)
    })
})

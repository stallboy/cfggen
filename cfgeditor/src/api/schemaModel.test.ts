import {describe, it, expect} from 'vitest'
import {isNumberType, isPrimitiveType, NUMBER_TYPES, PRIMITIVE_TYPES} from './schemaModel'

// isPrimitiveType / isNumberType / PRIMITIVE_TYPES / NUMBER_TYPES 是后端 cfggen 类型系统的
// "原始类型 / 数字类型" 分类，集中在本文件为单一权威（原散落于 embedding/entityModel/schema.tsx/EntityForm）。

describe('isPrimitiveType / isNumberType', () => {
    it('isPrimitiveType：bool/int/long/float/str/text 为 true，其余为 false', () => {
        for (const t of ['bool', 'int', 'long', 'float', 'str', 'text']) {
            expect(isPrimitiveType(t)).toBe(true)
        }
        for (const t of ['Pos', 'list<int>', 'Dog', '', 'map<str,int>']) {
            expect(isPrimitiveType(t)).toBe(false)
        }
    })

    it('isNumberType：int/long/float 为 true，bool/str/text 为 false（bool 非 number）', () => {
        for (const t of ['int', 'long', 'float']) {
            expect(isNumberType(t)).toBe(true)
        }
        for (const t of ['bool', 'str', 'text']) {
            expect(isNumberType(t)).toBe(false)
        }
    })

    it('不变量：所有 number 都是 primitive（NUMBER_TYPES ⊂ PRIMITIVE_TYPES）', () => {
        for (const t of NUMBER_TYPES) {
            expect(PRIMITIVE_TYPES.has(t)).toBe(true)
            expect(isPrimitiveType(t)).toBe(true)
        }
    })

    it('集合字面量快照（与 PrimitiveType 类型定义保持一致，防漏改一处）', () => {
        expect([...PRIMITIVE_TYPES].sort()).toEqual(['bool', 'float', 'int', 'long', 'str', 'text'])
        expect([...NUMBER_TYPES].sort()).toEqual(['float', 'int', 'long'])
    })
})

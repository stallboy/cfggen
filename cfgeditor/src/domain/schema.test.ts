import {describe, it, expect} from 'vitest'
import {
    Schema, getField, getImpl, getMapEntryTypeName, isPkInteger, getNextId, getDefaultIdInTable,
} from './schema.tsx'
import {
    field, fk, makeInterface, makeRawSchema, makeStruct, makeTable,
} from '@/test/fixtures'

// ---------------------------------------------------------------------------
// 纯函数（非类方法）
// ---------------------------------------------------------------------------

describe('getField', () => {
    it('按名称返回字段', () => {
        const t = makeTable('T', [field('id', 'int'), field('name', 'str')])
        expect(getField(t, 'name')).toStrictEqual(field('name', 'str'))
    })

    it('未找到返回 null', () => {
        expect(getField(makeStruct('S', []), 'x')).toBeNull()
    })
})

describe('getImpl', () => {
    it('按名称返回实现', () => {
        const a = makeStruct('A', [field('x', 'int')])
        const b = makeStruct('B', [field('y', 'int')])
        const iface = makeInterface('IE', [a, b])
        expect(getImpl(iface, 'B')).toBe(b)
    })

    it('未找到返回 null', () => {
        expect(getImpl(makeInterface('IE', []), 'Z')).toBeNull()
    })
})

describe('getMapEntryTypeName', () => {
    it('用 $ 前缀 + (id 或 name) + - + 字段名 构造', () => {
        const s = makeStruct('Cost', [])
        expect(getMapEntryTypeName(s, 'm')).toBe('$Cost-m')
    })

    it('优先使用 id（如 impl 的 IE.A）', () => {
        const s = makeStruct('A', [])
        s.id = 'IE.A'
        expect(getMapEntryTypeName(s, 'm')).toBe('$IE.A-m')
    })
})

describe('isPkInteger', () => {
    it('单个 int 主键 → true', () => {
        expect(isPkInteger(makeTable('T', [field('id', 'int')], {pk: ['id']}))).toBe(true)
    })

    it('单个 long 主键 → true', () => {
        expect(isPkInteger(makeTable('T', [field('id', 'long')], {pk: ['id']}))).toBe(true)
    })

    it('单个 str 主键 → false', () => {
        expect(isPkInteger(makeTable('T', [field('id', 'str')], {pk: ['id']}))).toBe(false)
    })

    it('复合主键 → false', () => {
        expect(isPkInteger(makeTable('T', [field('a', 'int'), field('b', 'int')], {pk: ['a', 'b']}))).toBe(false)
    })

    it('主键字段不存在 → false', () => {
        expect(isPkInteger(makeTable('T', [], {pk: ['id']}))).toBe(false)
    })
})

describe('getNextId', () => {
    const table = (ids: string[]) => makeTable('T', [field('id', 'int')], {
        pk: ['id'],
        recordIds: ids.map(id => ({id})),
    })

    it('返回大于 curId 的下一个空闲整数', () => {
        // 已有 {1,2,4}，curId=2 → 3
        expect(getNextId(table(['1', '2', '4']), '2')).toBe(3)
    })

    it('跳过已存在的 id', () => {
        // curId=4 → 5（5 空闲）
        expect(getNextId(table(['1', '2', '4']), '4')).toBe(5)
    })

    it('非整数主键返回 null', () => {
        const strTable = makeTable('T', [field('id', 'str')], {pk: ['id'], recordIds: [{id: 'a'}]})
        expect(getNextId(strTable, 'a')).toBeNull()
    })

    it('curId 非数字时按 0 处理', () => {
        // curId='abc' → 0；已有 {1,2,4}；++→1(有)→2(有)→3(空) → 3
        expect(getNextId(table(['1', '2', '4']), 'abc')).toBe(3)
    })

    it('无记录时返回 curId+1', () => {
        expect(getNextId(table([]), '0')).toBe(1)
    })
})

// ---------------------------------------------------------------------------
// Schema 类
// ---------------------------------------------------------------------------

describe('Schema 构造与基础查找', () => {
    it('isEditable 透传', () => {
        const s = new Schema(makeRawSchema([], {isEditable: false}))
        expect(s.isEditable).toBe(false)
    })

    it('getSTable：表返回 STable，非表/缺失返回 null', () => {
        const t = makeTable('Hero', [field('id', 'int')])
        const st = makeStruct('Vec', [field('x', 'float')])
        const s = new Schema(makeRawSchema([t, st]))
        expect(s.getSTable('Hero')).toBe(t)
        expect(s.getSTable('Vec')).toBeNull() // struct 不是 table
        expect(s.getSTable('Nope')).toBeNull()
    })

    it('itemMap 收录 struct/table/interface', () => {
        const t = makeTable('Hero', [])
        const st = makeStruct('Vec', [])
        const ie = makeInterface('IE', [])
        const s = new Schema(makeRawSchema([t, st, ie]))
        expect(s.itemMap.has('Hero')).toBe(true)
        expect(s.itemMap.has('Vec')).toBe(true)
        expect(s.itemMap.has('IE')).toBe(true)
    })
})

describe('Schema impl 推理', () => {
    it('为每个 impl 设置 id（接口名.实现名）与 extends', () => {
        const a = makeStruct('A', [field('x', 'int')])
        const ie = makeInterface('IE', [a])
        const s = new Schema(makeRawSchema([ie]))
        // 构造副作用：写入 impl.id / impl.extends，并登记到 itemIncludeImplMap
        expect(s.itemIncludeImplMap.get('IE.A')).toBe(a)
        expect(a.id).toBe('IE.A')
        expect(a.extends).toBe(ie)
    })

    it('itemIncludeImplMap 同时收录接口名与各 impl.id', () => {
        const a = makeStruct('A', [])
        const b = makeStruct('B', [])
        const ie = makeInterface('IE', [a, b])
        const s = new Schema(makeRawSchema([ie]))
        expect(s.itemIncludeImplMap.has('IE')).toBe(true)
        expect(s.itemIncludeImplMap.has('IE.A')).toBe(true)
        expect(s.itemIncludeImplMap.has('IE.B')).toBe(true)
        expect(s.itemIncludeImplMap.get('IE.A')).toBe(a)
    })
})

describe('Schema map entry 类型构造', () => {
    it('struct 的 map<K,V> 字段生成 $name-field 结构体（含 key/value 字段）', () => {
        const st = makeStruct('Cost', [field('m', 'map<str,int>')])
        const s = new Schema(makeRawSchema([st]))
        const entry = s.itemIncludeImplMap.get('$Cost-m')
        expect(entry).toBeDefined()
        expect(entry!.type).toBe('struct')
        expect((entry as {fields: {name: string; type: string}[]}).fields).toEqual([
            {name: 'key', type: 'str', comment: ''},
            {name: 'value', type: 'int', comment: ''},
        ])
    })
})

describe('Schema idMap / hasId', () => {
    it('table.idMap 映射 recordId.id → recordId', () => {
        const t = makeTable('Hero', [field('id', 'int')], {
            recordIds: [{id: '1', title: 'Knight'}, {id: '2'}],
        })
        new Schema(makeRawSchema([t]))
        expect(t.idMap!.get('1')).toStrictEqual({id: '1', title: 'Knight'})
        expect(t.idMap!.get('2')).toStrictEqual({id: '2'})
    })

    it('hasId 判断 id 是否存在', () => {
        const t = makeTable('Hero', [field('id', 'int')], {recordIds: [{id: '1'}]})
        const schema = new Schema(makeRawSchema([t]))
        expect(schema.hasId(t, '1')).toBe(true)
        expect(schema.hasId(t, '9')).toBe(false)
    })
})

describe('Schema refInTables（反向外键）', () => {
    it('被引用的表 refInTables 包含引用方表名', () => {
        const weapon = makeTable('Weapon', [field('id', 'int')], {recordIds: [{id: '10'}]})
        const hero = makeTable('Hero', [field('id', 'int'), field('weaponId', 'int')], {
            foreignKeys: [fk('fk_weapon', ['weaponId'], 'Weapon')],
        })
        new Schema(makeRawSchema([weapon, hero]))
        expect(weapon.refInTables!.has('Hero')).toBe(true)
    })
})

// ---------------------------------------------------------------------------
// 依赖与引用表推导
// ---------------------------------------------------------------------------

describe('getDirectDepStructsByItem', () => {
    it('收集 struct 的非原始类型依赖（含 list<>/map<> 内部类型）', () => {
        const other = makeStruct('Other', [field('v', 'int')])
        const s = makeStruct('S', [
            field('a', 'int'),          // 原始，忽略
            field('ref', 'Other'),      // struct 引用
            field('lst', 'list<Other>'),// list 元素
            field('mp', 'map<str,Other>'), // map value（key 原始忽略）
        ])
        const schema = new Schema(makeRawSchema([other, s]))
        expect(schema.getDirectDepStructsByItem(s)).toEqual(new Set(['Other']))
    })

    it('仅原始字段的 struct 无依赖', () => {
        const s = makeStruct('S', [field('a', 'int'), field('b', 'str')])
        const schema = new Schema(makeRawSchema([s]))
        expect(schema.getDirectDepStructsByItem(s)).toEqual(new Set())
    })

    it('引用了不存在类型的字段被过滤', () => {
        const s = makeStruct('S', [field('g', 'Ghost')])
        const schema = new Schema(makeRawSchema([s]))
        expect(schema.getDirectDepStructsByItem(s)).toEqual(new Set())
    })

    it('interface 的直接依赖是各 impl.id', () => {
        const a = makeStruct('A', [])
        const b = makeStruct('B', [])
        const ie = makeInterface('IE', [a, b])
        const schema = new Schema(makeRawSchema([ie]))
        expect(schema.getDirectDepStructsByItem(ie)).toEqual(new Set(['IE.A', 'IE.B']))
    })
})

describe('getAllDepStructs', () => {
    it('传递闭包：S→Other→Other2 全部纳入', () => {
        const other2 = makeStruct('Other2', [field('v', 'int')])
        const other = makeStruct('Other', [field('o2', 'Other2')])
        const s = makeStruct('S', [field('o', 'Other')])
        const schema = new Schema(makeRawSchema([other2, other, s]))
        expect(schema.getAllDepStructs(s)).toEqual(new Set(['S', 'Other', 'Other2']))
    })

    it('环引用不导致死循环', () => {
        const a = makeStruct('A', [field('b', 'B')])
        const b = makeStruct('B', [field('a', 'A')])
        const schema = new Schema(makeRawSchema([a, b]))
        expect(schema.getAllDepStructs(a)).toEqual(new Set(['A', 'B']))
    })
})

describe('getAllRefTablesByItem', () => {
    it('汇总自身及依赖链上的外键引用表', () => {
        const weapon = makeTable('Weapon', [field('id', 'int')], {recordIds: [{id: '1'}]})
        const cost = makeStruct('Cost', [field('w', 'int')], {
            foreignKeys: [fk('fk_w', ['w'], 'Weapon')],
        })
        const hero = makeTable('Hero', [field('id', 'int'), field('c', 'Cost')], {})
        const schema = new Schema(makeRawSchema([weapon, cost, hero]))
        // Hero → Cost →(FK) Weapon
        expect(schema.getAllRefTablesByItem(hero)).toEqual(new Set(['Weapon']))
    })

    it('interface 的 enumRef 被收集', () => {
        const enumTbl = makeTable('EnumTbl', [field('id', 'int')], {recordIds: [{id: '1'}]})
        const ie = makeInterface('IE', [makeStruct('A', [])], {enumRef: 'EnumTbl'})
        const schema = new Schema(makeRawSchema([enumTbl, ie]))
        expect(schema.getAllRefTablesByItem(ie)).toEqual(new Set(['EnumTbl']))
    })
})

// ---------------------------------------------------------------------------
// 默认值
// ---------------------------------------------------------------------------

describe('defaultValue', () => {
    it('struct 默认值：各原始类型 + list/map 空数组 + 嵌套结构递归', () => {
        const inner = makeStruct('Inner', [field('n', 'int')])
        const outer = makeStruct('Outer', [
            field('b', 'bool'),
            field('i', 'int'),
            field('f', 'float'),
            field('s', 'str'),
            field('t', 'text'),
            field('l', 'list<int>'),
            field('m', 'map<str,int>'),
            field('inner', 'Inner'),
        ])
        const schema = new Schema(makeRawSchema([inner, outer]))
        expect(schema.defaultValue(outer)).toEqual({
            $type: 'Outer',
            b: false, i: 0, f: 0, s: '', t: '', l: [], m: [],
            inner: {$type: 'Inner', n: 0},
        })
    })

    it('interface 默认值取 defaultImpl', () => {
        const a = makeStruct('A', [field('x', 'int')])
        const b = makeStruct('B', [field('y', 'str')])
        const ie = makeInterface('IE', [a, b], {defaultImpl: 'B'})
        const schema = new Schema(makeRawSchema([ie]))
        expect(schema.defaultValue(ie)).toEqual({$type: 'IE.B', y: ''})
    })

    it('interface 无 defaultImpl 时取第一个 impl', () => {
        const a = makeStruct('A', [field('x', 'int')])
        const ie = makeInterface('IE', [a])
        const schema = new Schema(makeRawSchema([ie]))
        expect(schema.defaultValue(ie)).toEqual({$type: 'IE.A', x: 0})
    })
})

// ---------------------------------------------------------------------------
// 句柄 / 查找辅助
// ---------------------------------------------------------------------------

describe('getFkTargetHandle', () => {
    it('fk.refKeys 存在时返回 @in_<第一个 refKey>', () => {
        const schema = new Schema(makeRawSchema([]))
        expect(schema.getFkTargetHandle(fk('f', ['k'], 'T', {refKeys: ['rk']}))).toBe('@in_rk')
    })

    it('无 refKeys 时取目标表 pk[0]', () => {
        const t = makeTable('T', [field('tid', 'int')], {pk: ['tid']})
        const schema = new Schema(makeRawSchema([t]))
        expect(schema.getFkTargetHandle(fk('f', ['k'], 'T'))).toBe('@in_tid')
    })

    it('目标表不存在时返回 @in', () => {
        const schema = new Schema(makeRawSchema([]))
        expect(schema.getFkTargetHandle(fk('f', ['k'], 'Ghost'))).toBe('@in')
    })
})

describe('getSTableByLastName', () => {
    it('按命名空间后的末尾名匹配表', () => {
        const t = makeTable('game.hero.Hero', [field('id', 'int')])
        const schema = new Schema(makeRawSchema([t]))
        expect(schema.getSTableByLastName('Hero')).toBe(t)
    })

    it('无命名空间的表名直接匹配', () => {
        const t = makeTable('Hero', [field('id', 'int')])
        const schema = new Schema(makeRawSchema([t]))
        expect(schema.getSTableByLastName('Hero')).toBe(t)
    })

    it('未匹配返回 undefined', () => {
        const schema = new Schema(makeRawSchema([]))
        expect(schema.getSTableByLastName('Nope')).toBeUndefined()
    })
})

describe('getIdTitle', () => {
    it('返回 (table, id) 对应记录的 title', () => {
        const t = makeTable('Hero', [field('id', 'int')], {
            recordIds: [{id: '1', title: 'Knight'}],
        })
        const schema = new Schema(makeRawSchema([t]))
        expect(schema.getIdTitle('Hero', '1')).toBe('Knight')
    })

    it('记录无 title 时返回 undefined', () => {
        const t = makeTable('Hero', [field('id', 'int')], {recordIds: [{id: '2'}]})
        const schema = new Schema(makeRawSchema([t]))
        expect(schema.getIdTitle('Hero', '2')).toBeUndefined()
    })

    it('id 不存在返回 undefined', () => {
        const t = makeTable('Hero', [field('id', 'int')], {recordIds: [{id: '1'}]})
        const schema = new Schema(makeRawSchema([t]))
        expect(schema.getIdTitle('Hero', '99')).toBeUndefined()
    })
})

describe('getDefaultIdInTable', () => {
    it('表有记录时返回第一条记录 id', () => {
        const t = makeTable('Hero', [field('id', 'int')], {recordIds: [{id: '1'}, {id: '2'}]})
        const schema = new Schema(makeRawSchema([t]))
        expect(getDefaultIdInTable(schema, 'Hero', '0')).toBe('1')
    })

    it('表无记录时返回 curId', () => {
        const t = makeTable('Hero', [field('id', 'int')], {recordIds: []})
        const schema = new Schema(makeRawSchema([t]))
        expect(getDefaultIdInTable(schema, 'Hero', 'fallback')).toBe('fallback')
    })

    it('表不存在时返回 curId', () => {
        const schema = new Schema(makeRawSchema([]))
        expect(getDefaultIdInTable(schema, 'Ghost', 'fallback')).toBe('fallback')
    })
})

import {describe, it, expect} from 'vitest'
import {RecordEditEntityCreator} from './recordEditEntityCreator.ts'
import {EntityEditField} from '../../flow/entityModel.ts'
import {Schema} from '../../domain/schema.tsx'
import {Folds} from '../../flow/embedded/Folds.ts'
import {field, fk, makeInterface, makeRawSchema, makeStruct, makeTable} from '../../test/fixtures.ts'

function buildSchema() {
    const small = makeStruct('Small', [field('dmg', 'int')])              // 可内嵌：1 个 primitive
    const big = makeStruct('Big', [field('name', 'str'), field('desc', 'str'), field('dmg', 'int')]) // 不可内嵌
    const wrap = makeStruct('Wrap', [
        field('sm', 'list<Small>'), field('bg', 'list<Big>'),
        field('sf', 'Small'), field('bf', 'Big'), field('lp', 'list<int>'),
    ])
    const flags = makeStruct('Flags', [field('a', 'bool'), field('b', 'int'), field('c', 'str')])
    const a = makeStruct('A', [field('x', 'int')])
    const ie = makeInterface('IE', [a])
    const weapon = makeTable('Weapon', [field('id', 'int')], {
        recordIds: [{id: '1', title: 'Sword'}, {id: '2'}],
    })
    const hero = makeTable('Hero', [
        field('name', 'str'), field('hp', 'int'), field('alive', 'bool'), field('weaponId', 'int'),
    ], {foreignKeys: [fk('fk_w', ['weaponId'], 'Weapon', {refType: 'rPrimary'})]})
    return {
        schema: new Schema(makeRawSchema([small, big, wrap, flags, ie, weapon, hero])),
        hero, weapon,
    }
}

function newCreator(schema: Schema, curTable: ReturnType<typeof makeTable>) {
    return new RecordEditEntityCreator(new Map(), schema, curTable, '1', new Folds([]), () => {})
}

describe('RecordEditEntityCreator.makeEditFields 原始字段', () => {
    it('struct 生成 primitive 字段，保留 eleType 与值', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('Big')!,
            {$type: 'Big', name: 'n', desc: 'd', dmg: 5} as never,
            [],
        )
        const summary = (fields as EntityEditField[]).map(f => [f.name, f.type, (f as {eleType: string}).eleType, (f as {value: unknown}).value])
        expect(summary).toEqual([
            ['name', 'primitive', 'str', 'n'],
            ['desc', 'primitive', 'str', 'd'],
            ['dmg', 'primitive', 'int', 5],
        ])
    })

    it('缺失/ falsy 值时取类型默认值（bool→false, int→0, str→""）', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('Flags')!,
            {$type: 'Flags'} as never,
            [],
        ) as EntityEditField[]
        const byName = Object.fromEntries(fields.map(f => [f.name, (f as {value: unknown}).value]))
        expect(byName).toEqual({a: false, b: 0, c: ''})
    })
})

describe('RecordEditEntityCreator.makeEditFields interface / table', () => {
    it('interface 生成 $impl 字段，implFields 取自对应实现', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('IE')! as never,
            {$type: 'IE.A', x: 1} as never,
            [],
        ) as EntityEditField[]
        expect(fields).toHaveLength(1)
        const impl = fields[0] as {name: string; type: string; value: string; implFields: EntityEditField[]}
        expect(impl.name).toBe('$impl')
        expect(impl.type).toBe('interface')
        expect(impl.value).toBe('A')
        expect(impl.implFields).toHaveLength(1)
        expect((impl.implFields[0] as {name: string; value: number}).value).toBe(1)
    })

    it('interface 未知 impl 时 implFields 为空（仍创建 $impl 字段）', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('IE')! as never,
            {$type: 'IE.Ghost'} as never,
            [],
        ) as EntityEditField[]
        expect((fields[0] as {value: string}).value).toBe('Ghost')
        expect((fields[0] as {implFields: unknown[]}).implFields).toEqual([])
    })

    it('table 在末尾追加 $submit(funcSubmit) 字段', () => {
        const {schema, hero} = buildSchema()
        const c = newCreator(schema, hero)
        const fields = c.makeEditFields(hero, {$type: 'Hero', name: 'h', hp: 1, alive: true, weaponId: 1} as never, []) as EntityEditField[]
        const submit = fields[fields.length - 1] as {name: string; type: string}
        expect(submit.name).toBe('$submit')
        expect(submit.type).toBe('funcSubmit')
    })
})

describe('RecordEditEntityCreator.makeEditFields list/struct 引用与内嵌判定', () => {
    it('单元素可内嵌 list → structRef(embedded)；不可内嵌 list → funcAdd；structRef 可内嵌/回退；list<primitive> → arrayOfPrimitive', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('Wrap')!,
            {
                $type: 'Wrap',
                sm: [{$type: 'Small', dmg: 3}],
                bg: [{$type: 'Big', name: 'n', desc: 'd', dmg: 1}],
                sf: {$type: 'Small', dmg: 1},
                bf: {$type: 'Big', name: 'n', desc: 'd', dmg: 1},
                lp: [1, 2],
            } as never,
            [],
        ) as EntityEditField[]

        const byName = Object.fromEntries(fields.map(f => [f.name, f])) as Record<string, EntityEditField & {embeddedField?: unknown; value?: unknown}>

        // sm：单元素可内嵌 list → structRef + embeddedField
        expect(byName.sm.type).toBe('structRef')
        expect(byName.sm.embeddedField).toBeDefined()

        // bg：不可内嵌 list → funcAdd
        expect(byName.bg.type).toBe('funcAdd')

        // sf：可内嵌 struct → structRef + embeddedField
        expect(byName.sf.type).toBe('structRef')
        expect(byName.sf.embeddedField).toBeDefined()

        // bf：不可内嵌 struct → structRef，value '[]'，无 embeddedField
        expect(byName.bf.type).toBe('structRef')
        expect(byName.bf.value).toBe('[]')
        expect(byName.bf.embeddedField).toBeUndefined()

        // lp：list<primitive> → arrayOfPrimitive
        expect(byName.lp.type).toBe('arrayOfPrimitive')
        expect(byName.lp.value).toEqual([1, 2])
    })

    it('内嵌字段提取出 struct 的 primitive 值', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('Wrap')!,
            {$type: 'Wrap', sf: {$type: 'Small', dmg: 7}} as never,
            [],
        ) as (EntityEditField & {embeddedField?: {fields: {value: unknown; name: string}[]}})[]
        const sf = fields.find(f => f.name === 'sf')!
        expect(sf.embeddedField!.fields).toEqual([{value: 7, type: 'int', name: 'dmg', comment: ''}])
    })
})

describe('RecordEditEntityCreator.getAutoCompleteOptions', () => {
    it('外键字段返回目标表的 id 选项', () => {
        const {schema, hero} = buildSchema()
        const c = newCreator(schema, hero)
        const opt = c.getAutoCompleteOptions(hero, 'weaponId')
        expect(opt).toBeDefined()
        expect(opt!.isValueInteger).toBe(true)   // Weapon pk 为 int
        expect(opt!.isEnum).toBe(false)           // entryType=eNo
        expect(opt!.options).toHaveLength(2)
        expect(opt!.options[0].labelstr).toBe('1 Sword')
        expect(opt!.options[0].value).toBe('1')
    })

    it('非外键字段返回 undefined', () => {
        const {schema, hero} = buildSchema()
        const c = newCreator(schema, hero)
        expect(c.getAutoCompleteOptions(hero, 'name')).toBeUndefined()
    })

    it('无 foreignKeys 的 struct 返回 undefined', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const big = schema.itemIncludeImplMap.get('Big')! as never
        expect(c.getAutoCompleteOptions(big, 'name')).toBeUndefined()
    })
})

import {describe, it, expect} from 'vitest'
import {RecordEditEntityCreator} from './recordEditEntityCreator.ts'
import {EntityEditField} from '@/domain/entityModel.ts'
import {Schema} from '@/domain/schema.ts'
import {field, fk, makeInterface, makeRawSchema, makeStruct, makeTable} from '@/test/fixtures.ts'
import {EditingSession} from '@/services/editingSession.ts'
import {JSONObject} from '@/api/recordModel.ts'

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
    // 这些用例只测 makeEditFields / getAutoCompleteOptions 的字段生成逻辑，不触发编辑回调，
    // 故 session 传 mock、editingObject 传占位即可。
    return new RecordEditEntityCreator(new Map(), schema, curTable, '1',
        {} as unknown as EditingSession, {$type: 'Placeholder'} as JSONObject)
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

        // bf：不可内嵌 struct → structRef，无 embeddedField
        expect(byName.bf.type).toBe('structRef')
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

describe('RecordEditEntityCreator list 嵌入（$embed_<fieldName>，类 2）', () => {
    const big = () => makeStruct('Big', [field('name', 'str'), field('desc', 'str'), field('dmg', 'int')])
    const bigItem = (name: string, dmg: number) => ({$type: 'Big', name, desc: 'd', dmg})

    /** 建含 Big + WrapT(bg: list<Big>) 的 schema，跑 createThis 返回 entityMap。 */
    function createEntities(obj: JSONObject) {
        const wrapT = makeTable('WrapT', [field('bg', 'list<Big>')])
        const s2 = new Schema(makeRawSchema([big(), wrapT]))
        const entityMap = new Map()
        new RecordEditEntityCreator(entityMap, s2, wrapT, '1',
            {} as unknown as EditingSession, obj).createThis()
        return entityMap
    }

    it('funcAdd 字段带 listEmbed：未嵌入 embedded=false + itemCount', () => {
        const onlyList = makeStruct('OnlyList', [field('bg', 'list<Big>')])
        const s2 = new Schema(makeRawSchema([big(), onlyList]))
        const c = newCreator(s2, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            onlyList,
            {$type: 'OnlyList', bg: [bigItem('n', 1)]} as never,
            [],
        ) as EntityEditField[]
        const bg = fields.find(f => f.name === 'bg')!
        expect(bg.type).toBe('funcAdd')
        const le = (bg as {listEmbed?: {embedded: boolean; itemCount: number}}).listEmbed!
        expect(le.embedded).toBe(false)
        expect(le.itemCount).toBe(1)
    })

    it('嵌入的多元素 list（$embed_bg=true）→ funcAdd 摘要行', () => {
        const onlyList = makeStruct('OnlyList', [field('bg', 'list<Big>')])
        const s2 = new Schema(makeRawSchema([big(), onlyList]))
        const c = newCreator(s2, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            onlyList,
            {$type: 'OnlyList', bg: [bigItem('a', 1), bigItem('b', 2)], '$embed_bg': true} as never,
            [],
        ) as EntityEditField[]
        const bg = fields.find(f => f.name === 'bg')! as EntityEditField & {
            listEmbed?: {embedded: boolean; itemCount: number}
        }
        expect(bg.type).toBe('funcAdd')
        expect(bg.listEmbed!.embedded).toBe(true)
        expect(bg.listEmbed!.itemCount).toBe(2)
    })

    it('createEntity：嵌入的 list 不建子 entity（hasChild 仍为 true）', () => {
        const entityMap = createEntities({
            $type: 'WrapT', bg: [bigItem('a', 1), bigItem('b', 2)], '$embed_bg': true,
        } as never)
        expect(entityMap.size).toBe(1)   // 只有根节点，子 element 不建
        const root = [...entityMap.values()][0] as {edit: {hasChild: boolean}}
        expect(root.edit.hasChild).toBe(true)
    })

    it('createEntity：未嵌入的 list 正常建子 entity', () => {
        const entityMap = createEntities({
            $type: 'WrapT', bg: [bigItem('a', 1), bigItem('b', 2)],
        } as never)
        expect(entityMap.size).toBe(3)   // 根 + 2 个元素节点
    })
})

describe('RecordEditEntityCreator 单元素 list 嵌入（类 1）', () => {
    const small = () => makeStruct('Small', [field('dmg', 'int')])
    const onlySm = () => makeStruct('OnlySm', [field('sm', 'list<Small>')])

    function makeSmFields(obj: JSONObject) {
        const s2 = new Schema(makeRawSchema([small(), onlySm()]))
        const c = newCreator(s2, makeTable('Placeholder', []))
        return c.makeEditFields(onlySm(), obj, []) as (EntityEditField & {
            embeddedField?: unknown; expandEmbedded?: unknown;
            listEmbed?: {embedded: boolean; itemCount: number}
        })[]
    }

    it('默认（无键）→ 内嵌 Tag', () => {
        const fields = makeSmFields({$type: 'OnlySm', sm: [{$type: 'Small', dmg: 3}]} as never)
        const sm = fields.find(f => f.name === 'sm')!
        expect(sm.type).toBe('structRef')
        expect(sm.embeddedField).toBeDefined()
        expect(sm.expandEmbedded).toBeDefined()   // 内嵌 Tag 行挂展开入口
    })

    it('$embed_sm=false → 显式展开：funcAdd 非嵌入态', () => {
        const fields = makeSmFields({$type: 'OnlySm', sm: [{$type: 'Small', dmg: 3}], '$embed_sm': false} as never)
        const sm = fields.find(f => f.name === 'sm')!
        expect(sm.type).toBe('funcAdd')
        expect(sm.embeddedField).toBeUndefined()
        expect(sm.listEmbed!.embedded).toBe(false)
    })

    it('$embed_sm=true → 类 1 语义视同默认（收起）→ 内嵌 Tag（true 键本是类 1 残留，读侧按语义消化）', () => {
        const fields = makeSmFields({$type: 'OnlySm', sm: [{$type: 'Small', dmg: 3}], '$embed_sm': true} as never)
        const sm = fields.find(f => f.name === 'sm')!
        expect(sm.type).toBe('structRef')
        expect(sm.embeddedField).toBeDefined()
    })

    it('单元素展开态点嵌入按钮 ⇒ 类 1 收起=删键（不写 true 残留，守不变式）', () => {
        const session = new EditingSession({
            resultCode: 'ok', table: 't', id: '1', maxObjs: 0,
            object: {$type: 'OnlySm', sm: [{$type: 'Small', dmg: 3}], '$embed_sm': false} as never,
            refs: [],
        })
        const s2 = new Schema(makeRawSchema([small(), onlySm()]))
        const c = new RecordEditEntityCreator(new Map(), s2, makeTable('Placeholder', []), '1',
            session, {$type: 'Placeholder'} as JSONObject)
        const fields = c.makeEditFields(onlySm(),
            {$type: 'OnlySm', sm: [{$type: 'Small', dmg: 3}], '$embed_sm': false} as never, []) as (EntityEditField & {
            listEmbed?: {embedded: boolean; onUpdateListEmbed: (embed: boolean, position: never) => void}
        })[]
        const sm = fields.find(f => f.name === 'sm')!
        expect(sm.type).toBe('funcAdd')   // 展开态
        sm.listEmbed!.onUpdateListEmbed(true, {id: 'n', x: 0, y: 0} as never)
        expect('$embed_sm' in session.getEditingObject()).toBe(false)   // 删键回默认内嵌，而非写 true
    })

    it('多元素展开态点嵌入按钮 ⇒ 类 2 写 $embed=true', () => {
        const session = new EditingSession({
            resultCode: 'ok', table: 't', id: '1', maxObjs: 0,
            object: {$type: 'OnlySm', sm: [{$type: 'Small', dmg: 3}, {$type: 'Small', dmg: 4}]} as never,
            refs: [],
        })
        const s2 = new Schema(makeRawSchema([small(), onlySm()]))
        const c = new RecordEditEntityCreator(new Map(), s2, makeTable('Placeholder', []), '1',
            session, {$type: 'Placeholder'} as JSONObject)
        const fields = c.makeEditFields(onlySm(),
            {$type: 'OnlySm', sm: [{$type: 'Small', dmg: 3}, {$type: 'Small', dmg: 4}]} as never, []) as (EntityEditField & {
            listEmbed?: {embedded: boolean; onUpdateListEmbed: (embed: boolean, position: never) => void}
        })[]
        const sm = fields.find(f => f.name === 'sm')!
        sm.listEmbed!.onUpdateListEmbed(true, {id: 'n', x: 0, y: 0} as never)
        expect(session.getEditingObject()['$embed_sm']).toBe(true)
    })
})

describe('RecordEditEntityCreator 展开/回嵌入口（$embed_<fieldName>，类 1）', () => {
    it('可内嵌且展开的 struct 字段（$embed_sf=false）→ structRef 占位行带 reEmbed；不可内嵌 → 无', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('Wrap')!,
            {
                $type: 'Wrap',
                sf: {$type: 'Small', dmg: 1},
                bf: {$type: 'Big', name: 'n', desc: 'd', dmg: 1},
                '$embed_sf': false,
            } as never,
            [],
        ) as (EntityEditField & {reEmbed?: unknown; embeddedField?: unknown; expandEmbedded?: unknown})[]

        const sf = fields.find(f => f.name === 'sf')!
        expect(sf.type).toBe('structRef')
        expect(sf.embeddedField).toBeUndefined()   // 展开态，非内嵌
        expect(sf.reEmbed).toBeDefined()           // 占位行挂回嵌入口
        expect(sf.expandEmbedded).toBeUndefined()

        const bf = fields.find(f => f.name === 'bf')!
        expect(bf.type).toBe('structRef')
        expect(bf.reEmbed).toBeUndefined()         // 不可内嵌 → 无回嵌入口
    })

    it('内嵌态（无 $embed 键）→ embeddedField + expandEmbedded，无 reEmbed', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('Wrap')!,
            {$type: 'Wrap', sf: {$type: 'Small', dmg: 1}} as never,
            [],
        ) as (EntityEditField & {reEmbed?: unknown; embeddedField?: unknown; expandEmbedded?: unknown})[]
        const sf = fields.find(f => f.name === 'sf')!
        expect(sf.embeddedField).toBeDefined()
        expect(sf.expandEmbedded).toBeDefined()    // 内嵌 Tag 行挂展开入口
        expect(sf.reEmbed).toBeUndefined()
    })

    it('旧格式迁移：子对象上的 $fold=false 失效（视为节点级 fold 的 inert 残留）→ 默认内嵌', () => {
        const {schema} = buildSchema()
        const c = newCreator(schema, makeTable('Placeholder', []))
        const fields = c.makeEditFields(
            schema.itemIncludeImplMap.get('Wrap')!,
            {$type: 'Wrap', sf: {$type: 'Small', dmg: 1, '$fold': false}} as never,
            [],
        ) as (EntityEditField & {embeddedField?: unknown; reEmbed?: unknown})[]
        const sf = fields.find(f => f.name === 'sf')!
        expect(sf.embeddedField).toBeDefined()   // 旧 $fold=false 不再表展开，接受视觉状态回嵌
        expect(sf.reEmbed).toBeUndefined()
    })
})

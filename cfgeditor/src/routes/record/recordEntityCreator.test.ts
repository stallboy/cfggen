import {describe, it, expect} from 'vitest'
import {RecordEntityCreator} from './recordEntityCreator.ts'
import {EntityEdgeType, EntityType} from '../../flow/entityModel.ts'
import {RefId} from '../../api/recordModel.ts'
import {Schema} from '../../domain/schema.tsx'
import {field, makeRawSchema, makeStruct} from '../../test/fixtures.ts'

function buildSchema() {
    const hero = makeStruct('Hero', [
        field('name', 'str'), field('hp', 'int'), field('alive', 'bool'),
        field('weapon', 'Weapon'), field('tags', 'list<str>'), field('skills', 'list<Skill>'),
    ])
    const weapon = makeStruct('Weapon', [field('dmg', 'int')])
    const skill = makeStruct('Skill', [field('name', 'str')])
    return new Schema(makeRawSchema([hero, weapon, skill]))
}

// 资源解析短路：assetRefTable 为空 → findAllResInfos 返回 undefined
const noRes = {
    tauriConf: {assetDir: '', assetRefTable: '', resDirs: []},
    resourceDir: '', resMap: new Map(),
}

function newCreator(schema: Schema, refId: RefId = {table: 'Hero', id: '1'}, refs: never[] = []) {
    const entityMap = new Map()
    return {
        entityMap,
        creator: new RecordEntityCreator(entityMap, schema, refId, refs as never, noRes.tauriConf, noRes.resourceDir, noRes.resMap),
    }
}

describe('RecordEntityCreator.createRecordEntity 字段值格式化', () => {
    it('原始字段：str/int 原样 toString，bool 转 ✔️/✘', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        const obj = {$type: 'Hero', name: 'Knight', hp: 100, alive: true}
        const e = creator.createRecordEntity('Hero_1', obj as never)!

        const values = (e.fields as {name: string; value: string}[]).map(f => [f.name, f.value])
        expect(values).toEqual([['name', 'Knight'], ['hp', '100'], ['alive', '✔️']])
    })

    it('bool false 显示为 ✘', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero_1', {$type: 'Hero', alive: false} as never)!
        expect((e.fields as {value: string}[])[0].value).toBe('✘')
    })

    it('字段注释：schema 非空 comment 直接使用；空 comment 保留为空串；字段不在 schema 中才回落字段名', () => {
        // Hero2.name 带 comment '名字'；obj 还含一个不在 schema 中的 extra 字段
        const hero2 = makeStruct('Hero2', [field('name', 'str', '名字')])
        const schema = new Schema(makeRawSchema([hero2]))
        const {creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero2_1', {$type: 'Hero2', name: 'x', extra: 1} as never)!
        const byName = Object.fromEntries(
            (e.fields as {name: string; comment: string}[]).map(f => [f.name, f.comment]),
        )
        // sField.comment='名字' → 用之
        expect(byName.name).toBe('名字')
        // extra 不在 schema → sField=null → ?? 回落到字段名
        expect(byName.extra).toBe('extra')
    })

    it('字段在 schema 中且 comment 为空串时保留空串（?? 不对空串生效）', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero_1', {$type: 'Hero', name: 'x'} as never)!
        // field() 默认 comment='' → 结果仍为 ''
        expect((e.fields as {comment: string}[])[0].comment).toBe('')
    })

    it('原始类型数组用逗号拼接', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero_1', {$type: 'Hero', tags: ['a', 'b', 'c']} as never)!
        expect((e.fields as {name: string; value: string}[]).find(f => f.name === 'tags')!.value).toBe('a,b,c')
    })

    it('空数组显示为 []', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero_1', {$type: 'Hero', tags: []} as never)!
        expect((e.fields as {name: string; value: string}[]).find(f => f.name === 'tags')!.value).toBe('[]')
    })
})

describe('RecordEntityCreator.createRecordEntity 嵌套与边', () => {
    it('struct 字段生成子实体与 Normal 边，值显示为 <>', () => {
        const schema = buildSchema()
        const {entityMap, creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero_1', {
            $type: 'Hero', weapon: {$type: 'Weapon', dmg: 5},
        } as never)!

        // 子实体已登记
        const weapon = entityMap.get('Hero_1-weapon')
        expect(weapon).toBeDefined()
        expect((weapon.fields as {name: string; value: string}[]).find(f => f.name === 'dmg')!.value).toBe('5')
        // 父字段值 '<>'
        expect((e.fields as {name: string; value: string}[]).find(f => f.name === 'weapon')!.value).toBe('<>')
        // Normal 边
        expect(e.sourceEdges).toContainEqual({
            sourceHandle: 'weapon', target: 'Hero_1-weapon', targetHandle: '@in', type: EntityEdgeType.Normal,
        })
    })

    it('struct 数组生成多个子实体，值显示为 []*N', () => {
        const schema = buildSchema()
        const {entityMap, creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero_1', {
            $type: 'Hero', skills: [{$type: 'Skill', name: 'fire'}, {$type: 'Skill', name: 'ice'}],
        } as never)!

        expect(entityMap.has('Hero_1-skills[0]')).toBe(true)
        expect(entityMap.has('Hero_1-skills[1]')).toBe(true)
        expect((e.fields as {name: string; value: string}[]).find(f => f.name === 'skills')!.value).toBe('[]*2')
        expect(e.sourceEdges).toHaveLength(2)
    })
})

describe('RecordEntityCreator.createRecordEntity 实体属性', () => {
    it('label 取自 getLabel($type)，arrayIndex 追加后缀', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero_1', {$type: 'Hero', name: 'x'} as never)!
        expect(e.label).toBe('Hero')

        const e2 = creator.createRecordEntity('Hero_1-skills[0]', {$type: 'Skill', name: 'fire'} as never, undefined, 3)!
        // label = getLabel('Skill') + '.' + 3
        expect(e2.label).toBe('Skill.3')
    })

    it('entityType 为 Normal，userData 为构造时的 refId', () => {
        const schema = buildSchema()
        const refId = {table: 'Hero', id: '7'}
        const {creator} = newCreator(schema, refId)
        const e = creator.createRecordEntity('Hero_7', {$type: 'Hero', name: 'x'} as never)!
        expect(e.entityType).toBe(EntityType.Normal)
        expect(e.userData).toBe(refId)
    })

    it('$type 缺失返回 null', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        expect(creator.createRecordEntity('X', {} as never)).toBeNull()
    })

    it('未知 $type（未在 schema 中）返回 null', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        expect(creator.createRecordEntity('X', {$type: 'Ghost'} as never)).toBeNull()
    })

    it('$ 前缀字段（$type/$note/$refs）不作为展示字段', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema)
        const e = creator.createRecordEntity('Hero_1', {
            $type: 'Hero', $note: 'n', $refs: [], name: 'x',
        } as never)!
        const names = (e.fields as {name: string}[]).map(f => f.name)
        expect(names).toEqual(['name'])
        expect(e.note).toBe('n')
    })
})

describe('RecordEntityCreator.createRecordEntity 引用边（createRefs）', () => {
    it('obj.$refs 命中 refs 列表时添加 Ref 边', () => {
        const schema = buildSchema()
        const {creator} = newCreator(schema, {table: 'Hero', id: '1'}, [
            {table: 'Weapon', id: '10', value: '', depth: 1},
        ] as never)
        const e = creator.createRecordEntity('Hero_1', {
            $type: 'Hero', name: 'x',
            $refs: [{firstField: 'weaponId', toTable: 'Weapon', toId: '10'}],
        } as never)!

        expect(e.sourceEdges).toContainEqual({
            sourceHandle: 'weaponId', target: 'Weapon_10', targetHandle: '@in',
            type: EntityEdgeType.Ref, label: undefined,
        })
    })
})

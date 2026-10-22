import {describe, it, expect} from 'vitest'
import {TableEntityCreator, UserData} from './tableEntityCreator.ts'
import {EntityEdgeType, EntityType} from '@/domain/entityModel.ts'
import {Schema} from '@/domain/schema.ts'
import {field, fk, makeInterface, makeRawSchema, makeStruct, makeTable} from '@/test/fixtures.ts'

// 构造测试用 schema：Hero 依赖 Cost(struct) 与 IPet(interface)，并有 FK→Team；IPet.enumRef→PetEnum
function buildSchema() {
    const hero = makeTable('Hero', [
        field('id', 'int'),
        field('cost', 'Cost'),
        field('pet', 'IPet'),
        field('teamId', 'int'),
    ], {
        foreignKeys: [fk('fk_team', ['teamId'], 'Team', {refType: 'rPrimary'})],
    })
    const cost = makeStruct('Cost', [field('gold', 'int')])
    const dog = makeStruct('Dog', [field('bite', 'int')])
    const cat = makeStruct('Cat', [field('claw', 'int')])
    const ipet = makeInterface('IPet', [dog, cat], {enumRef: 'PetEnum'})
    const team = makeTable('Team', [field('id', 'int')], {recordIds: [{id: '1'}]})
    const petEnum = makeTable('PetEnum', [field('id', 'int')], {recordIds: [{id: '1'}]})
    const schema = new Schema(makeRawSchema([hero, cost, ipet, team, petEnum]))
    return {schema, hero}
}

describe('TableEntityCreator.includeSubStructs', () => {
    it('显示局部索引、混合来源、目标唯一键与结果类型，不为ROOT引用猜根表', () => {
        const {schema, hero} = buildSchema()
        hero.fields[1].index = ['gold']
        hero.foreignKeys = [fk('target', ['::teamId'], 'Team', {
            refType: 'rObject', refKeys: ['code'], resultType: 'Cost',
            pathReference: {
                sources: [{start: 'ANCESTOR', owner: 'Hero', fields: ['teamId']}, {start: 'CURRENT', owner: '', fields: ['teamId']}],
                target: {start: 'TABLE', owner: 'Team', fields: ['children']}, inline: false,
            },
        }), fk('fixed', [], '', {
            refType: 'rObject', resultType: 'Cost',
            pathReference: {sources: [], target: {start: 'ROOT', owner: '', fields: ['cost']}, inline: false},
        })]
        const entities = new Map()
        const creator = new TableEntityCreator(entities, schema, hero, 10)
        creator.includeSubStructs()
        creator.includeRefTables()
        const fields = entities.get('t-Hero').fields
        expect(fields.find((field: {name: string}) => field.name === 'cost').value).toBe('Cost [index: gold]')
        expect(fields.find((field: {name: string}) => field.name === '→target').value).toBe('[@Hero::teamId, teamId] → Team[code]::children : Cost')
        expect(fields.find((field: {name: string}) => field.name === '→fixed').value).toBe('[] → ::cost : Cost')
        expect(entities.get('t-Hero').sourceEdges.filter((edge: {type: EntityEdgeType}) => edge.type === EntityEdgeType.Ref)).toHaveLength(1)
    })

    it('祖先目标只连接图中已有的类型入口', () => {
        const {schema, hero} = buildSchema()
        hero.foreignKeys = ['Cost', 'Missing'].map(owner => fk(owner, [], '', {
            refType: 'rObject', resultType: 'Cost',
            pathReference: {sources: [], target: {start: 'ANCESTOR', owner, fields: ['gold']}, inline: false},
        }))
        const entities = new Map()
        const creator = new TableEntityCreator(entities, schema, hero, 10)
        creator.includeSubStructs()
        creator.includeRefTables()
        const references = entities.get('t-Hero').sourceEdges.filter((edge: {type: EntityEdgeType}) => edge.type === EntityEdgeType.Ref)
        expect(references).toHaveLength(1)
        expect(references[0]).toMatchObject({sourceHandle: '@out', target: 't-Cost', targetHandle: '@in'})
        expect(entities.has('t-Missing')).toBe(false)
    })

    it('内部引用只投影外部表，并为跨层来源使用节点级端口', () => {
        const {schema, hero} = buildSchema()
        hero.foreignKeys = [
            fk('child', ['::teamId'], 'Team', {
                refType: 'rObject',
                pathReference: {
                    sources: [{start: 'ROOT', owner: '', fields: ['teamId']}],
                    target: {start: 'TABLE', owner: 'Team', fields: ['children']},
                    inline: false,
                },
            }),
            fk('local', [], '', {
                refType: 'rObject',
                pathReference: {
                    sources: [], target: {start: 'ROOT', owner: '', fields: ['cost']}, inline: false,
                },
            }),
        ]
        const entities = new Map()
        const creator = new TableEntityCreator(entities, schema, hero, 10)
        creator.includeSubStructs()
        creator.includeRefTables()
        const edges = entities.get('t-Hero').sourceEdges.filter((e: {type: EntityEdgeType}) => e.type === EntityEdgeType.Ref)
        expect(edges).toHaveLength(1)
        expect(edges[0]).toMatchObject({sourceHandle: '@out', target: 't-Team'})
    })

    it('从 curTable 出发展开依赖闭包，生成各结构体/接口/实现节点', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        new TableEntityCreator(entityMap, schema, hero, 10).includeSubStructs()

        expect(entityMap.has('t-Hero')).toBe(true)
        expect(entityMap.has('t-Cost')).toBe(true)
        expect(entityMap.has('t-IPet')).toBe(true)
        expect(entityMap.has('t-IPet.Dog')).toBe(true)
        expect(entityMap.has('t-IPet.Cat')).toBe(true)
    })

    it('struct/table 节点的字段 value 为字段类型', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        new TableEntityCreator(entityMap, schema, hero, 10).includeSubStructs()

        const heroEntity = entityMap.get('t-Hero')
        const fields = heroEntity.fields as {name: string; value: string}[]
        expect(fields.map(f => [f.name, f.value])).toEqual([
            ['id', 'int'], ['cost', 'Cost'], ['pet', 'IPet'], ['teamId', 'int'],
        ])
    })

    it('interface 节点无字段', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        new TableEntityCreator(entityMap, schema, hero, 10).includeSubStructs()

        expect(entityMap.get('t-IPet').fields).toEqual([])
    })

    it('struct 字段依赖连边：sourceHandle=字段名，target=@in', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        new TableEntityCreator(entityMap, schema, hero, 10).includeSubStructs()

        const heroEntity = entityMap.get('t-Hero')
        const costEdge = heroEntity.sourceEdges.find((e: {target: string}) => e.target === 't-Cost')
        expect(costEdge).toMatchObject({
            sourceHandle: 'cost', target: 't-Cost', targetHandle: '@in', type: EntityEdgeType.Normal,
        })
        const petEdge = heroEntity.sourceEdges.find((e: {target: string}) => e.target === 't-IPet')
        expect(petEdge.sourceHandle).toBe('pet')
    })

    it('interface → impl 连边：sourceHandle=@out', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        new TableEntityCreator(entityMap, schema, hero, 10).includeSubStructs()

        const ipetEntity = entityMap.get('t-IPet')
        const targets = ipetEntity.sourceEdges.map((e: {target: string}) => e.target)
        expect(targets).toEqual(['t-IPet.Dog', 't-IPet.Cat'])
        expect(ipetEntity.sourceEdges[0].sourceHandle).toBe('@out')
    })

    it('maxImpl 限制展开的 impl 数量', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        new TableEntityCreator(entityMap, schema, hero, 1).includeSubStructs()

        expect(entityMap.has('t-IPet.Dog')).toBe(true)
        expect(entityMap.has('t-IPet.Cat')).toBe(false)
    })

    it('userData 记录所属表与原始 item', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        new TableEntityCreator(entityMap, schema, hero, 10).includeSubStructs()

        const ud = entityMap.get('t-Cost').userData as UserData
        expect(ud).toBeInstanceOf(UserData)
        expect(ud.table).toBe('Hero')
        expect(ud.item.name).toBe('Cost')
    })
})

describe('TableEntityCreator.includeRefTables', () => {
    it('外键目标表作为 Ref 节点加入，并连 Ref 边', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        const creator = new TableEntityCreator(entityMap, schema, hero, 10)
        creator.includeSubStructs()
        creator.includeRefTables()

        // Team 作为 Ref 节点加入
        const teamEntity = entityMap.get('t-Team')
        expect(teamEntity).toBeDefined()
        expect(teamEntity.entityType).toBe(EntityType.Ref)

        // Hero 上的 Ref 边：sourceHandle=外键字段名，targetHandle 取 Team.pk
        const heroEntity = entityMap.get('t-Hero')
        const refEdge = heroEntity.sourceEdges.find((e: {target: string}) => e.target === 't-Team')
        expect(refEdge).toMatchObject({
            sourceHandle: 'teamId', target: 't-Team', targetHandle: '@in_id', type: EntityEdgeType.Ref,
        })
    })

    it('interface 的 enumRef 作为 Ref 节点加入并连 @out 边', () => {
        const {schema, hero} = buildSchema()
        const entityMap = new Map()
        const creator = new TableEntityCreator(entityMap, schema, hero, 10)
        creator.includeSubStructs()
        creator.includeRefTables()

        expect(entityMap.has('t-PetEnum')).toBe(true)
        const ipetEntity = entityMap.get('t-IPet')
        const enumEdge = ipetEntity.sourceEdges.find((e: {target: string}) => e.target === 't-PetEnum')
        expect(enumEdge).toMatchObject({
            sourceHandle: '@out', target: 't-PetEnum', type: EntityEdgeType.Ref,
        })
    })
})

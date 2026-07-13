import {describe, it, expect} from 'vitest'
import {includeRefTables} from './tableRefEntity'
import {EntityType} from '@/domain/entityModel'
import {Schema} from '@/domain/schema'
import {field, fk, makeRawSchema, makeTable} from '@/test/fixtures'
import {STable} from '@/api/schemaModel'

// 依赖链：Hero →(fk)→ Team →(fk)→ Guild；Shop →(fk)→ Hero（使 Hero.refInTables = {Shop}）
function buildSchema(): {schema: Schema, hero: STable} {
    const hero = makeTable('Hero', [field('id', 'int'), field('teamId', 'int')], {
        foreignKeys: [fk('fk_team', ['teamId'], 'Team', {refType: 'rPrimary'})],
    })
    const team = makeTable('Team', [field('id', 'int'), field('guildId', 'int')], {
        foreignKeys: [fk('fk_guild', ['guildId'], 'Guild', {refType: 'rPrimary'})],
    })
    const guild = makeTable('Guild', [field('id', 'int')])
    const shop = makeTable('Shop', [field('id', 'int'), field('heroId', 'int')], {
        foreignKeys: [fk('fk_hero', ['heroId'], 'Hero', {refType: 'rPrimary'})],
    })
    const schema = new Schema(makeRawSchema([hero, team, guild, shop]))
    return {schema, hero}
}

describe('includeRefTables', () => {
    it('curTable 自身始终作为 Normal 节点入图', () => {
        const {schema, hero} = buildSchema()
        const map = new Map()
        includeRefTables(map, hero, schema, false, 0, 100)
        expect(map.has('Hero')).toBe(true)
        expect(map.get('Hero').entityType).toBe(EntityType.Normal)
    })

    it('refIn=false：沿外键 BFS 展开，depth1=Ref、depth2=Ref2，不含 refIn 节点', () => {
        const {schema, hero} = buildSchema()
        const map = new Map()
        includeRefTables(map, hero, schema, false, 2, 100)
        expect(map.has('Hero')).toBe(true)
        expect(map.has('Team')).toBe(true)
        expect(map.get('Team').entityType).toBe(EntityType.Ref)
        expect(map.has('Guild')).toBe(true)
        expect(map.get('Guild').entityType).toBe(EntityType.Ref2)
        expect(map.has('Shop')).toBe(false)  // refIn=false 不加引用方节点
    })

    it('maxOutDepth=1：只展开到 depth1，depth2 的 Guild 不入图', () => {
        const {schema, hero} = buildSchema()
        const map = new Map()
        includeRefTables(map, hero, schema, false, 1, 100)
        expect(map.has('Team')).toBe(true)
        expect(map.has('Guild')).toBe(false)
    })

    it('maxOutDepth=0：不展开任何外键目标（仅 curTable）', () => {
        const {schema, hero} = buildSchema()
        const map = new Map()
        includeRefTables(map, hero, schema, false, 0, 100)
        expect([...map.keys()]).toEqual(['Hero'])
    })

    it('refIn=true：把引用 curTable 的表作为 RefIn 节点加入并连边 → curTable', () => {
        const {schema, hero} = buildSchema()
        const map = new Map()
        includeRefTables(map, hero, schema, true, 0, 100)  // maxOutDepth=0，只验 refIn
        expect(map.has('Shop')).toBe(true)
        const shop = map.get('Shop')
        expect(shop.entityType).toBe(EntityType.RefIn)
        const edge = shop.sourceEdges[0]
        expect(edge.target).toBe('Hero')
        expect(edge.sourceHandle).toBe('@out')
        expect(edge.targetHandle).toBe('@in')
    })

    it('maxNode 截断：maxNode=1 时 depth2 的 Guild 被截（软上限，depth1 仍入图）', () => {
        const {schema, hero} = buildSchema()
        const map = new Map()
        includeRefTables(map, hero, schema, false, 2, 1)
        expect(map.has('Hero')).toBe(true)
        expect(map.has('Team')).toBe(true)
        expect(map.has('Guild')).toBe(false)  // size 超 maxNode 后 break，Guild 未入图
    })

    it('BFS 连边：每层实体向其已在图中的直接 ref 连边', () => {
        const {schema, hero} = buildSchema()
        const map = new Map()
        includeRefTables(map, hero, schema, false, 2, 100)
        const heroTargets = map.get('Hero').sourceEdges.map((e: {target: string}) => e.target)
        expect(heroTargets).toContain('Team')
        const teamTargets = map.get('Team').sourceEdges.map((e: {target: string}) => e.target)
        expect(teamTargets).toContain('Guild')
    })
})

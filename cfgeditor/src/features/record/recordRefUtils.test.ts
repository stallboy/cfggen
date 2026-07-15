import {describe, it, expect} from 'vitest'
import {getLastName, getLabel, getId, createRefs, createRefEntities} from './recordRefUtils.ts'
import {Entity, EntityEdgeType, EntitySourceEdge, EntityType} from '@/domain/entityModel.ts'
import {BriefRecord, Refs} from '@/api/recordModel.ts'
import {Schema} from '@/domain/schema.ts'
import {field, makeRawSchema, makeTable} from '@/test/fixtures.ts'

describe('getLastName / getLabel / getId', () => {
    it('getLastName 取最后一个 . 段', () => {
        expect(getLastName('a.b.c')).toBe('c')
        expect(getLastName('solo')).toBe('solo')
    })

    it('getLabel 去掉首个命名空间段', () => {
        expect(getLabel('ns.Hero')).toBe('Hero')
        expect(getLabel('game.sys.Hero')).toBe('sys.Hero')
        expect(getLabel('Hero')).toBe('Hero')
    })

    it('getId 用 _ 连接 table 与 id', () => {
        expect(getId('Hero', '1')).toBe('Hero_1')
    })

    it('getLabel 对 getLabel 再取getId 的典型用法', () => {
        // createRefEntities 中 label = getId(getLabel(table), id)
        expect(getId(getLabel('game.Hero'), '1')).toBe('Hero_1')
    })
})

describe('createRefs', () => {
    function makeEntity() {
        return {
            id: 'Hero_1', label: 'Hero', type: 'card' as const,
            brief: {value: ''}, sourceEdges: [] as EntitySourceEdge[],
        }
    }

    it('为目标在 briefRecords 中的引用添加 Ref 边（按 firstField handle）', () => {
        const entity = makeEntity()
        const refs: Refs = {$refs: [{firstField: 'weapon', toTable: 'Weapon', toId: '10'}]}
        const briefs: BriefRecord[] = [{table: 'Weapon', id: '10', value: '', depth: 1}]

        createRefs(entity as Entity, refs, briefs)

        expect(entity.sourceEdges).toEqual([{
            sourceHandle: 'weapon',
            target: 'Weapon_10',
            targetHandle: '@in',
            type: EntityEdgeType.Ref,
            label: undefined,
        }])
    })

    it('目标不在 briefRecords 中时不加边', () => {
        const entity = makeEntity()
        const refs: Refs = {$refs: [{firstField: 'weapon', toTable: 'Weapon', toId: '99'}]}
        createRefs(entity as Entity, refs, [])
        expect(entity.sourceEdges).toHaveLength(0)
    })

    it('checkTable 返回 false 时过滤该引用', () => {
        const entity = makeEntity()
        const refs: Refs = {$refs: [{firstField: 'weapon', toTable: 'Weapon', toId: '10'}]}
        const briefs: BriefRecord[] = [{table: 'Weapon', id: '10', value: '', depth: 1}]
        createRefs(entity as Entity, refs, briefs, () => false)
        expect(entity.sourceEdges).toHaveLength(0)
    })

    it('isEnityBrief=true 时源 handle 用 @out', () => {
        const entity = makeEntity()
        const refs: Refs = {$refs: [{firstField: 'weapon', toTable: 'Weapon', toId: '10'}]}
        const briefs: BriefRecord[] = [{table: 'Weapon', id: '10', value: '', depth: 1}]
        createRefs(entity as Entity, refs, briefs, undefined, true)
        expect(entity.sourceEdges[0].sourceHandle).toBe('@out')
    })

    it('无 $refs 时为无操作', () => {
        const entity = makeEntity()
        createRefs(entity as Entity, {} as Refs, [])
        expect(entity.sourceEdges).toHaveLength(0)
    })
})

describe('createRefEntities', () => {
    function schemaWith(tables: string[]) {
        return new Schema(makeRawSchema(
            tables.map(t => makeTable(t, [field('id', 'int')], {recordIds: [{id: '1'}]})),
        ))
    }

    // 让 findAllResInfos 直接返回 undefined，排除资源解析干扰
    const noRes = {tauriConf: {assetDir: '', assetRefTable: '', resDirs: []}, resourceDir: '', resMap: new Map()}

    it('为每条 briefRecord 生成 card 实体，entityType 随 depth 变化', () => {
        const schema = schemaWith(['Hero', 'Weapon'])
        const entityMap = new Map()
        const briefs: BriefRecord[] = [
            {table: 'Hero', id: '1', value: 'h', depth: 0},
            {table: 'Weapon', id: '10', value: 'w', depth: 1},
        ]
        createRefEntities({
            entityMap, schema, briefRecordRefs: briefs, isCreateRefs: false, ...noRes,
        })

        const hero = entityMap.get('Hero_1')
        const weapon = entityMap.get('Weapon_10')
        expect(hero).toBeDefined()
        expect(weapon).toBeDefined()
        expect(hero.entityType).toBe(EntityType.Normal)
        expect(weapon.entityType).toBe(EntityType.Ref)
        // label = getId(getLabel(table), id)
        expect(hero.label).toBe('Hero_1')
    })

    it('depth>1 标记为 Ref2，depth<0 标记为 RefIn', () => {
        const schema = schemaWith(['A', 'B', 'C'])
        const entityMap = new Map()
        const briefs: BriefRecord[] = [
            {table: 'A', id: '1', value: '', depth: 2},
            {table: 'B', id: '1', value: '', depth: -1},
        ]
        createRefEntities({entityMap, schema, briefRecordRefs: briefs, isCreateRefs: false, ...noRes})
        expect(entityMap.get('A_1').entityType).toBe(EntityType.Ref2)
        expect(entityMap.get('B_1').entityType).toBe(EntityType.RefIn)
    })

    it('schema 中不存在的表被跳过', () => {
        const schema = schemaWith(['Hero'])
        const entityMap = new Map()
        const briefs: BriefRecord[] = [{table: 'Ghost', id: '1', value: '', depth: 0}]
        createRefEntities({entityMap, schema, briefRecordRefs: briefs, isCreateRefs: false, ...noRes})
        expect(entityMap.size).toBe(0)
    })

    it('isCreateRefs=true 时在 brief 之间建立 @out 引用边', () => {
        const schema = schemaWith(['Hero', 'Weapon'])
        const entityMap = new Map()
        const briefs: BriefRecord[] = [
            {table: 'Hero', id: '1', value: 'h', depth: 0, $refs: [{firstField: 'weapon', toTable: 'Weapon', toId: '10'}]},
            {table: 'Weapon', id: '10', value: 'w', depth: 1},
        ]
        createRefEntities({entityMap, schema, briefRecordRefs: briefs, isCreateRefs: true, ...noRes})
        const hero = entityMap.get('Hero_1')
        expect(hero.sourceEdges).toEqual([{
            sourceHandle: '@out', target: 'Weapon_10', targetHandle: '@in',
            type: EntityEdgeType.Ref, label: undefined,
        }])
    })

    it('recordRefInShowLinkMaxNode：RefIn 数量超限时抑制其引用边', () => {
        const schema = schemaWith(['Hero', 'A', 'B'])
        const entityMap = new Map()
        // 2 个 depth=-1 的 RefIn，maxNode=1 → 超限 → isRefInNotShowLink
        const briefs: BriefRecord[] = [
            {table: 'Hero', id: '1', value: 'h', depth: 0, $refs: [
                {firstField: 'a', toTable: 'A', toId: '1'},
                {firstField: 'b', toTable: 'B', toId: '1'},
            ]},
            {table: 'A', id: '1', value: '', depth: -1},
            {table: 'B', id: '1', value: '', depth: -1},
        ]
        createRefEntities({
            entityMap, schema, briefRecordRefs: briefs, isCreateRefs: true,
            recordRefInShowLinkMaxNode: 1, ...noRes,
        })
        // RefIn 实体仍创建，但其 @out 边被抑制
        expect(entityMap.get('A_1').sourceEdges).toHaveLength(0)
        expect(entityMap.get('B_1').sourceEdges).toHaveLength(0)
    })
})

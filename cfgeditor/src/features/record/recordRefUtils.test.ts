import {describe, it, expect} from 'vitest'
import {getLabel, getId, createRefs, createRefEntities, createEditingRefs, incomingObjectReferences, locateRecordRefs} from './recordRefUtils.ts'
import {getLastSegment} from '@/domain/strUtils.ts'
import {Entity, EntityEdgeType, EntitySourceEdge, EntityType} from '@/domain/entityModel.ts'
import {BriefRecord, JSONObject, Refs} from '@/api/recordModel.ts'
import {Schema} from '@/domain/schema.ts'
import {field, makeRawSchema, makeTable, makeEditable, makeReadOnly, makeCard} from '@/test/fixtures.ts'
import {EditingSession} from '@/services/editingSession'
import {prepareEditingObject, referencesNeedValidation} from '@/services/editingObjectUtils'

describe('getLastSegment / getLabel / getId', () => {
    it('getLastSegment 取最后一个 . 段', () => {
        expect(getLastSegment('a.b.c')).toBe('c')
        expect(getLastSegment('solo')).toBe('solo')
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
    it('allows a same-record internal reference without adding a brief copy of the record', () => {
        const entity = makeReadOnly({id: 'Hero_1', label: 'Hero'})
        const refs = {$refs: [{firstField: 'phase', toTable: 'Hero', toId: '1', toPath: ['phases', 1], toRevision: 'a'}]}
        createRefs(entity, refs, [], undefined, false, {table: 'Hero', id: '1'})
        const target = makeReadOnly({id: 'Hero_1-phases[1]', label: 'phase'})
        locateRecordRefs(new Map([['Hero_1', entity], [target.id, target]]), 'a')
        expect(entity.sourceEdges[0].target).toBe(target.id)
    })
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

describe('validated editing references', () => {
    const record = {table: 'Hero', id: '1'}
    const reference = {firstField: 'next', toTable: 'Hero', toId: '1', toPath: ['phases', 1], toRevision: 'a', label: 'next'}
    const object = () => ({$type: 'Hero', phases: [{$type: 'Phase', $refs: [reference]}, {$type: 'Phase'}]}) as unknown as JSONObject

    it('projects metadata onto existing nodes and leaves the record payload unchanged', () => {
        const root = makeEditable({id: 'Hero_1', label: 'Hero'})
        const phase = makeEditable({id: 'Hero_1-phases[0]', label: 'phase'})
        const target = makeEditable({id: 'Hero_1-phases[1]', label: 'phase'})
        const entities = new Map<string, Entity>([[root.id, root], [phase.id, phase], [target.id, target]])
        const input = object()
        const before = structuredClone(input)
        createEditingRefs(entities, input, record, [], false)
        locateRecordRefs(entities, 'a')
        expect(phase.sourceEdges[0]).toMatchObject({sourceHandle: '@out', target: target.id, reference})
        expect(input).toEqual(before)
    })

    it('uses the visible parent for a hidden source and hidden target', () => {
        const root = makeEditable({id: 'Hero_1', label: 'folded'})
        const entities = new Map<string, Entity>([[root.id, root]])
        createEditingRefs(entities, object(), record, [], false)
        locateRecordRefs(entities, 'a')
        expect(root.sourceEdges[0]).toMatchObject({sourceHandle: '@out', target: root.id, label: 'next → phases[1]'})
    })

    it('removes derived positions while dirty and marks the record pending validation', () => {
        const root = makeEditable({id: 'Hero_1', label: 'Hero'})
        const entities = new Map<string, Entity>([[root.id, root]])
        createEditingRefs(entities, object(), record, [], true)
        expect(root.sourceEdges).toEqual([])
        expect(root.referencesPending).toBe(true)
    })

    it('shows an internal path on a brief target, and never uses mismatched array positions', () => {
        const root = makeReadOnly({id: 'Hero_1', label: 'Hero'})
        const target = makeCard({id: 'Other_2', label: 'brief'})
        const ref = {...reference, toTable: 'Other', toId: '2'}
        createRefs(root, {$refs: [ref]}, [{table: 'Other', id: '2', depth: 1, value: ''}])
        const entities = new Map<string, Entity>([[root.id, root], [target.id, target]])
        locateRecordRefs(entities, 'a')
        expect(root.sourceEdges[0]).toMatchObject({target: 'Other_2', label: 'next → phases[1]'})
        root.sourceEdges[0].label = 'next'
        entities.set('Other_2-phases[1]', makeReadOnly({id: 'Other_2-phases[1]', label: 'new position'}))
        locateRecordRefs(entities, 'b', 'pending')
        expect(root.sourceEdges[0]).toMatchObject({target: 'Other_2', label: 'next · pending'})
    })

    it('cycles produce edges between existing nodes without recursive copies', () => {
        const first = makeReadOnly({id: 'Hero_1-phases[0]', label: 'first'})
        const second = makeReadOnly({id: 'Hero_1-phases[1]', label: 'second'})
        const root = makeReadOnly({id: 'Hero_1', label: 'Hero'})
        const entities = new Map([[root.id, root], [first.id, first], [second.id, second]])
        createRefs(first, {$refs: [reference]}, [], undefined, false, record)
        createRefs(second, {$refs: [{...reference, toPath: ['phases', 0]}]}, [], undefined, false, record)
        locateRecordRefs(entities, 'a')
        expect(entities.size).toBe(3)
        expect(first.sourceEdges[0].target).toBe(second.id)
        expect(second.sourceEdges[0].target).toBe(first.id)
    })

    it('deduplicates incoming object paths so the existing brief open action is unambiguous', () => {
        const source = makeReadOnly({id: 'Hero_1', label: 'Hero'})
        const card = makeCard({id: 'Other_2', label: 'brief'})
        const entities = new Map<string, Entity>([[source.id, source], [card.id, card]])
        const ref = {...reference, toTable: 'Other', toId: '2'}
        createRefs(source, {$refs: [ref, {...ref, label: 'byCode'}]}, [{table: 'Other', id: '2', depth: 1, value: ''}])
        expect(incomingObjectReferences(entities, card.id)).toEqual([expect.objectContaining({toPath: ['phases', 1]})])
        createRefs(source, {$refs: [{...ref, toPath: ['phases', 0]}]}, [{table: 'Other', id: '2', depth: 1, value: ''}])
        expect(incomingObjectReferences(entities, card.id)).toHaveLength(2)
    })

    it('keeps old array positions hidden after commit until the corresponding new GET is available', () => {
        const original = {$type: 'Hero', phases: [{$type: 'Phase', key: 'A'}, {$type: 'Phase', key: 'B'}],
            $refs: [{...reference, toPath: ['phases', 0]}]} as unknown as JSONObject
        const session = new EditingSession({resultCode: 'ok', table: 'Hero', id: '1', object: original, refs: [], maxObjs: 0})
        session.initUndoBaseline()
        session.swapArrayItem(0, 1, ['phases'], {id: 'Hero_1', x: 0, y: 0})
        session.submit()
        session.onCommitSuccess()
        expect(session.getIsEdited()).toBe(false)
        const marker = {updatedAt: 1, revision: 'a'}
        const pending = referencesNeedValidation(session.getEditingObject(), prepareEditingObject(original), session.getIsEdited(), marker, marker)
        expect(pending).toBe(true)
        const root = makeEditable({id: 'Hero_1', label: 'Hero'})
        const entities = new Map<string, Entity>([[root.id, root], ['Hero_1-phases[0]', makeEditable({id: 'Hero_1-phases[0]', label: 'B'})],
            ['Hero_1-phases[1]', makeEditable({id: 'Hero_1-phases[1]', label: 'A'})]])
        createEditingRefs(entities, original, record, [], pending)
        expect(root.sourceEdges).toEqual([])
        expect(root.referencesPending).toBe(true)
        // A failed refetch leaves this same marker/data pending. A response carrying stale content is also insufficient.
        expect(referencesNeedValidation(session.getEditingObject(), prepareEditingObject(original), false, marker, marker)).toBe(true)
        expect(referencesNeedValidation(session.getEditingObject(), prepareEditingObject(original), false, {updatedAt: 2, revision: 'b'}, marker)).toBe(true)
        const refreshed = {...session.getEditingObject(), $refs: [{...reference, toRevision: 'b'}]} as unknown as JSONObject
        expect(referencesNeedValidation(session.getEditingObject(), prepareEditingObject(refreshed), false, {updatedAt: 2, revision: 'b'}, marker)).toBe(false)
        root.referencesPending = undefined
        createEditingRefs(entities, refreshed, record, [], false)
        locateRecordRefs(entities, 'b')
        expect(root.sourceEdges[0].target).toBe('Hero_1-phases[1]')
        session.dispose()
    })

    it('an unchanged save still waits for a response, while old services can validate without revision', () => {
        const object = {$type: 'Hero', id: 1}
        expect(referencesNeedValidation(object, object, false, {updatedAt: 1}, {updatedAt: 1})).toBe(true)
        expect(referencesNeedValidation(object, object, false, {updatedAt: 2}, {updatedAt: 1})).toBe(false)
        expect(referencesNeedValidation(object, object, false, {updatedAt: 2, revision: 'a'}, {updatedAt: 1, revision: 'a'})).toBe(true)
        expect(referencesNeedValidation(object, object, false, {updatedAt: 1, revision: 'b'}, {updatedAt: 1, revision: 'a'})).toBe(false)
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

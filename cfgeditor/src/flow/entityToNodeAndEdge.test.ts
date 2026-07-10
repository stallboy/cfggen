import {describe, it, expect} from 'vitest'
import {fillHandles, convertNodeAndEdges} from './entityToNodeAndEdge.ts'
import {Entity, EntityEdgeType, EntityGraph} from '@/domain/entityModel'
import {makeNodeShow, makeReadOnly, makeEditable, editWith} from '@/test/fixtures'
import {EntityEditField} from '@/domain/entityModel'

describe('fillHandles', () => {
    it('@out 源 handle 标记实体 handleOut', () => {
        const a = makeReadOnly({id: 'A', label: 'a', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'B', targetHandle: '@in', type: EntityEdgeType.Normal},
        ]})
        const b = makeReadOnly({id: 'B', label: 'b', fields: []})
        fillHandles(new Map([['A', a], ['B', b]]))

        expect(a.handleOut).toBe(true)
        expect(b.handleIn).toBe(true) // @in 标记目标 handleIn
    })

    it('字段名源 handle 标记对应字段的 handleOut（readonly）', () => {
        const a = makeReadOnly({id: 'A', label: 'a', fields: [
            {key: 'f1', name: 'f1', value: '1'},
        ], sourceEdges: [
            {sourceHandle: 'f1', target: 'B', targetHandle: '@in', type: EntityEdgeType.Normal},
        ]})
        const b = makeReadOnly({id: 'B', label: 'b', fields: []})
        fillHandles(new Map([['A', a], ['B', b]]))

        expect(a.fields[0].handleOut).toBe(true)
        expect(a.handleOut).toBeUndefined() // 未触发 @out
    })

    it('@in_<field> 目标 handle 标记目标实体对应字段的 handleIn', () => {
        const a = makeReadOnly({id: 'A', label: 'a', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'C', targetHandle: '@in_fc', type: EntityEdgeType.Normal},
        ]})
        const c = makeReadOnly({id: 'C', label: 'c', fields: [
            {key: 'fc', name: 'fc', value: '9'},
        ]})
        fillHandles(new Map([['A', a], ['C', c]]))

        expect(c.fields[0].handleIn).toBe(true)
        expect(c.handleIn).toBeUndefined()
    })

    it('editable 实体：在 edit.fields 与 interface.implFields 中查找字段', () => {
        const fields: EntityEditField[] = [
            {name: 'x', type: 'primitive', eleType: 'int', value: 0} as EntityEditField,
            {
                name: 'iface', type: 'interface', eleType: 'IF', value: 'Imp',
                autoCompleteOptions: {options: [], isValueInteger: false, isEnum: false},
                implFields: [{name: 'deep', type: 'primitive', eleType: 'int', value: 1} as EntityEditField],
                interfaceOnChangeImpl: () => {},
            } as EntityEditField,
        ]
        const e = makeEditable({id: 'E', label: 'e', edit: editWith(fields), sourceEdges: [
            {sourceHandle: 'deep', target: 'T', targetHandle: '@in', type: EntityEdgeType.Normal},
        ]})
        const t = makeReadOnly({id: 'T', label: 't', fields: []})
        fillHandles(new Map<string, Entity>([['E', e], ['T', t]]))

        const deepField = (e.edit.fields[1] as {implFields: {handleOut?: boolean}[]}).implFields[0]
        expect(deepField.handleOut).toBe(true)
    })

    it('目标实体不存在时不抛错', () => {
        const a = makeReadOnly({id: 'A', label: 'a', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'GHOST', targetHandle: '@in', type: EntityEdgeType.Normal},
        ]})
        expect(() => fillHandles(new Map([['A', a]]))).not.toThrow()
    })
})

describe('convertNodeAndEdges', () => {
    it('每个实体生成一个 node，位置固定 (100,100)，type=node', () => {
        const a = makeReadOnly({id: 'A', label: 'a', fields: []})
        const b = makeReadOnly({id: 'B', label: 'b', fields: []})
        const graph: EntityGraph = {entityMap: new Map([['A', a], ['B', b]])}

        const {nodes} = convertNodeAndEdges(graph)

        expect(nodes).toHaveLength(2)
        expect(nodes[0]).toMatchObject({id: 'A', type: 'node', position: {x: 100, y: 100}})
        expect((nodes[0].data as {entity: Entity}).entity.id).toBe('A')
    })

    it('按 sourceEdges 生成 edge，id 形如 source_target_n，自增计数', () => {
        const a = makeReadOnly({id: 'A', label: 'a', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'T1', targetHandle: '@in', type: EntityEdgeType.Normal},
        ]})
        const b = makeReadOnly({id: 'B', label: 'b', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'T2', targetHandle: '@in', type: EntityEdgeType.Ref},
        ]})
        const graph: EntityGraph = {entityMap: new Map([['A', a], ['B', b]])}

        const {edges} = convertNodeAndEdges(graph)

        expect(edges).toHaveLength(2)
        expect(edges[0]).toMatchObject({id: 'A_T1_1', source: 'A', target: 'T1', sourceHandle: '@out', targetHandle: '@in'})
        expect(edges[1].id).toBe('B_T2_2') // 全局自增
    })

    it('Ref 类型 edge 标记 animated，Normal 不标记', () => {
        const a = makeReadOnly({id: 'A', label: 'a', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'T', targetHandle: '@in', type: EntityEdgeType.Normal},
        ]})
        const b = makeReadOnly({id: 'B', label: 'b', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'T', targetHandle: '@in', type: EntityEdgeType.Ref},
        ]})
        const {edges} = convertNodeAndEdges({entityMap: new Map([['A', a], ['B', b]])})

        expect(edges[0].animated).toBeUndefined()
        expect(edges[1].animated).toBe(true)
    })

    it('无 sharedSetting 时边色为默认 #0898b5', () => {
        const a = makeReadOnly({id: 'A', label: 'a', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'T', targetHandle: '@in', type: EntityEdgeType.Normal},
        ]})
        const {edges} = convertNodeAndEdges({entityMap: new Map([['A', a]])})
        expect(edges[0].style).toEqual({stroke: '#0898b5'})
    })

    it('sharedSetting.nodeShow.edgeColor 决定边色并写入每个实体', () => {
        const ns = makeNodeShow({edgeColor: '#CCC'})
        const a = makeReadOnly({id: 'A', label: 'a', fields: [], sourceEdges: [
            {sourceHandle: '@out', target: 'T', targetHandle: '@in', type: EntityEdgeType.Normal},
        ]})
        const {edges} = convertNodeAndEdges({
            entityMap: new Map([['A', a]]),
            sharedSetting: {nodeShow: ns},
        })
        expect(edges[0].style).toEqual({stroke: '#CCC'})
        // sharedSetting 被回写到实体
        expect(a.sharedSetting?.nodeShow?.edgeColor).toBe('#CCC')
    })
})

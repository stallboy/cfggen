import {describe, expect, it} from 'vitest'
import {spreadFallbackNodes} from './fallbackNodes.ts'
import {EntityNode} from '../FlowGraph.tsx'

// 桩节点只需 id/position：spreadFallbackNodes 只读这两样，其余字段不参与。
function stubNode(id: string): EntityNode {
    return {id, position: {x: 100, y: 100}} as EntityNode
}

describe('spreadFallbackNodes', () => {
    it('保持节点数量与 id 顺序不变', () => {
        const nodes = ['a', 'b', 'c', 'd', 'e', 'f', 'g'].map(stubNode)
        const out = spreadFallbackNodes(nodes)
        expect(out.map(n => n.id)).toEqual(nodes.map(n => n.id))
    })

    it('铺开位置两两不重叠（不塌叠成一摞）', () => {
        const nodes = Array.from({length: 12}, (_, i) => stubNode(`n${i}`))
        const out = spreadFallbackNodes(nodes)
        const positions = new Set(out.map(n => `${n.position.x},${n.position.y}`))
        expect(positions.size).toBe(nodes.length)
    })

    it('不改动入参节点（纯函数）', () => {
        const nodes = [stubNode('a'), stubNode('b')]
        spreadFallbackNodes(nodes)
        expect(nodes[0].position).toEqual({x: 100, y: 100})
    })
})

import {describe, it, expect, vi, beforeEach} from 'vitest'

// vi.mock 的 factory 在 import 之前执行，用 vi.hoisted 把 mockLayout 提到可被 factory 引用的位置。
const {mockLayout} = vi.hoisted(() => ({mockLayout: vi.fn()}))

// mock ELK 主模块：layoutAsync 在模块加载时 `new ELK({workerUrl})`，这里用 mock 类替换，
// 其 layout 方法即 mockLayout。ElkNode/ElkExtendedEdge 是纯类型，运行时被擦除，无需提供。
vi.mock('elkjs/lib/elk-api.js', () => ({
    default: class MockELK {
        layout = mockLayout
    },
}))
// mock Vite 的 ?url 导入（worker 脚本 url），测试环境无 Vite 解析。
vi.mock('elkjs/lib/elk-worker.min.js?url', () => ({default: 'mock-worker-url'}))

import {layoutAsync, LayoutError} from './layoutAsync.ts'
import type {EntityNode} from '../FlowGraph.tsx'
import {makeReadOnly} from '@/test/fixtures'

function node(id: string): EntityNode {
    return {
        id,
        type: 'node',
        position: {x: 0, y: 0},
        data: {entity: makeReadOnly({id, label: id, fields: []})},
    }
}

describe('layoutAsync', () => {
    beforeEach(() => {
        mockLayout.mockReset()
    })

    it('成功时返回 id→rect map（含 ELK 算出的 x/y）', async () => {
        const nodes = [node('A'), node('B')]
        mockLayout.mockResolvedValue({
            children: [
                {id: 'A', x: 0, y: 0, width: 240, height: 40},
                {id: 'B', x: 300, y: 0, width: 240, height: 40},
            ],
        })
        const map = await layoutAsync(nodes, [], 'SIMPLE', undefined)
        expect(map.get('A')).toMatchObject({x: 0, y: 0})
        expect(map.get('B')).toMatchObject({x: 300, y: 0})
        // 尺寸由 nodeToLayoutChild 预填（calcWidthHeight 估算）
        expect(map.get('A')?.width).toBe(240)
        expect(map.get('A')?.height).toBe(40)
    })

    it('children 为 null 时 throw LayoutError(no_children) 而非 resolve undefined', async () => {
        mockLayout.mockResolvedValue({children: null})
        await expect(layoutAsync([node('A')], [], 'SIMPLE')).rejects.toBeInstanceOf(LayoutError)
        await expect(layoutAsync([node('A')], [], 'SIMPLE')).rejects.toMatchObject({code: 'no_children'})
    })

    it('nodes 存在重复 id（map.size < nodes.length）时 throw LayoutError(dropped_nodes)', async () => {
        // allPositionXYOk 的实际触发条件：id2RectMap 由 nodeToLayoutChild 按 node.id 预填，
        // 正常情况 map.size === nodes.length 恒成立（ELK 少返回 child 并不会缩小 map，因 map 不由 child 决定）；
        // 仅当出现重复 id（Map 去重后 map.size < nodes.length）时 guard 才判失败 → throw 交 react-query retry。
        const nodes = [node('A'), node('A')]
        mockLayout.mockResolvedValue({children: [{id: 'A', x: 0, y: 0, width: 240, height: 40}]})
        await expect(layoutAsync(nodes, [], 'SIMPLE')).rejects.toMatchObject({code: 'dropped_nodes'})
    })

    it('调用前 signal 已 abort 时 throw LayoutError(aborted)，不调用 elk.layout', async () => {
        const controller = new AbortController()
        controller.abort()
        mockLayout.mockResolvedValue({children: []})
        await expect(layoutAsync([], [], 'SIMPLE', undefined, controller.signal)).rejects.toMatchObject({code: 'aborted'})
        // aborted 在 elk.layout 之前判定，故不应调用 ELK
        expect(mockLayout).not.toHaveBeenCalled()
    })

    it('mrtree 策略同样走 throw 语义', async () => {
        mockLayout.mockResolvedValue({children: null})
        await expect(layoutAsync([node('A')], [], 'mrtree')).rejects.toMatchObject({code: 'no_children'})
    })

    it('LayoutError 是 Error 子类且带 code', () => {
        const e = new LayoutError('no_children', 'msg')
        expect(e).toBeInstanceOf(Error)
        expect(e.code).toBe('no_children')
        expect(e.name).toBe('LayoutError')
    })
})

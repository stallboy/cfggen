import type {NodeShowType} from './storageJson'

// NodeShowType 中影响 ELK 布局输出的字段（算法 / 间距 / 节点尺寸 / 拓扑过滤）。
// 改这些字段需要重跑 ELK；改其它字段（纯颜色/显示）不应触发重布局。
//
// 用途：
//   (1) useEntityToGraph 的 layout queryKey 只 pick 这些字段 —— 颜色变更→queryKey 不变→命中缓存，不重布局；
//   (2) store.setNodeShow 仅当这些字段变化时才 clearLayoutCache。
//
// 分类依据（逐字段核实）：
//   - *Layout（5 个）：layoutAsync 里 getLayoutStrategy 选的 ELK 算法。
//   - mrtreeSpacing/layeredSpacing/layeredNodeSpacing：layoutAsync 直接喂给 ELK 的间距参数。
//   - nodeWidth/editNodeWidth：calcWidthHeight 估宽 → ELK 不可压缩边界框。
//   - refShowDescription：getDsLenAndDesc → calcWidthHeight 估高（card 节点）。
//   - refTableHides/refContainEnum：RecordRef 的 checkTable 过滤建哪些实体 → 改拓扑（节点集合）。
// 故意排除的纯颜色/显示字段：nodeColor*/nodeColorsBy*/fieldColorsByName/edgeColor/edgeStrokeWidth/
//   editFoldColor/refIsShowCopyable —— 不进 ELK 输入。
export const NODESHOW_LAYOUT_KEYS = [
    'recordLayout', 'editLayout', 'refLayout', 'tableLayout', 'tableRefLayout',
    'mrtreeSpacing', 'layeredSpacing', 'layeredNodeSpacing',
    'nodeWidth', 'editNodeWidth',
    'refShowDescription',
    'refTableHides', 'refContainEnum',
] as const satisfies readonly (keyof NodeShowType)[]

export type NodeShowLayoutKey = typeof NODESHOW_LAYOUT_KEYS[number]

/** 从 NodeShowType 中挑出布局相关字段（用于 layout queryKey）。 */
export function pickLayoutKeys(nodeShow: NodeShowType): Pick<NodeShowType, NodeShowLayoutKey> {
    const result = {} as Pick<NodeShowType, NodeShowLayoutKey>
    for (const key of NODESHOW_LAYOUT_KEYS) {
        // 联合 key 动态写入会收窄为 never，转 Record 写入规避；读侧 nodeShow[key] 类型安全。
        (result as Record<string, unknown>)[key] = nodeShow[key]
    }
    return result
}

/**
 * 判断两个 NodeShowType 在"布局相关字段"上是否不同。
 * 用于 setNodeShow 决定是否需要 clearLayoutCache。
 * 注：refTableHides 是 string[]，表单每次产出新数组引用——必须按值比，否则改颜色（同内容新数组）
 * 会被误判为布局变化而清缓存。其余字段为基本类型，=== 即可。
 */
function shallowEqualValues(a: unknown, b: unknown): boolean {
    if (a === b) return true
    if (Array.isArray(a) && Array.isArray(b)) {
        return a.length === b.length && a.every((v, i) => v === b[i])
    }
    return false
}

export function layoutKeysChanged(a: NodeShowType, b: NodeShowType): boolean {
    for (const key of NODESHOW_LAYOUT_KEYS) {
        if (!shallowEqualValues(a[key], b[key])) return true
    }
    return false
}

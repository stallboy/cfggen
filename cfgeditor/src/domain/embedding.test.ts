import {describe, it, expect} from 'vitest'
import {JSONObject} from '@/api/recordModel'
import {SField, SInterface, SStruct} from '@/api/schemaModel'
import {
    EMBEDDING_CONFIG,
    canBeEmbeddedCheck,
    classifyListField,
    embedKey,
    extractEmbeddingFields,
    getEmbedState,
    normalizeOnAdd,
    normalizeOnDelete,
    normalizeOnImplSwitch,
} from './embedding'
import {field, makeInterface, makeStruct} from '@/test/fixtures'

// ---------------------------------------------------------------------------
// 就近工厂：被测函数（canBeEmbeddedCheck / extractEmbeddingFields）
// 均直接接收 SStruct | SInterface，无需构造 EmbeddingSchema 容器，故工厂只包字段/impl。
// ---------------------------------------------------------------------------
// JSONObject 要求 $type 必填，但 struct 判定不读 $type（仅 interface 分支读）。
// 这里给 struct 场景的字面量补占位 $type，满足类型又不影响判定。
function obj(values: Record<string, unknown> = {}): JSONObject {
    return {$type: '', ...values} as JSONObject
}

// 字段类型指向 'S'（struct）/ 'I'（interface），value 默认空对象
function checkStruct(fields: SField[], value: JSONObject = obj()) {
    return canBeEmbeddedCheck(value, makeStruct('S', fields))
}

function checkInterface(impls: SStruct[], value: JSONObject, over: Partial<SInterface> = {}) {
    return canBeEmbeddedCheck(value, makeInterface('I', impls, over))
}

const int = (n = 'n') => field(n, 'int')
const bool = (n = 'b') => field(n, 'bool')

// 共用 itemType fixture：可内嵌（1 int）/ 不可内嵌（5 primitive 超阈值）
const embeddableType = makeStruct('S', [int()])
const notEmbeddableType = makeStruct('Big', [int('a'), int('b'), int('c'), int('d'), bool('e')])

// isPrimitiveType / isNumberType / PRIMITIVE_TYPES / NUMBER_TYPES 已下沉到 @/api/schemaModel，
// 其测试见 schemaModel.test.ts。本文件聚焦内嵌规则本身。

// ===========================================================================
// 阈值配置契约（魔数集中地，锁住防误改）
// ===========================================================================
describe('EMBEDDING_CONFIG 阈值契约', () => {
    it('struct 比 interface 更宽松：number/bool 阈值都更大', () => {
        expect(EMBEDDING_CONFIG.struct.maxNumberFields).toBeGreaterThan(EMBEDDING_CONFIG.interface.maxNumberFields)
        expect(EMBEDDING_CONFIG.struct.maxBoolFields).toBeGreaterThan(EMBEDDING_CONFIG.interface.maxBoolFields)
    })

    it('当前阈值快照（struct / interface 各值，调整规则时这里会先红）', () => {
        expect(EMBEDDING_CONFIG.struct).toStrictEqual({
            maxFieldsForEmpty: 0,
            maxFieldsForSinglePrimitive: 1,
            maxNumberFields: 3,
            maxBoolFields: 4,
            boolAndNumberCombination: {boolCount: 1, numberCount: 1, totalFields: 2},
        })
        expect(EMBEDDING_CONFIG.interface).toStrictEqual({
            maxFieldsForEmpty: 0,
            maxFieldsForSinglePrimitive: 1,
            maxNumberFields: 2,
            maxBoolFields: 3,
            boolAndNumberCombination: {boolCount: 1, numberCount: 1, totalFields: 2},
        })
    })

    it('空 list 字段计数前固定过滤（无开关，行为断言见下方「空 list 过滤」用例）', () => {
        // 配置上不再有 filterEmptyLists 开关：EMBEDDING_CONFIG 只剩 struct / interface 两套阈值
        expect(Object.keys(EMBEDDING_CONFIG).sort()).toEqual(['interface', 'struct'])
    })
})

// ===========================================================================
// canBeEmbeddedCheck — struct（5 条规则，对应 embedding.md 例子表）
// ===========================================================================
describe('canBeEmbeddedCheck — struct（5 条规则）', () => {
    // --- 条件 a：没有字段 ---
    it('a 没有字段（空 struct）→ 可内嵌', () => {
        expect(checkStruct([])).toBe(true)
    })

    // --- 条件 b：只有 1 个 primitive ---
    it('b 只有 1 个 primitive（int/bool/str 均可）→ 可内嵌', () => {
        expect(checkStruct([int()])).toBe(true)
        expect(checkStruct([bool()])).toBe(true)
        expect(checkStruct([field('s', 'str')])).toBe(true)
    })

    // --- 条件 c：全是 number，数量 ≤ 3 ---
    it('c 全是 number：2 个 → 可内嵌（Pos {x,y}）', () => {
        expect(checkStruct([field('x', 'int'), field('y', 'int')])).toBe(true)
    })
    it('c 边值：正好 3 个 number（=阈值）→ 可内嵌', () => {
        expect(checkStruct([int('a'), int('b'), int('c')])).toBe(true)
    })
    it('c 边值：4 个 number（超阈值 maxNumberFields=3）→ 不内嵌（Box {x,y,z,w}）', () => {
        expect(checkStruct([int('a'), int('b'), int('c'), int('d')])).toBe(false)
    })

    // --- 条件 d：全是 bool，数量 ≤ 4 ---
    it('d 边值：正好 4 个 bool（=阈值）→ 可内嵌', () => {
        expect(checkStruct([bool('a'), bool('b'), bool('c'), bool('d')])).toBe(true)
    })
    it('d 边值：5 个 bool（超阈值 maxBoolFields=4）→ 不内嵌', () => {
        expect(checkStruct([bool('a'), bool('b'), bool('c'), bool('d'), bool('e')])).toBe(false)
    })

    // --- 条件 e：恰好 1 bool + 1 number ---
    it('e 恰好 1 bool + 1 number → 可内嵌（Flag {on,cnt}）', () => {
        expect(checkStruct([field('on', 'bool'), field('cnt', 'int')])).toBe(true)
    })

    // --- allPrimitive 约束：任一字段非 primitive 即否决 ---
    it('allPrimitive 约束：仅含 struct 子字段 → 不内嵌（Wrap {pos:Pos}）', () => {
        expect(checkStruct([field('pos', 'Pos')])).toBe(false)
    })
    it('allPrimitive 约束：primitive + struct 混合 → 不内嵌（Mixed {a:int,b:Pos}）', () => {
        expect(checkStruct([field('a', 'int'), field('b', 'Pos')])).toBe(false)
    })
})

// ===========================================================================
// canBeEmbeddedCheck — interface（$type 解析 + 更严阈值）
// ===========================================================================
describe('canBeEmbeddedCheck — interface（$type 解析 + 更严阈值）', () => {
    const dog = makeStruct('Dog', [field('bite', 'int')])  // 单 primitive 可内嵌

    it('$type 缺失 → false（后端脏数据 / 新旧 schema 不一致）', () => {
        expect(checkInterface([dog], {} as JSONObject)).toBe(false)
    })

    it('$type 直接是 impl 名 → 解析成功', () => {
        expect(checkInterface([dog], {$type: 'Dog'})).toBe(true)
    })

    it('$type 带 module 前缀（mod.Dog）→ split(".") 取末段解析', () => {
        expect(checkInterface([dog], {$type: 'mod.Dog'})).toBe(true)
    })

    it('$type 指向不存在的 impl → false', () => {
        expect(checkInterface([dog], {$type: 'Cat'})).toBe(false)
    })

    it('number 阈值更严：3 个 number → interface 不内嵌（maxNumberFields=2）', () => {
        const three = makeStruct('Three', [int('a'), int('b'), int('c')])
        expect(checkInterface([three], {$type: 'Three'})).toBe(false)
    })

    it('number 阈值边值：2 个 number → interface 可内嵌', () => {
        const two = makeStruct('Two', [int('a'), int('b')])
        expect(checkInterface([two], {$type: 'Two'})).toBe(true)
    })
})

// ===========================================================================
// 不变量：同输入 struct vs interface 的阈值差异（最易在重构中误平）
// ===========================================================================
describe('canBeEmbeddedCheck — struct vs interface 阈值差异', () => {
    it('3 个 number：struct 可内嵌、interface 不可（最经典的阈值差异）', () => {
        const fields = [int('a'), int('b'), int('c')]
        expect(checkStruct(fields)).toBe(true)
        const three = makeStruct('Three', fields)
        expect(checkInterface([three], {$type: 'Three'})).toBe(false)
    })
})

// ===========================================================================
// extractEmbeddingFields
// ===========================================================================
describe('extractEmbeddingFields', () => {
    it('可内嵌：摊平为 {value,type,name,comment} 列表（保留 comment）', () => {
        const pos = makeStruct('Pos', [field('x', 'int', 'X 坐标'), field('y', 'int')])
        const r = extractEmbeddingFields(pos, obj({x: 1, y: 2}))
        expect(r).not.toBeNull()
        expect(r!.embeddedFields).toEqual([
            {value: 1, type: 'int', name: 'x', comment: 'X 坐标'},
            {value: 2, type: 'int', name: 'y', comment: ''},
        ])
    })

    it('不可内嵌（超阈值）→ null', () => {
        const box = makeStruct('Box', [int('a'), int('b'), int('c'), int('d')])  // 4 number 超阈值
        expect(extractEmbeddingFields(box, obj())).toBeNull()
    })

    it('空 struct → {embeddedFields: []}（非 null，区分「可内嵌但无字段」与「不可内嵌」）', () => {
        const empty = makeStruct('Empty', [])
        expect(extractEmbeddingFields(empty, obj())).toEqual({embeddedFields: []})
    })

    it('字段缺失 / 为 null 时回退类型默认值：bool→false, int/long/float→0, str/text→空串', () => {
        const cases: [string, unknown][] = [
            ['bool', false], ['int', 0], ['long', 0], ['float', 0], ['str', ''], ['text', ''],
        ]
        for (const [type, dft] of cases) {
            const s = makeStruct('S', [field('v', type)])  // 单 primitive 可内嵌
            expect(extractEmbeddingFields(s, obj())!.embeddedFields[0].value).toBe(dft)
            expect(extractEmbeddingFields(s, obj({v: null}))!.embeddedFields[0].value).toBe(dft)
        }
    })

    it('显式值优先于默认值', () => {
        const s = makeStruct('S', [field('i', 'int')])
        expect(extractEmbeddingFields(s, obj({i: 42}))!.embeddedFields[0].value).toBe(42)
    })

    it('interface 命中 defaultImpl → implNameToDisplay 为 undefined（无需额外标注）', () => {
        const dog = makeStruct('Dog', [field('bite', 'int')])
        const iface = makeInterface('I', [dog], {defaultImpl: 'Dog'})
        const r = extractEmbeddingFields(iface, {$type: 'Dog'})
        expect(r).not.toBeNull()
        expect(r!.implNameToDisplay).toBeUndefined()
    })

    it('interface 命中非 defaultImpl → implNameToDisplay = impl 名（需标注具体实现）', () => {
        const dog = makeStruct('Dog', [field('bite', 'int')])
        const cat = makeStruct('Cat', [field('claw', 'int')])
        const iface = makeInterface('I', [dog, cat], {defaultImpl: 'Dog'})
        const r = extractEmbeddingFields(iface, {$type: 'Cat'})
        expect(r).not.toBeNull()
        expect(r!.implNameToDisplay).toBe('Cat')
    })

    // --- 空 list 字段过滤（common.filterEmptyLists）---
    it('空 list 字段被过滤：3 number + 空 list<int> → 按 3 number 内嵌，且不提取该字段', () => {
        const s = makeStruct('S', [int('a'), int('b'), int('c'), field('lst', 'list<int>')])
        const r = extractEmbeddingFields(s, obj({a: 1, b: 2, c: 3, lst: []}))
        expect(r).not.toBeNull()
        expect(r!.embeddedFields.map(f => f.name)).toEqual(['a', 'b', 'c'])
    })

    it('空 list 过滤使原本破坏 allPrimitive 的结构变可内嵌：2 number + 空 list → 可内嵌', () => {
        // 不过滤：3 字段含 list → allPrimitive=false → 不内嵌
        // 过滤空 list 后：剩 2 number allPrimitive → 命中条件 c → 内嵌
        const s = makeStruct('S', [int('a'), int('b'), field('lst', 'list<int>')])
        const r = extractEmbeddingFields(s, obj({a: 1, b: 2, lst: []}))
        expect(r).not.toBeNull()
        expect(r!.embeddedFields.map(f => f.name)).toEqual(['a', 'b'])
    })

    it('非空 list 字段保留 → 破坏 allPrimitive → 不内嵌 → null', () => {
        const s = makeStruct('S', [int('a'), int('b'), field('lst', 'list<int>')])
        expect(extractEmbeddingFields(s, obj({a: 1, b: 2, lst: [1, 2]}))).toBeNull()
    })
})

// ===========================================================================
// embed 状态机：键原语 + 读侧分类 + 写侧归一化政策（纯函数直测）
// ===========================================================================
describe('embedKey / getEmbedState（键原语）', () => {
    it('embedKey 加 $embed_ 前缀', () => {
        expect(embedKey('equipList')).toBe('$embed_equipList')
    })
    it('getEmbedState 读键值；无键 / 无 obj → undefined', () => {
        expect(getEmbedState(obj({$embed_x: true}), 'x')).toBe(true)
        expect(getEmbedState(obj({$embed_x: false}), 'x')).toBe(false)
        expect(getEmbedState(obj(), 'x')).toBeUndefined()
        expect(getEmbedState(undefined, 'x')).toBeUndefined()
    })
})

describe('classifyListField（4 稳态机读侧）', () => {
    it('类 1：恰 1 元素可内嵌、默认(undefined) → embedTag', () => {
        expect(classifyListField([obj({n: 1})], embeddableType, undefined)).toBe('embedTag')
    })
    it('类 1：恰 1 元素可内嵌、true（视同默认收起） → embedTag', () => {
        expect(classifyListField([obj({n: 1})], embeddableType, true)).toBe('embedTag')
    })
    it('类 1：恰 1 元素可内嵌、false（显式展开） → nodes', () => {
        expect(classifyListField([obj({n: 1})], embeddableType, false)).toBe('nodes')
    })
    it('类 2：多元素、true（收起） → summary', () => {
        expect(classifyListField([obj({n: 1}), obj({n: 2})], embeddableType, true)).toBe('summary')
    })
    it('类 2：多元素、默认(undefined) → nodes', () => {
        expect(classifyListField([obj({n: 1}), obj({n: 2})], embeddableType, undefined)).toBe('nodes')
    })
    it('单元素不可内嵌 → nodes（无内嵌选项）', () => {
        expect(classifyListField([obj()], notEmbeddableType, undefined)).toBe('nodes')
    })
    it('空数组 → nodes', () => {
        expect(classifyListField([], embeddableType, undefined)).toBe('nodes')
    })
})

describe('normalizeOnAdd', () => {
    it('0→1 可内嵌 → writeFalse（默认展开、立即可编辑）', () => {
        expect(normalizeOnAdd([obj()], embeddableType)).toBe('writeFalse')
    })
    it('1→2 → delete（false 键变残留需清）', () => {
        expect(normalizeOnAdd([obj(), obj()], embeddableType)).toBe('delete')
    })
    it('0→1 不可内嵌 → delete', () => {
        expect(normalizeOnAdd([obj()], notEmbeddableType)).toBe('delete')
    })
})

describe('normalizeOnDelete', () => {
    it('删到空 → delete', () => {
        expect(normalizeOnDelete([], embeddableType)).toBe('delete')
    })
    it('删到 1 且可内嵌、原键 true（收起） → delete（延续收起意图成内嵌 Tag）', () => {
        expect(normalizeOnDelete([obj()], embeddableType, true)).toBe('delete')
    })
    it('删到 1 且可内嵌、原键 false / undefined（展开） → writeFalse（保持展开）', () => {
        expect(normalizeOnDelete([obj()], embeddableType, false)).toBe('writeFalse')
        expect(normalizeOnDelete([obj()], embeddableType, undefined)).toBe('writeFalse')
    })
    it('删到 1 且不可内嵌 → delete（类 2 默认展开）', () => {
        expect(normalizeOnDelete([obj()], notEmbeddableType)).toBe('delete')
    })
    it('删到 ≥2 → noop（类 2 收起 true 仍合法）', () => {
        expect(normalizeOnDelete([obj(), obj()], notEmbeddableType, true)).toBe('noop')
        expect(normalizeOnDelete([obj(), obj(), obj()], embeddableType)).toBe('noop')
    })
})

describe('normalizeOnImplSwitch', () => {
    it('struct 字段：新 impl 可内嵌 → writeFalse（保持展开）', () => {
        expect(normalizeOnImplSwitch(obj(), embeddableType)).toBe('writeFalse')
    })
    it('struct 字段：新 impl 不可内嵌 → delete（清残留）', () => {
        expect(normalizeOnImplSwitch(obj(), notEmbeddableType)).toBe('delete')
    })
    it('list 元素：恰剩 1 且可内嵌 → writeFalse', () => {
        expect(normalizeOnImplSwitch(obj(), embeddableType, 1)).toBe('writeFalse')
    })
    it('list 元素：多元素 → delete（哪怕可内嵌）', () => {
        expect(normalizeOnImplSwitch(obj(), embeddableType, 2)).toBe('delete')
    })
    it('list 元素：恰剩 1 但不可内嵌 → delete', () => {
        expect(normalizeOnImplSwitch(obj(), notEmbeddableType, 1)).toBe('delete')
    })
})

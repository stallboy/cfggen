import {describe, it, expect} from 'vitest'
import {History, HistoryItem} from './historyModel.ts'

describe('History', () => {
    // ---------------------------------------------------------------------------
    // addItem
    // ---------------------------------------------------------------------------
    describe('addItem', () => {
        it('空历史时创建首条记录，索引为 0', () => {
            const h = new History().addItem('hero', '1')

            expect(h.items).toHaveLength(1)
            expect(h.items[0]).toStrictEqual(new HistoryItem('hero', '1'))
            expect(h.index).toBe(0)
            expect(h.cur()).toStrictEqual(new HistoryItem('hero', '1'))
        })

        it('与当前记录（table+id）相同时为无操作，返回同一实例', () => {
            const h = new History().addItem('hero', '1')
            const h2 = h.addItem('hero', '1')

            expect(h2).toBe(h)
            expect(h2.items).toHaveLength(1)
        })

        it('追加新记录并推进索引', () => {
            const h = new History().addItem('hero', '1').addItem('hero', '2')

            expect(h.items).toHaveLength(2)
            expect(h.index).toBe(1)
            expect(h.cur()).toStrictEqual(new HistoryItem('hero', '2'))
        })

        it('同表不同 id 时更新 lastOpenIdMap', () => {
            const h = new History()
                .addItem('hero', '1')
                .addItem('hero', '2')

            expect(h.findLastOpenId('hero')).toBe('2')
        })

        it('同表同 id（与当前相同）不更新 lastOpenIdMap', () => {
            const h = new History()
                .addItem('hero', '1')
                .addItem('hero', '2')
                .addItem('hero', '2') // 与当前相同 → 无操作

            expect(h.findLastOpenId('hero')).toBe('2')
            expect(h.items).toHaveLength(2)
        })

        it('不同表各自记录最后打开的 id', () => {
            const h = new History()
                .addItem('hero', '1')
                .addItem('item', 'a')
                .addItem('hero', '2')

            expect(h.findLastOpenId('hero')).toBe('2')
            expect(h.findLastOpenId('item')).toBe('a')
        })

        it('回退后再添加新记录会丢弃前进历史', () => {
            // 1 -> 2 -> 3，回退到 2，再添加 4：3 应被丢弃
            const h = new History()
                .addItem('hero', '1')
                .addItem('hero', '2')
                .addItem('hero', '3')
                .prev() // 指向 2
                .addItem('hero', '4')

            const ids = h.items.map(i => i.id)
            expect(ids).toEqual(['1', '2', '4'])
            expect(h.cur()?.id).toBe('4')
            expect(h.index).toBe(2)
        })

        it('连续添加超过 maxHistory 后稳定在 22 条（index 21），丢弃最旧记录', () => {
            // maxHistory=20：每次 addItem 用 slice(index-20, index+1) 取最近 21 条再加 1 条新条目，
            // 因此稳态为 22 条、index=21，继续添加只滚动丢弃最旧的一条。
            let h = new History()
            for (let i = 0; i < 30; i++) {
                h = h.addItem('hero', String(i))
            }

            expect(h.items).toHaveLength(22)
            expect(h.index).toBe(21)
            // 最近的 22 条为 "8".."29"
            expect(h.items[0].id).toBe('8')
            expect(h.cur()?.id).toBe('29')
        })

        it('刚达到上限边界（22 条）时长度为 22、index 为 21', () => {
            let h = new History()
            for (let i = 0; i < 22; i++) {
                h = h.addItem('hero', String(i))
            }
            expect(h.items).toHaveLength(22)
            expect(h.index).toBe(21)
            expect(h.cur()?.id).toBe('21')
        })
    })

    // ---------------------------------------------------------------------------
    // 导航
    // ---------------------------------------------------------------------------
    describe('导航', () => {
        it('canPrev：仅当索引大于 0 时可后退', () => {
            const empty = new History()
            expect(empty.canPrev()).toBe(false)

            const one = new History().addItem('t', '1')
            expect(one.canPrev()).toBe(false)

            const two = one.addItem('t', '2')
            expect(two.canPrev()).toBe(true)
        })

        it('prev：后退一步，items 不变仅移动索引', () => {
            const h = new History().addItem('t', '1').addItem('t', '2').prev()

            expect(h.index).toBe(0)
            expect(h.cur()?.id).toBe('1')
            expect(h.items).toHaveLength(2)
        })

        it('prev：在边界（无可后退）时返回同一实例', () => {
            const h = new History().addItem('t', '1')
            expect(h.prev()).toBe(h)
        })

        it('canNext：仅当索引小于末尾时可前进', () => {
            const h = new History().addItem('t', '1').addItem('t', '2').prev()
            expect(h.canNext()).toBe(true)

            // 回到末尾后不可前进
            const atEnd = h.next()
            expect(atEnd.canNext()).toBe(false)
        })

        it('next：前进一步', () => {
            const h = new History()
                .addItem('t', '1')
                .addItem('t', '2')
                .prev() // index 0
                .next() // index 1

            expect(h.index).toBe(1)
            expect(h.cur()?.id).toBe('2')
        })

        it('next：在末尾时返回同一实例', () => {
            const h = new History().addItem('t', '1').addItem('t', '2')
            expect(h.next()).toBe(h)
        })
    })

    // ---------------------------------------------------------------------------
    // cur / findLastOpenId
    // ---------------------------------------------------------------------------
    describe('cur / findLastOpenId', () => {
        it('cur：空历史返回 undefined', () => {
            expect(new History().cur()).toBeUndefined()
        })

        it('findLastOpenId：从未添加过的表返回 undefined', () => {
            const h = new History().addItem('hero', '1')
            expect(h.findLastOpenId('other')).toBeUndefined()
        })

        it('findLastOpenId：仅首条记录时不记录（无操作分支不写入 map）', () => {
            // addItem 的空历史分支返回空 Map，不写入首条 table/id
            const h = new History().addItem('hero', '1')
            expect(h.findLastOpenId('hero')).toBeUndefined()
        })
    })

    // ---------------------------------------------------------------------------
    // 不可变性
    // ---------------------------------------------------------------------------
    describe('不可变性', () => {
        it('addItem/prev/next 都返回新实例，不修改原 History', () => {
            const h = new History().addItem('t', '1')
            const originalIndex = h.index
            const originalLen = h.items.length

            h.addItem('t', '2')
            h.prev()
            h.next()

            expect(h.index).toBe(originalIndex)
            expect(h.items).toHaveLength(originalLen)
        })
    })
})

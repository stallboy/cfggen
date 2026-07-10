import {describe, it, expect} from 'vitest'
import {notesToMap, Notes} from './noteModel.ts'

describe('notesToMap', () => {
    it('把 notes 数组转成 key->note 的 Map', () => {
        const notes: Notes = {
            notes: [
                {key: 'hero_1', note: '主角'},
                {key: 'item_a', note: '道具A'},
            ],
        }

        const map = notesToMap(notes)

        expect(map).toBeInstanceOf(Map)
        expect(map.get('hero_1')).toBe('主角')
        expect(map.get('item_a')).toBe('道具A')
        expect(map.size).toBe(2)
    })

    it('空数组返回空 Map', () => {
        expect(notesToMap({notes: []}).size).toBe(0)
    })

    it('重复 key 时后者覆盖前者', () => {
        const map = notesToMap({
            notes: [
                {key: 'k', note: '旧'},
                {key: 'k', note: '新'},
            ],
        })
        expect(map.get('k')).toBe('新')
        expect(map.size).toBe(1)
    })

    it('保留空字符串 note', () => {
        const map = notesToMap({notes: [{key: 'k', note: ''}]})
        expect(map.get('k')).toBe('')
        expect(map.has('k')).toBe(true)
    })
})

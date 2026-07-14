import {describe, it, expect} from 'vitest'
import {getResBrief, getResBriefEmoji} from './getResBrief'
import {ResInfo} from '@/domain/resInfo'

function res(type: ResInfo['type'], over: Partial<ResInfo> = {}): ResInfo {
    return {type, name: 'n', path: 'p', ...over}
}

describe('getResBrief', () => {
    it('空数组 → 空串（全 0 计数都不出现）', () => {
        expect(getResBrief([])).toBe('')
    })

    it('单类计数：video/audio/image 各成一段；subtitles 落 default 计入 o', () => {
        expect(getResBrief([res('video')])).toBe('1v')
        expect(getResBrief([res('audio')])).toBe('1a')
        expect(getResBrief([res('image')])).toBe('1i')
        expect(getResBrief([res('subtitles')])).toBe('1o')
    })

    it('同类多个累加', () => {
        expect(getResBrief([res('video'), res('video'), res('video')])).toBe('3v')
    })

    it('拼串顺序固定为 v → a → i → o', () => {
        expect(getResBrief([res('video'), res('audio'), res('image'), res('subtitles')])).toBe('1v1a1i1o')
    })

    it('计数为 0 的类别不出现', () => {
        expect(getResBrief([res('video'), res('image')])).toBe('1v1i')  // 无 a/o
    })

    it('audioTracks 计入 a（每条音轨累加，含其所属资源自身的 audio 计数）', () => {
        const r = res('video', {audioTracks: [{name: 'x', path: 'p'}, {name: 'y', path: 'p'}]})
        expect(getResBrief([r])).toBe('1v2a')  // 1 video + 2 audio tracks
    })

    it('subtitlesTracks 计入 o（每条字幕累加）', () => {
        const r = res('video', {subtitlesTracks: [{name: 'x', path: 'p', lang: 'en'}]})
        expect(getResBrief([r])).toBe('1v1o')
    })

    it('聚合：混合各类 + 音轨 + 字幕', () => {
        const brief = getResBrief([
            res('video', {audioTracks: [{name: 'a', path: 'p'}]}),
            res('audio'),
            res('image'),
            res('other', {subtitlesTracks: [{name: 's', path: 'p', lang: 'zh'}]}),
        ])
        // v=1, a=1(audio)+1(track)=2, i=1, o=1(other)+1(sub track)=2
        expect(brief).toBe('1v2a1i2o')
    })
})

describe('getResBriefEmoji', () => {
    it('空数组 → 空串', () => {
        expect(getResBriefEmoji([])).toBe('')
    })

    it('计数与 getResBrief 同源（同输入同 v/a/i/o），仅呈现换 emoji+数字', () => {
        const input = [
            res('video', {audioTracks: [{name: 'a', path: 'p'}]}),
            res('audio'),
            res('image'),
            res('other', {subtitlesTracks: [{name: 's', path: 'p', lang: 'zh'}]}),
        ]
        // 与上例同输入：v=1, a=2, i=1, o=2
        expect(getResBriefEmoji(input)).toBe('🎬1 🔊2 🖼1 📎2')
    })

    it('计数为 0 的类别不出现', () => {
        expect(getResBriefEmoji([res('video'), res('image')])).toBe('🎬1 🖼1')
    })
})

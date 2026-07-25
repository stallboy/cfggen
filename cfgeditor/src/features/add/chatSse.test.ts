import {describe, it, expect} from 'vitest';
import {accumulateSseContent} from './chatSse.ts';

/** 构造一帧 {data: JSON.stringify(obj)}，模拟 x-sdk 流式帧的 data 字段载荷 */
const frame = (obj: unknown) => ({data: JSON.stringify(obj)});

describe('accumulateSseContent', () => {
    it('累积 delta.content，遇 finish_reason 返回 trim 后内容（首尾空白被 trim）', () => {
        const chunks = [
            frame({choices: [{delta: {content: ' {"a":'}}]}),
            frame({choices: [{delta: {content: '1} '}}]}),
            frame({choices: [{delta: {}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('{"a":1}');
    });

    it('跳过 [DONE] 哨兵帧', () => {
        const chunks = [
            {data: '[DONE]'},
            frame({choices: [{delta: {content: 'x'}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('x');
    });

    it('跳过 data 为 null 的帧', () => {
        const chunks = [
            {data: null},
            frame({choices: [{delta: {content: 'y'}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('y');
    });

    it('跳过非法 JSON 帧，不中断整条流', () => {
        const chunks = [
            {data: 'not json'},
            frame({choices: [{delta: {content: 'z'}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('z');
    });

    it('无 finish_reason 返回 null（流未完成）', () => {
        const chunks = [frame({choices: [{delta: {content: 'partial'}}]})];
        expect(accumulateSseContent(chunks)).toBe(null);
    });

    it('finish_reason 但内容空返回空串（调用方据此不 mutate）', () => {
        const chunks = [frame({choices: [{delta: {}, finish_reason: 'stop'}]})];
        expect(accumulateSseContent(chunks)).toBe('');
    });

    it('choices 缺失或为空数组时不崩', () => {
        const chunks = [
            frame({}),
            frame({choices: []}),
            frame({choices: [{delta: {content: 'ok'}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('ok');
    });

    it('delta.content 为空串时不累积', () => {
        const chunks = [
            frame({choices: [{delta: {content: ''}}]}),
            frame({choices: [{delta: {content: 'a'}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('a');
    });

    it('空 chunks 数组返回 null', () => {
        expect(accumulateSseContent([])).toBe(null);
    });

    it('finish_reason 后续帧被忽略（遇首个 finish 即 break）', () => {
        const chunks = [
            frame({choices: [{delta: {content: 'first'}, finish_reason: 'stop'}]}),
            frame({choices: [{delta: {content: 'IGNORED'}}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('first');
    });

    it('data 是 JSON null 不崩、后续帧恢复', () => {
        const chunks = [
            {data: 'null'},
            frame({choices: [{delta: {content: 'a'}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('a');
    });

    it('data 是非字符串非 null（数字）时跳过', () => {
        const chunks = [
            {data: 123},
            frame({choices: [{delta: {content: 'x'}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('x');
    });

    it('choices[0] 为 null 时跳过、后续帧恢复', () => {
        const chunks = [
            frame({choices: [null]}),
            frame({choices: [{delta: {content: 'y'}, finish_reason: 'stop'}]}),
        ];
        expect(accumulateSseContent(chunks)).toBe('y');
    });
});

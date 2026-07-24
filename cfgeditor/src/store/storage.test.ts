import {beforeEach, describe, expect, it} from 'vitest';
import {getPrefBool, getPrefEnumStr, getPrefInt, getPrefJson, getPrefStr} from './storage';

// storage.ts 中触达 Tauri fs 的（readPrefAsyncOnce / writeTextFile / debounce 调度）属副作用，不测；
// 此处只锁 localStorage 读 + 默认值回退的纯逻辑。各 getter 的「falsy 短路」行为
// （空串视为未设）是契约的一部分——navTo 不会写空串，但 readConf 从 yml 反序列化可能产生空串。

describe('storage localStorage 读 + 默认值回退', () => {
    beforeEach(() => {
        localStorage.clear();
    });

    describe('getPrefInt', () => {
        it('读取合法数字', () => {
            localStorage.setItem('k', '42');
            expect(getPrefInt('k', 0)).toBe(42);
        });

        it('负数', () => {
            localStorage.setItem('k', '-7');
            expect(getPrefInt('k', 0)).toBe(-7);
        });

        it('缺失返回默认', () => {
            expect(getPrefInt('k', 10)).toBe(10);
        });

        it('非数字字符串返回默认', () => {
            localStorage.setItem('k', 'abc');
            expect(getPrefInt('k', 10)).toBe(10);
        });

        it('空字符串返回默认（!v 短路，parseInt("")=NaN）', () => {
            localStorage.setItem('k', '');
            expect(getPrefInt('k', 10)).toBe(10);
        });

        it('数字带空格仍可解析（parseInt 容忍前导空白）', () => {
            localStorage.setItem('k', '  17');
            expect(getPrefInt('k', 0)).toBe(17);
        });
    });

    describe('getPrefBool', () => {
        it('true 字面量返回 true', () => {
            localStorage.setItem('k', 'true');
            expect(getPrefBool('k', false)).toBe(true);
        });

        it('false 字面量返回 false', () => {
            localStorage.setItem('k', 'false');
            expect(getPrefBool('k', true)).toBe(false);
        });

        it('非 "true" 字面量一律返回 false（严格字面匹配，不识别 "1"/"yes"）', () => {
            localStorage.setItem('k', '1');
            expect(getPrefBool('k', true)).toBe(false);
            localStorage.setItem('k', 'yes');
            expect(getPrefBool('k', true)).toBe(false);
        });

        it('缺失返回默认', () => {
            expect(getPrefBool('k', true)).toBe(true);
            expect(getPrefBool('k', false)).toBe(false);
        });

        it('空字符串返回默认（!v 短路）', () => {
            localStorage.setItem('k', '');
            expect(getPrefBool('k', true)).toBe(true);
        });
    });

    describe('getPrefStr', () => {
        it('读取字符串', () => {
            localStorage.setItem('k', 'hello');
            expect(getPrefStr('k', '')).toBe('hello');
        });

        it('缺失返回默认', () => {
            expect(getPrefStr('k', 'fallback')).toBe('fallback');
        });

        it('空字符串返回默认（!v 短路）', () => {
            localStorage.setItem('k', '');
            expect(getPrefStr('k', 'fallback')).toBe('fallback');
        });

        it('含特殊字符的字符串原样返回', () => {
            localStorage.setItem('k', 'a/b/c?d=e');
            expect(getPrefStr('k', '')).toBe('a/b/c?d=e');
        });
    });

    describe('getPrefEnumStr', () => {
        const enums = ['record', 'table', 'recordRef'];

        it('合法枚举值原样返回（类型断言由调用方负责）', () => {
            localStorage.setItem('curPage', 'record');
            expect(getPrefEnumStr('curPage', enums)).toBe('record');
            localStorage.setItem('curPage', 'recordRef');
            expect(getPrefEnumStr('curPage', enums)).toBe('recordRef');
        });

        it('非法值返回 undefined（不在枚举中）', () => {
            localStorage.setItem('curPage', 'nope');
            expect(getPrefEnumStr('curPage', enums)).toBeUndefined();
        });

        it('缺失返回 undefined', () => {
            expect(getPrefEnumStr('curPage', enums)).toBeUndefined();
        });

        it('空字符串返回 undefined（!v 短路，且不在枚举中）', () => {
            localStorage.setItem('curPage', '');
            expect(getPrefEnumStr('curPage', enums)).toBeUndefined();
        });
    });

    describe('getPrefJson', () => {
        it('合法 JSON 经 parser 解析返回对象', () => {
            localStorage.setItem('k', '{"a":1,"b":"x"}');
            const result = getPrefJson('k', (s) => JSON.parse(s) as {a: number; b: string});
            expect(result).toEqual({a: 1, b: 'x'});
        });

        it('parser 抛错时返回 undefined（吞异常，console.log 记录）', () => {
            localStorage.setItem('k', 'not json');
            const result = getPrefJson<{x: number}>('k', (s) => JSON.parse(s));
            expect(result).toBeUndefined();
        });

        it('缺失返回 undefined', () => {
            expect(getPrefJson('missing', (s) => JSON.parse(s))).toBeUndefined();
        });

        it('空字符串返回 undefined（!v 短路）', () => {
            localStorage.setItem('k', '');
            expect(getPrefJson('k', (s) => JSON.parse(s))).toBeUndefined();
        });

        it('自定义 parser（非 JSON.parse）也能正确路由', () => {
            // 模拟 Convert.toXxx 的反序列化路径：parser 产出的对象就是返回值
            localStorage.setItem('k', 'raw');
            const result = getPrefJson('k', (s) => ({parsed: s.toUpperCase()}));
            expect(result).toEqual({parsed: 'RAW'});
        });
    });
});

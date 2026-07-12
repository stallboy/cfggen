import {describe, expect, it} from 'vitest';
import {EditingSession, isDeeplyEqual} from './editingSession';
import {EFitView} from '@/domain/entityModel';
import {JSONArray, JSONObject, RecordResult} from '@/api/recordModel';
import {structCopy} from './clipboard';
import {Schema} from '@/domain/schema';
import {field, makeInterface, makeRawSchema, makeStruct} from '@/test/fixtures';

// 最小 recordResult fixture（maybeReset 只用 table/id/object）。
function makeRecord(table: string, id: string, object: JSONObject): RecordResult {
    return {resultCode: 'ok', table, id, maxObjs: 0, object, refs: []};
}

const POS = {id: 'n', x: 0, y: 0};

const ITEM = (): JSONObject => ({'$type': 'Item'});

describe('EditingSession.maybeReset', () => {
    it('早退：同 table/id 且内容相等时保留当前编辑态（不额外 bump）', () => {
        const rr = makeRecord('t', '1', {'$type': 'Foo', items: []});
        const s = new EditingSession(rr);
        const editingRef = s.getEditingObject();
        const v0 = s.getStructureVersion();

        s.addArrayItem(ITEM(), ['items'], POS); // 先做一次结构编辑
        const vAfterEdit = s.getStructureVersion();
        expect(vAfterEdit).toBe(v0 + 1);

        s.maybeReset(rr); // 同 recordResult → originalEditingObject 未变 → 早退
        expect(s.getEditingObject()).toBe(editingRef); // 引用未换，编辑态保留
        expect(s.getStructureVersion()).toBe(vAfterEdit); // maybeReset 未额外 bump
    });

    it('真 reset：不同 id 时重置，isEdited 归 false', () => {
        const rr = makeRecord('t', '1', {'$type': 'Foo', items: []});
        const s = new EditingSession(rr);
        s.addArrayItem(ITEM(), ['items'], POS);
        expect(s.getIsEdited()).toBe(true);

        const vBefore = s.getStructureVersion();
        s.maybeReset(makeRecord('t', '2', {'$type': 'Foo', items: []}));
        expect(s.getIsEdited()).toBe(false);
        expect(s.getStructureVersion()).toBe(vBefore + 1); // reset 也 bump
    });

    it('幂等：连续两次 maybeReset 相同 recordResult 只 reset 一次', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        const rr2 = makeRecord('t', '2', {'$type': 'Foo', items: []});
        s.maybeReset(rr2);
        const v = s.getStructureVersion();
        const obj = s.getEditingObject();

        s.maybeReset(rr2); // 同 rr2，内容相等 → 早退
        expect(s.getStructureVersion()).toBe(v);
        expect(s.getEditingObject()).toBe(obj);
    });

    it('真 reset（同 id 但服务端内容变了）：触发重置', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.addArrayItem(ITEM(), ['items'], POS);
        // 同 id，但 recordResult.object 与 originalEditingObject 不同（后台推了新数据）
        s.maybeReset(makeRecord('t', '1', {'$type': 'Foo', items: [{$type: 'Item'}]}));
        expect(s.getIsEdited()).toBe(false); // 以新内容为基准，未编辑
    });
});

describe('EditingSession 值类 vs 结构类（性能契约）', () => {
    it('值类 updateNote：就地改但不 bump structureVersion、不通知 listeners', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        const v0 = s.getStructureVersion();
        let notified = 0;
        const unsub = s.subscribe(() => {
            notified++;
        });

        s.updateNote('hello', []);
        expect(s.getStructureVersion()).toBe(v0); // 不 bump
        expect(notified).toBe(0); // 不 emit
        expect((s.getEditingObject() as JSONObject)['$note']).toBe('hello'); // 就地改成功
        unsub();
    });

    it('结构类 addArrayItem：bump structureVersion 并通知 listeners', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        const v0 = s.getStructureVersion();
        let notified = 0;
        const unsub = s.subscribe(() => {
            notified++;
        });

        s.addArrayItem(ITEM(), ['items'], POS);
        expect(s.getStructureVersion()).toBe(v0 + 1);
        expect(notified).toBe(1);
        unsub();
    });

    it('deleteArrayItem / swapArrayItem / updateInterfaceValue 均触发一次 emit', () => {
        const s = new EditingSession(makeRecord('t', '1', {
            '$type': 'Foo', items: [ITEM(), ITEM()], child: {$type: 'Child'}
        }));
        let notified = 0;
        s.subscribe(() => {
            notified++;
        });

        s.deleteArrayItem(0, ['items'], POS);
        s.swapArrayItem(0, 1, ['items'], POS);
        s.updateInterfaceValue({$type: 'Other'}, ['child'], POS);
        expect(notified).toBe(3);
    });
});

describe('EditingSession submit / replaceEditingObject', () => {
    it('submit 读到 in-place 编辑后的最新 editingObject', () => {
        let captured: JSONObject | null = null;
        const s = new EditingSession(
            makeRecord('t', '1', {'$type': 'Foo', items: []}),
            {mutate: (o) => { captured = o; }},
        );
        s.addArrayItem(ITEM(), ['items'], POS);
        s.submit();
        expect(captured).toBe(s.getEditingObject());
        expect((s.getEditingObject().items as JSONArray).length).toBe(1);
    });

    it('replaceEditingObject：getEditingObject 返回新引用，且判定为已编辑', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        const old = s.getEditingObject();
        const newObj: JSONObject = {'$type': 'Bar', items: [ITEM()]};
        s.replaceEditingObject(newObj);
        expect(s.getEditingObject()).toBe(newObj);
        expect(s.getEditingObject()).not.toBe(old);
        expect(s.getIsEdited()).toBe(true); // newObj !== originalEditingObject(旧 Foo)
    });

    it('replaceEditingObject：就地剥离入参的 $refs（与 prepareEditingObject 对齐）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        // 模拟 Chat/AddJson 的外部 JSON 带后端附加的 $refs 引用元数据
        const withRefs: JSONObject = {'$type': 'Bar', items: [], '$refs': [{$type: 'Ref'}]};
        s.replaceEditingObject(withRefs);
        expect('$refs' in s.getEditingObject()).toBe(false);   // $refs 被剥离
        expect(s.getEditingObject()['$type']).toBe('Bar');     // 其余字段保留
        expect(s.getEditingObject()).toBe(withRefs);           // 就地净化，仍共享引用（未 clone）
    });
});

describe('EditingSession.onCommitSuccess（提交边界：重置脏基准）', () => {
    it('重基准后 getIsEdited 归 false（提交成功 → 脏标记清除）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.addArrayItem(ITEM(), ['items'], POS);
        expect(s.getIsEdited()).toBe(true);
        s.onCommitSuccess();
        expect(s.getIsEdited()).toBe(false);
    });

    it('重基准为深拷：提交后再编辑不污染基准，isEdited 重新 true', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.addArrayItem(ITEM(), ['items'], POS);
        s.onCommitSuccess();                    // 提交成功，基准 = 当前态
        expect(s.getIsEdited()).toBe(false);
        s.addArrayItem(ITEM(), ['items'], POS); // 再编辑
        expect(s.getIsEdited()).toBe(true);     // 基准未被污染，新编辑判定 dirty
    });
});

describe('EditingSession.getEditingObjectRes（layout 通道）', () => {
    it('反映 fitView/isEdited 的变化', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        expect(s.getEditingObjectRes().fitView).toBe(EFitView.FitFull);
        expect(s.getEditingObjectRes().isEdited).toBe(false);

        s.addArrayItem(ITEM(), ['items'], POS);
        const res = s.getEditingObjectRes();
        expect(res.fitView).toBe(EFitView.FitId);
        expect(res.fitViewToIdPosition).toEqual(POS);
        expect(res.isEdited).toBe(true);
    });
});

describe('EditingSession fuzz（确定性伪随机混合操作）', () => {
    it('混合结构/值类/reset：structureVersion 单调不减，editingObject 始终有效', () => {
        // 确定性 LCG，避免测试不确定性
        let seed = 12345;
        const rand = () => {
            seed = (seed * 1103515245 + 12345) & 0x7fffffff;
            return seed / 0x7fffffff;
        };
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        let lastVersion = s.getStructureVersion();
        for (let i = 0; i < 300; i++) {
            const op = Math.floor(rand() * 5);
            switch (op) {
                case 0:
                    s.addArrayItem(ITEM(), ['items'], POS);  // 结构类：bump
                    break;
                case 1:
                    s.updateNote(`n${i}`, []);  // 值类：不 bump
                    break;
                case 2: {
                    const newId = String(Math.floor(rand() * 5));
                    s.maybeReset(makeRecord('t', newId, {'$type': 'Foo', items: []}));  // 同id早退 / 异id reset
                    break;
                }
                case 3:
                    s.replaceEditingObject({'$type': 'Bar', items: []});  // 结构类：bump
                    break;
                case 4:
                    s.deleteArrayItem(0, ['items'], POS);  // 空数组时 splice 无效但不崩，仍 bump
                    break;
            }
            expect(s.getStructureVersion()).toBeGreaterThanOrEqual(lastVersion);
            lastVersion = s.getStructureVersion();
            expect(s.getEditingObject()).toBeTruthy();
            expect(typeof s.getEditingObject()['$type']).toBe('string');
        }
    });

    it('subscribe/unsubscribe 配对：Set 语义 + 重复反注册安全', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        let count = 0;
        const listener = () => {
            count++;
        };
        const unsub1 = s.subscribe(listener);
        const unsub2 = s.subscribe(listener);  // 同一 listener 再注册（Set 去重）
        s.addArrayItem(ITEM(), ['items'], POS);
        expect(count).toBe(1);  // Set 去重：只通知一次
        unsub1();
        unsub2();  // 第二次 unsub 对已 delete 的无影响
        s.addArrayItem(ITEM(), ['items'], POS);
        expect(count).toBe(1);  // 反注册后不再通知（不 leak）
    });
});

describe('EditingSession.updateFormValues（类型转换 + $impl 早退）', () => {
    // schema: struct Foo { int x; float y; list<int> arr; }
    const schema = new Schema(makeRawSchema([
        makeStruct('Foo', [field('x', 'int'), field('y', 'float'), field('arr', 'list<int>')]),
    ]));

    it('int 字段：字符串转 number，非法字符串 → 0（toInt 的 NaN 兜底）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', x: 0, y: 0, arr: []}));
        s.updateFormValues(schema, {x: '123'}, []);
        expect(s.getEditingObject()['x']).toBe(123);
        s.updateFormValues(schema, {x: 'abc'}, []);
        expect(s.getEditingObject()['x']).toBe(0);   // parseInt('abc')=NaN → 0
    });

    it('float 字段：字符串转 number，非法 → 0（toFloat 的 NaN 兜底）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', x: 0, y: 0, arr: []}));
        s.updateFormValues(schema, {y: '1.5'}, []);
        expect(s.getEditingObject()['y']).toBe(1.5);
        s.updateFormValues(schema, {y: 'xyz'}, []);
        expect(s.getEditingObject()['y']).toBe(0);
    });

    it('list<int>：过滤 undefined 元素后逐个转 int', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', x: 0, y: 0, arr: []}));
        // antd Form 会返回含 undefined 的数组（保留上一个 form 的槽位），这里应被过滤
        s.updateFormValues(schema, {arr: [undefined, '2', undefined, '4']}, []);
        expect(s.getEditingObject()['arr']).toEqual([2, 4]);
    });

    it('$impl 与当前 $type 末尾名不一致时整段跳过（拦截 impl 切换过渡帧）', () => {
        const ie = makeInterface('IFoo', [
            makeStruct('ImplA', [field('x', 'int')], {id: 'IFoo.ImplA'}),
            makeStruct('ImplB', [field('y', 'str')], {id: 'IFoo.ImplB'}),
        ]);
        const schemaIe = new Schema(makeRawSchema([ie]));
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'IFoo.ImplA', x: 1}));
        // values 带 $impl=ImplB（≠ 当前 ImplA）+ 普通字段 x → 整段 return，x 不被改
        s.updateFormValues(schemaIe, {$impl: 'ImplB', x: '2'}, []);
        expect(s.getEditingObject()['x']).toBe(1);
    });
});

describe('EditingSession.pasteStruct（深拷贝独立性）', () => {
    it('多处粘贴不共享引用：改一处不影响后粘的另一处', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', a: {$type: 'A'}, b: {$type: 'B'}}));
        structCopy({'$type': 'Src', val: 1});
        s.pasteStruct(['a'], POS);
        (s.getEditingObject()['a'] as JSONObject)['val'] = 999;   // 改 a 处
        s.pasteStruct(['b'], POS);                                 // 再粘到 b
        const b = s.getEditingObject()['b'] as JSONObject;
        expect(b['val']).toBe(1);                                  // b 仍是原始副本（深拷贝生效）
        expect(b).not.toBe(s.getEditingObject()['a']);            // 且不与 a 共享引用
    });
});

describe('isDeeplyEqual（导出 + Set 优化）', () => {
    it('key 顺序不同但内容相同 → true', () => {
        expect(isDeeplyEqual({a: 1, b: 2}, {b: 2, a: 1})).toBe(true);
    });
    it('key 数量不同 → false', () => {
        expect(isDeeplyEqual({a: 1}, {a: 1, b: 2})).toBe(false);
    });
    it('一方有对方无的 key → false', () => {
        expect(isDeeplyEqual({a: 1, b: 2}, {a: 1, c: 2})).toBe(false);
    });
    it('嵌套对象 + 数组深比较', () => {
        expect(isDeeplyEqual({a: [1, {x: 2}]}, {a: [1, {x: 2}]})).toBe(true);
        expect(isDeeplyEqual({a: [1, {x: 3}]}, {a: [1, {x: 2}]})).toBe(false);
    });
    it('数组长度不同 → false', () => {
        expect(isDeeplyEqual([1, 2], [1, 2, 3])).toBe(false);
    });
});

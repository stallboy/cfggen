import {describe, expect, it, vi} from 'vitest';
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

describe('EditingSession undo/redo（结构类）', () => {
    it('结构编辑后 undo 恢复到编辑前', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.addArrayItem(ITEM(), ['items'], POS);
        expect((s.getEditingObject().items as JSONArray).length).toBe(1);
        s.undo();
        expect((s.getEditingObject().items as JSONArray).length).toBe(0);
    });

    it('undo 后 redo 恢复到编辑后', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.addArrayItem(ITEM(), ['items'], POS);
        s.undo();
        s.redo();
        expect((s.getEditingObject().items as JSONArray).length).toBe(1);
    });

    it('canUndo/canRedo 状态随操作切换', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        expect(s.canUndo()).toBe(false);
        expect(s.canRedo()).toBe(false);
        s.addArrayItem(ITEM(), ['items'], POS);
        expect(s.canUndo()).toBe(true);
        s.undo();
        expect(s.canUndo()).toBe(false);
        expect(s.canRedo()).toBe(true);
        s.redo();
        expect(s.canUndo()).toBe(true);
        expect(s.canRedo()).toBe(false);
    });

    it('undo 后 editingObject 独立于栈（深拷，后续编辑不污染历史）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.addArrayItem(ITEM(), ['items'], POS);
        s.undo();                                       // 回到 items=[]
        s.addArrayItem(ITEM(), ['items'], POS);         // 新编辑（分叉，redo 历史清）
        s.undo();
        expect((s.getEditingObject().items as JSONArray).length).toBe(0);  // 回到 baseline
    });

    it('结构 undo 视口稳定：fitView=KeepStable，锚点=被撤销操作节点 id', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.addArrayItem(ITEM(), ['items'], POS);   // POS.id='n'
        s.undo();
        const res = s.getEditingObjectRes();
        expect(res.fitView).toBe(EFitView.KeepStable);
        expect(res.fitViewToIdPosition).toEqual({id: 'n', x: 0, y: 0});
    });

    it('值类 undo 不动视口：fitView=NoChange', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.updateNote('hello', []);   // 值类（coalescing，undo 开头 flush 入 NoChange 快照）
        s.undo();
        expect(s.getEditingObjectRes().fitView).toBe(EFitView.NoChange);
    });

    it('整体替换 undo 重新铺满：fitView=FitFull', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.replaceEditingObject({'$type': 'Bar', items: []});
        s.undo();
        expect(s.getEditingObjectRes().fitView).toBe(EFitView.FitFull);
    });

    it('delete undo 锚点取父：fitViewToIdPosition.id = undoAnchorId', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: [ITEM()]}));
        s.initUndoBaseline();
        s.deleteArrayItem(0, ['items'], POS, 'parentId');   // 第4参 = undoAnchorId（父）
        s.undo();
        const res = s.getEditingObjectRes();
        expect(res.fitView).toBe(EFitView.KeepStable);
        expect(res.fitViewToIdPosition).toEqual({id: 'parentId', x: 0, y: 0});
    });

    it('replaceEditingObject 入栈可 undo', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.replaceEditingObject({'$type': 'Bar', items: [ITEM()]});
        expect(s.getEditingObject()['$type']).toBe('Bar');
        s.undo();
        expect(s.getEditingObject()['$type']).toBe('Foo');
    });

    it('分叉：undo 后新结构编辑清 redo 历史', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.addArrayItem(ITEM(), ['items'], POS);
        s.undo();
        expect(s.canRedo()).toBe(true);
        s.addArrayItem(ITEM(), ['items'], POS);   // 新结构编辑（分叉）
        expect(s.canRedo()).toBe(false);          // redo 历史作废
    });

    it('onCommitSuccess 清栈（提交后 undo 历史清）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: []}));
        s.initUndoBaseline();
        s.addArrayItem(ITEM(), ['items'], POS);
        expect(s.canUndo()).toBe(true);
        s.onCommitSuccess();
        expect(s.canUndo()).toBe(false);
        expect(s.canRedo()).toBe(false);
    });
});

describe('EditingSession 值类 coalescing', () => {
    // schema: struct Foo { int x; float y; list<int> arr; }
    const schema = new Schema(makeRawSchema([
        makeStruct('Foo', [field('x', 'int'), field('y', 'float'), field('arr', 'list<int>')]),
    ]));
    const rec = () => makeRecord('t', '1', {'$type': 'Foo', x: 0, y: 0, arr: []});

    it('同字段连续键入合并为一步 undo（换字段关闭旧组）', () => {
        const s = new EditingSession(rec());
        s.initUndoBaseline();
        s.updateFormValues(schema, {x: 1, y: 0, arr: []}, [], {x: 1});
        s.updateFormValues(schema, {x: 2, y: 0, arr: []}, [], {x: 2});
        s.updateFormValues(schema, {x: 3, y: 0, arr: []}, [], {x: 3});
        expect(s.canUndo()).toBe(false);   // 同字段 + 500ms 内 → 未 flush，不入栈
        s.updateFormValues(schema, {x: 3, y: 1, arr: []}, [], {y: 1});   // 换字段 → flush x 组
        expect(s.canUndo()).toBe(true);
        s.undo();   // 撤 y 组 → {x:3,y:0}
        expect(s.getEditingObject()['x']).toBe(3);
        expect(s.getEditingObject()['y']).toBe(0);
        s.undo();   // 撤 x 组（3 次键入一步撤）→ {x:0,y:0}
        expect(s.getEditingObject()['x']).toBe(0);
    });

    it('undo 开头 flush 未 capture 的键入（不丢最后一次输入）', () => {
        const s = new EditingSession(rec());
        s.initUndoBaseline();
        s.updateFormValues(schema, {x: 1, y: 0, arr: []}, [], {x: 1});
        expect(s.canUndo()).toBe(false);   // 未 flush
        s.undo();   // undo 开头 flush x 组 → capture → 撤 → x 回 0
        expect(s.getEditingObject()['x']).toBe(0);
    });

    it('Form.List 长度变 = 结构步（立即 capture，不经 coalescing）', () => {
        const s = new EditingSession(rec());
        s.initUndoBaseline();
        s.updateFormValues(schema, {x: 0, y: 0, arr: [1, 2]}, [], {arr: [1, 2]});   // arr 0→2
        expect(s.canUndo()).toBe(true);   // 结构步立即 capture
        s.undo();
        expect(s.getEditingObject()['arr']).toEqual([]);
    });

    it('Form.List 长度同 = 值类合并（不立即 capture）', () => {
        const s = new EditingSession(rec());
        s.initUndoBaseline();
        s.updateFormValues(schema, {x: 0, y: 0, arr: [1, 2]}, [], {arr: [1, 2]});   // 0→2 结构步
        s.updateFormValues(schema, {x: 0, y: 0, arr: [9, 2]}, [], {arr: [9, 2]});   // 长度同 2→2，值类合并
        expect(s.canUndo()).toBe(true);    // 第一步结构步已 capture
        // 第二步未 flush（同字段 arr + 500ms 内）→ 仍是 1 步
        s.undo();   // 撤第一步（arr 2→0）？还是第二步？
        // undo 开头 flush 第二步（arr=[9,2] capture）→ 栈=[snap{arr:[1,2]}, snap{arr:[9,2]}]
        // popUndo 弹 snap{arr:[9,2]} 返回 snap{arr:[1,2]} → arr=[1,2]
        expect(s.getEditingObject()['arr']).toEqual([1, 2]);
    });

    it('per-key O(1) 不变量：值类编辑不 bump structureVersion、不 emit（仅 flush 时 emit 一次）', () => {
        const s = new EditingSession(rec());
        s.initUndoBaseline();
        const v0 = s.getStructureVersion();
        let notified = 0;
        s.subscribe(() => notified++);
        s.updateFormValues(schema, {x: 1, y: 0, arr: []}, [], {x: 1});
        expect(s.getStructureVersion()).toBe(v0);   // 不 bump
        expect(notified).toBe(0);                    // 不 emit（coalescing 只设 timer）
        s.updateFormValues(schema, {x: 1, y: 2, arr: []}, [], {y: 2});   // 换字段 → flush
        expect(s.getStructureVersion()).toBe(v0);   // capture 不 bump
        expect(notified).toBe(1);                    // flush 的 emit
    });

    it('timer 500ms 到期 flush', () => {
        vi.useFakeTimers();
        try {
            const s = new EditingSession(rec());
            s.initUndoBaseline();
            s.updateFormValues(schema, {x: 1, y: 0, arr: []}, [], {x: 1});
            expect(s.canUndo()).toBe(false);
            vi.advanceTimersByTime(500);
            expect(s.canUndo()).toBe(true);   // timer 到期 flush
        } finally {
            vi.useRealTimers();
        }
    });

    it('dispose flush 未 capture 键入 + 清 timer（不抛、不残留 fire）', () => {
        vi.useFakeTimers();
        try {
            const s = new EditingSession(rec());
            s.initUndoBaseline();
            s.updateFormValues(schema, {x: 1, y: 0, arr: []}, [], {x: 1});
            s.dispose();   // flush + 清 timer + clear listeners
            vi.advanceTimersByTime(500);   // timer 已清，不 fire（不抛即过）
        } finally {
            vi.useRealTimers();
        }
    });

    it('结构操作前 flush 值类组（beforeStructuralChange）：键入与结构操作分开两步 undo', () => {
        const s = new EditingSession(rec());
        s.initUndoBaseline();
        s.updateFormValues(schema, {x: 5, y: 0, arr: []}, [], {x: 5});   // 键入 x=5（未 flush）
        s.updateFold(true, [], POS);   // 结构操作：beforeStructuralChange flush x 组，再 capture $fold 步
        expect(s.canUndo()).toBe(true);
        s.undo();   // 撤 $fold → x 仍 5
        expect(s.getEditingObject()['$fold']).toBeUndefined();
        expect(s.getEditingObject()['x']).toBe(5);
        s.undo();   // 撤 x 键入 → x 回 0
        expect(s.getEditingObject()['x']).toBe(0);
    });
});

describe('EditingSession list 折叠（$fold_<fieldName>）', () => {
    it('updateListFold(true) 在父对象上写 $fold_ 键，(false) 删键，各 emit 一次', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: [ITEM()]}));
        let notified = 0;
        s.subscribe(() => {
            notified++;
        });

        s.updateListFold(true, 'items', [], POS);
        expect(s.getEditingObject()['$fold_items']).toBe(true);
        s.updateListFold(false, 'items', [], POS);
        expect('$fold_items' in s.getEditingObject()).toBe(false);   // 删键，不残留 false
        expect(notified).toBe(2);
    });

    it('updateListFold 支持嵌套 parentChain（写到嵌套父对象上）', () => {
        const s = new EditingSession(makeRecord('t', '1', {
            '$type': 'Foo', child: {$type: 'Child', items: [ITEM()]}
        }));
        s.updateListFold(true, 'items', ['child'], POS);
        const child = s.getEditingObject()['child'] as JSONObject;
        expect(child['$fold_items']).toBe(true);
        expect('$fold_items' in s.getEditingObject()).toBe(false);   // 顶层不挂
    });

    it('往折叠中的 list 加元素 ⇒ 自动展开（删 $fold_ 键，同一步 undo）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: [ITEM()]}));
        s.initUndoBaseline();
        s.updateListFold(true, 'items', [], POS);
        const v0 = s.getStructureVersion();

        s.addArrayItem(ITEM(), ['items'], POS);
        expect('$fold_items' in s.getEditingObject()).toBe(false);   // 自动展开
        expect(s.getStructureVersion()).toBe(v0 + 1);                // 单步结构变更
        expect((s.getEditingObject()['items'] as JSONArray).length).toBe(2);

        s.undo();   // 一步撤掉「加元素 + 展开」
        expect(s.getEditingObject()['$fold_items']).toBe(true);
        expect((s.getEditingObject()['items'] as JSONArray).length).toBe(1);
    });

    it('addArrayItemAtIndex 同样自动展开', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: [ITEM()]}));
        s.updateListFold(true, 'items', [], POS);
        s.addArrayItemAtIndex(ITEM(), 0, ['items'], POS);
        expect('$fold_items' in s.getEditingObject()).toBe(false);
    });

    it('折叠中删到最后一个元素 ⇒ 自动展开（空 list 折叠无意义）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: [ITEM()]}));
        s.updateListFold(true, 'items', [], POS);
        s.deleteArrayItem(0, ['items'], POS);
        expect('$fold_items' in s.getEditingObject()).toBe(false);
    });

    it('未删空时保留折叠键', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: [ITEM(), ITEM()]}));
        s.updateListFold(true, 'items', [], POS);
        s.deleteArrayItem(0, ['items'], POS);
        expect(s.getEditingObject()['$fold_items']).toBe(true);
    });

    it('undo/redo 恢复 $fold_ 键（状态在 JSON 里，不走独立 state）', () => {
        const s = new EditingSession(makeRecord('t', '1', {'$type': 'Foo', items: [ITEM()]}));
        s.initUndoBaseline();
        s.updateListFold(true, 'items', [], POS);
        expect(s.getEditingObject()['$fold_items']).toBe(true);
        s.undo();
        expect('$fold_items' in s.getEditingObject()).toBe(false);
        s.redo();
        expect(s.getEditingObject()['$fold_items']).toBe(true);
    });
});

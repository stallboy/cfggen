import {describe, expect, it} from 'vitest';
import {EditingSession} from './editingSession';
import {EFitView} from '@/domain/entityModel';
import {JSONArray, JSONObject, RecordResult} from '@/api/recordModel';

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

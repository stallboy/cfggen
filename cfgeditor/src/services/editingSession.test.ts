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

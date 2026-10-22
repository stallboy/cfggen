import {describe, expect, it} from 'vitest';
import {referencePathLabel, resolveReferenceTarget, visibleRecordNode} from './referenceTarget';
import {FieldRef} from '@/api/recordModel';
import {Entity} from './entityModel';
import {makeCard, makeReadOnly} from '@/test/fixtures';

const reference = (path: (string | number)[] = ['machines', 0, 'phases', 1]): FieldRef => ({
    firstField: 'next', toTable: 'ability', toId: '1', toPath: path, toRevision: 'a',
});
function nodes(...ids: string[]): Map<string, Entity> {
    return new Map(ids.map(id => [id, makeReadOnly({id, label: id})]));
}

describe('reference target fieldChain', () => {
    it('locates a same-record internal object using the existing node id convention', () => {
        const map = nodes('ability_1', 'ability_1-machines[0]', 'ability_1-machines[0]-phases[1]');
        expect(resolveReferenceTarget(reference(), map, 'a')).toMatchObject({nodeId: 'ability_1-machines[0]-phases[1]', exact: true, stale: false});
    });

    it('maps interface values directly through the map entry value, without a type segment', () => {
        const map = nodes('ability_1', 'ability_1-actions[2]', 'ability_1-actions[2]-value');
        expect(resolveReferenceTarget(reference(['actions', 2, 'value']), map, 'a')?.nodeId).toBe('ability_1-actions[2]-value');
    });

    it('falls back to the nearest visible parent when a target is folded or embedded', () => {
        const map = nodes('ability_1', 'ability_1-machines[0]');
        expect(resolveReferenceTarget(reference(), map, 'a')).toMatchObject({nodeId: 'ability_1-machines[0]', exact: false, stale: false});
        expect(visibleRecordNode('ability_1', ['actions', 2, 'value'], nodes('ability_1', 'ability_1-actions[2]')))
            .toEqual({nodeId: 'ability_1-actions[2]', exact: false});
    });

    it('keeps a brief target at the card while exposing its internal path', () => {
        const map = new Map<string, Entity>([['ability_1', makeCard({id: 'ability_1', label: 'brief'})]]);
        expect(resolveReferenceTarget(reference(), map, 'a')).toMatchObject({nodeId: 'ability_1', exact: false, stale: false, pathLabel: 'machines[0].phases[1]'});
    });

    it('never uses old array positions in a new or missing revision', () => {
        const map = nodes('ability_1', 'ability_1-machines[0]-phases[1]');
        for (const revision of ['b', undefined]) {
            expect(resolveReferenceTarget(reference(), map, revision)).toMatchObject({nodeId: 'ability_1', exact: false, stale: true});
        }
        expect(resolveReferenceTarget({...reference(), toRevision: undefined}, map, 'a')).toMatchObject({nodeId: 'ability_1', stale: true});
        expect(resolveReferenceTarget({...reference(['machines', 1, 'phases', 0]), toRevision: 'b'},
            nodes('ability_1', 'ability_1-machines[1]-phases[0]'), 'b')?.nodeId).toBe('ability_1-machines[1]-phases[0]');
    });

    it('dirty editing cannot reuse positions even if the response revision matches', () => {
        expect(resolveReferenceTarget(reference(), nodes('ability_1', 'ability_1-machines[0]-phases[1]'), 'a', true))
            .toMatchObject({nodeId: 'ability_1', exact: false, stale: true});
    });

    it('legacy refs stay at the record root, and an empty new chain means the root object', () => {
        const map = nodes('ability_1');
        const legacy = {firstField: 'id', toTable: 'ability', toId: '1'};
        expect(resolveReferenceTarget(legacy, map)).toEqual({nodeId: 'ability_1', exact: false, stale: false});
        expect(resolveReferenceTarget(reference([]), map, 'a')).toMatchObject({nodeId: 'ability_1', exact: true, stale: false});
    });

    it('does not invent a node for a filtered or unloaded record', () => {
        expect(resolveReferenceTarget(reference(), nodes('other_1'), 'a')).toBeUndefined();
    });

    it('walks structured segments and formats map/list paths without splitting field names', () => {
        expect(visibleRecordNode('ability_1', ['field-with-dash', 2, 'value'], nodes('ability_1', 'ability_1-field-with-dash[2]')))
            .toEqual({nodeId: 'ability_1-field-with-dash[2]', exact: false});
        expect(referencePathLabel(['map', 2, 'value'])).toBe('map[2].value');
        expect(referencePathLabel([])).toBe('root');
    });
});

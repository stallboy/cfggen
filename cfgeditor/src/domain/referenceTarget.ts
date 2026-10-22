import {FieldRef} from '@/api/recordModel';
import {Entity} from './entityModel';

/** Reuse the record creator's existing fieldChain-to-node-id convention. */
export function visibleRecordNode(rootId: string, path: (string | number)[], entities: ReadonlyMap<string, Entity>) {
    if (!entities.has(rootId)) return undefined;
    let candidate = rootId;
    let nodeId = rootId;
    for (const part of path) {
        candidate += typeof part === 'number' ? `[${part}]` : `-${part}`;
        if (entities.has(candidate)) nodeId = candidate;
    }
    return {nodeId, exact: nodeId === candidate && entities.get(nodeId)?.type !== 'card'};
}

export function referencePathLabel(path: (string | number)[]): string {
    return path.reduce<string>((label, part) => typeof part === 'number' ? `${label}[${part}]`
        : label ? `${label}.${part}` : part, '') || 'root';
}

/** Array positions are meaningful only in the response snapshot that produced them. */
export function resolveReferenceTarget(ref: FieldRef, entities: ReadonlyMap<string, Entity>, revision?: string, dirty = false) {
    const rootId = `${ref.toTable}_${ref.toId}`;
    if (!entities.has(rootId)) return undefined;
    if (!ref.toPath) return {nodeId: rootId, exact: false, stale: false};
    const pathLabel = referencePathLabel(ref.toPath);
    if (dirty || !revision || !ref.toRevision || revision !== ref.toRevision) {
        return {nodeId: rootId, exact: false, stale: true, pathLabel};
    }
    if (entities.get(rootId)?.type === 'card') return {nodeId: rootId, exact: false, stale: false, pathLabel};
    return {...visibleRecordNode(rootId, ref.toPath, entities)!, stale: false, pathLabel};
}

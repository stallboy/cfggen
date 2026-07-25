/** 取 '.' 分隔 id 的最后一段（'a.b.c' → 'c'）。 */
export function getLastSegment(id: string): string {
    const seps = id.split('.');
    return seps[seps.length - 1];
}

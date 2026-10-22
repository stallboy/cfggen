type $RelationSource$ = { scope: string; fields: readonly number[] };
type $RelationStep$ = { field: number; kind: number; keys: readonly number[] };
type $RelationPlan$ = { root: string; slot: number; name: string; nullable: boolean; sources: readonly $RelationSource$[]; target: string; recordKey: readonly number[]; steps: readonly $RelationStep$[] };
interface $RelationValue$ { $relationFields$(): readonly unknown[] }
type $RelationType$ = { name: string; ctor: Function; indexes: readonly { field: number; keys: readonly number[] }[]; plans: readonly $RelationPlan$[]; bind: (value: object, refs: readonly unknown[]) => void };
type $RelationOccurrence$ = { value: object; root: object; table: string; parent: $RelationOccurrence$ | undefined };

function $relationKey$(parts: readonly unknown[]): string {
    return JSON.stringify(parts, (_key, value: unknown) => {
        if (typeof value === "bigint") return { $long: value.toString() };
        return value !== null && typeof value === "object" && "$relationFields$" in value
            ? (value as $RelationValue$).$relationFields$() : value;
    });
}

class $RelationResolver$ {
    private readonly types = new Map<Function, $RelationType$>();
    private readonly tables = new Map<string, readonly object[]>();
    private readonly fields = new Map<object, readonly unknown[]>();
    private readonly indexes = new Map<readonly unknown[], Map<string, Map<string, object>>>();
    private readonly maps = new Map<Map<unknown, unknown>, Map<string, unknown>>();
    private readonly occurrences: $RelationOccurrence$[] = [];
    constructor(private readonly errors: LoadErrors, types: readonly $RelationType$[]) {
        for (const type of types) this.types.set(type.ctor, type);
    }
    table(name: string, rows: readonly object[]): void { this.tables.set(name, rows); }
    private read(value: object): readonly unknown[] {
        let fields = this.fields.get(value);
        if (!fields) { fields = (value as $RelationValue$).$relationFields$(); this.fields.set(value, fields); }
        return fields;
    }
    resolve(): void {
        for (const [table, rows] of this.tables) for (const row of rows) this.walk(row, row, table);
        for (const at of this.occurrences) {
            const type = this.types.get(at.value.constructor)!;
            const refs: unknown[] = [];
            for (const plan of type.plans) if (plan.root === at.table) refs[plan.slot] = this.bind(at, type, plan);
            type.bind(at.value, refs);
        }
    }
    private bind(at: $RelationOccurrence$, type: $RelationType$, plan: $RelationPlan$): unknown {
        const keys: unknown[] = [];
        let valid = true;
        for (const source of plan.sources) {
            let value: unknown = this.scope(source.scope, at);
            if (value === undefined) { valid = false; continue; }
            for (const field of source.fields) value = this.read(value as object)[field];
            keys.push(value);
        }
        const external = plan.target.startsWith("table:");
        let target: unknown = external ? this.tables.get(plan.target.substring(6)) : this.scope(plan.target, at);
        if (!valid || target === undefined) throw Error("Missing reference scope: " + type.name + "." + plan.name);
        let used = 0;
        if (external) { target = this.index(target as readonly object[], plan.recordKey).get($relationKey$(keys.slice(0, plan.recordKey.length))); used = plan.recordKey.length; }
        for (const step of plan.steps) {
            if (target === undefined) break;
            const member = this.read(target as object)[step.field];
            if (step.kind === 0) target = member;
            else if (step.kind === 1) target = this.index(member as readonly object[], step.keys).get($relationKey$(keys.slice(used, used + step.keys.length)));
            else target = this.map(member as Map<unknown, unknown>).get($relationKey$([keys[used]]));
            used += step.kind === 2 ? 1 : step.keys.length;
        }
        if (target === undefined && !plan.nullable) this.errors.RefNull(type.name, at.table, plan.name);
        return target;
    }
    private scope(scope: string, at: $RelationOccurrence$): object | undefined {
        if (!scope) return at.value;
        if (scope === "root") return at.root;
        for (let current: $RelationOccurrence$ | undefined = at; current; current = current.parent) {
            const value = current.value;
            if ("ancestor:" + this.types.get(value.constructor)!.name === scope) return value;
        }
        return undefined;
    }
    private walk(value: unknown, root: object, table: string, parent?: $RelationOccurrence$): void {
        if (value === null || typeof value !== "object") return;
        const type = this.types.get(value.constructor);
        if (type) {
            const at = {value, root, table, parent};
            if (type.plans.length) this.occurrences.push(at);
            const fields = this.read(value);
            for (const index of type.indexes) this.index(fields[index.field] as readonly object[], index.keys);
            for (const field of fields) this.walk(field, root, table, at);
        } else if (Array.isArray(value)) {
            for (const item of value) this.walk(item, root, table, parent);
        } else if (value instanceof Map) {
            this.map(value);
            for (const [key, item] of value) { this.walk(key, root, table, parent); this.walk(item, root, table, parent); }
        }
    }
    private index(rows: readonly object[], keys: readonly number[]): Map<string, object> {
        let definitions = this.indexes.get(rows);
        if (!definitions) { definitions = new Map(); this.indexes.set(rows, definitions); }
        const definition = keys.join(",");
        let index = definitions.get(definition);
        if (!index) {
            index = new Map(); definitions.set(definition, index);
            for (const row of rows) {
                const fields = this.read(row); const key = $relationKey$(keys.map(field => fields[field]));
                if (index.has(key)) throw Error("Duplicate local index key: " + key);
                index.set(key, row);
            }
        }
        return index;
    }
    private map(rows: Map<unknown, unknown>): Map<string, unknown> {
        let index = this.maps.get(rows);
        if (!index) {
            index = new Map(); this.maps.set(rows, index);
            for (const [key, value] of rows) {
                const canonical = $relationKey$([key]);
                if (index.has(canonical)) throw Error("Duplicate map key: " + canonical);
                index.set(canonical, value);
            }
        }
        return index;
    }
}


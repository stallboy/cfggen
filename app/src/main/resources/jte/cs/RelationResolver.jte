private sealed class RelationResolver
{
    internal sealed record Source(string Scope, int[] Fields);
    internal sealed record Step(int Field, int Kind, int[] Keys);
    internal sealed record Index(int Field, int[] Keys);
    internal sealed record Plan(string Root, int Slot, string Name, bool Nullable, Source[] Sources, string Target, int[] RecordKey, Step[] Steps);
    internal sealed record Shape(string Name, Type RuntimeType, Func<object, object[]> Fields, Index[] Indexes, Plan[] Plans, Action<object, object?[]> Bind);
    private sealed record Occurrence(object Value, object Root, string Table, Occurrence? Parent);
    internal static object WrapMap<K, V>(IReadOnlyDictionary<K, V> map) where K : notnull
    {
        var view = new Dictionary<object, object>();
        foreach (var pair in map) view.Add(pair.Key, pair.Value!);
        return view;
    }
    private sealed class Identity : IEqualityComparer<object>
    {
        public new bool Equals(object? a, object? b) => ReferenceEquals(a, b);
        public int GetHashCode(object value) => System.Runtime.CompilerServices.RuntimeHelpers.GetHashCode(value);
    }
    private sealed class Key : IEquatable<Key>
    {
        private readonly object[] _parts;
        internal Key(object[] parts) { _parts = parts; }
        public bool Equals(Key? other) => other != null && System.Linq.Enumerable.SequenceEqual(_parts, other._parts);
        public override bool Equals(object? other) => other is Key key && Equals(key);
        public override int GetHashCode() { int hash = 17; foreach (var part in _parts) hash = unchecked(hash * 31 + part.GetHashCode()); return hash; }
    }
    private readonly Dictionary<Type, Shape> _types = new Dictionary<Type, Shape>();
    private readonly Dictionary<string, System.Collections.IEnumerable> _tables = new Dictionary<string, System.Collections.IEnumerable>();
    private readonly Dictionary<object, object[]> _fields = new Dictionary<object, object[]>(new Identity());
    private readonly Dictionary<object, Dictionary<string, Dictionary<Key, object>>> _indexes = new Dictionary<object, Dictionary<string, Dictionary<Key, object>>>(new Identity());
    private readonly List<Occurrence> _occurrences = new List<Occurrence>();
    private readonly LoadErrors _errors;

    internal RelationResolver(LoadErrors errors, Shape[] shapes)
    {
        _errors = errors;
        foreach (var shape in shapes) _types.Add(shape.RuntimeType, shape);
    }
    internal void Table(string name, System.Collections.IEnumerable rows) { _tables.Add(name, rows); }
    private object[] Read(object value)
    {
        if (!_fields.TryGetValue(value, out var fields)) {
            fields = _types[value.GetType()].Fields(value);
            _fields.Add(value, fields);
        }
        return fields;
    }

    internal void Resolve()
    {
        foreach (var table in _tables) foreach (object row in table.Value) Walk(row, row, table.Key, null);
        _errors.CheckErrors();
        foreach (var at in _occurrences) {
            var shape = _types[at.Value.GetType()];
            int slots = 0; foreach (var plan in shape.Plans) slots = Math.Max(slots, plan.Slot + 1);
            var refs = new object?[slots];
            foreach (var plan in shape.Plans) {
                if (plan.Root != at.Table) continue;
                refs[plan.Slot] = Bind(at, shape, plan);
            }
            shape.Bind(at.Value, refs);
        }
    }

    private object? Bind(Occurrence at, Shape shape, Plan plan)
    {
        var keys = new List<object>(plan.Sources.Length);
        bool valid = true;
        foreach (var source in plan.Sources) {
            object? value = Scope(source.Scope, at);
            if (value == null) { valid = false; continue; }
            foreach (int field in source.Fields) value = Read(value)[field];
            keys.Add(value);
        }
        bool external = plan.Target.StartsWith("table:", StringComparison.Ordinal);
        object? target = external ? _tables[plan.Target.Substring(6)] : Scope(plan.Target, at);
        if (!valid || target == null) {
            _errors.MissingReference(shape.Name, plan.Name, "scope", at.Table);
            return null;
        }
        int used = 0;
        if (external) {
            Lookup((System.Collections.IEnumerable)target, plan.RecordKey, plan.Target)
                .TryGetValue(new Key(keys.GetRange(0, plan.RecordKey.Length).ToArray()), out target);
            used = plan.RecordKey.Length;
        }
        foreach (var step in plan.Steps) {
            if (target == null) break;
            object member = Read(target)[step.Field];
            if (step.Kind == 0) target = member;
            else if (step.Kind == 1) Lookup((System.Collections.IEnumerable)member, step.Keys, shape.Name)
                .TryGetValue(new Key(keys.GetRange(used, step.Keys.Length).ToArray()), out target);
            else ((Dictionary<object, object>)member).TryGetValue(keys[used], out target);
            used += step.Kind == 2 ? 1 : step.Keys.Length;
        }
        if (target == null && !plan.Nullable) _errors.MissingReference(shape.Name, plan.Name, plan.Target, keys);
        return target;
    }

    private object? Scope(string scope, Occurrence at)
    {
        if (scope.Length == 0) return at.Value;
        if (scope == "root") return at.Root;
        for (Occurrence? current = at; current != null; current = current.Parent) {
            object value = current.Value;
            if ("ancestor:" + _types[value.GetType()].Name == scope) return value;
        }
        return null;
    }

    private void Walk(object value, object root, string table, Occurrence? parent)
    {
        if (_types.TryGetValue(value.GetType(), out var shape)) {
            var at = new Occurrence(value, root, table, parent);
            if (shape.Plans.Length != 0) _occurrences.Add(at);
            var fields = Read(value);
            foreach (var index in shape.Indexes) Lookup((System.Collections.IEnumerable)fields[index.Field], index.Keys, shape.Name);
            foreach (var field in fields) Walk(field, root, table, at);
        }
        else if (value is Dictionary<object, object> map) { foreach (var pair in map) { Walk(pair.Key, root, table, parent); Walk(pair.Value, root, table, parent); } }
        else if (value is System.Collections.IEnumerable list && value is not string) { foreach (object item in list) Walk(item, root, table, parent); }
    }

    private Dictionary<Key, object> Lookup(System.Collections.IEnumerable rows, int[] keys, string name)
    {
        if (!_indexes.TryGetValue(rows, out var definitions)) { definitions = new Dictionary<string, Dictionary<Key, object>>(); _indexes.Add(rows, definitions); }
        string definition = string.Join(",", keys);
        if (definitions.TryGetValue(definition, out var index)) return index;
        index = new Dictionary<Key, object>(); definitions.Add(definition, index);
        foreach (object row in rows) {
            var fields = Read(row); var parts = new object[keys.Length];
            for (int i = 0; i < keys.Length; i++) parts[i] = fields[keys[i]];
            if (!index.TryAdd(new Key(parts), row)) _errors.DuplicateKey(name, parts);
        }
        return index;
    }
}

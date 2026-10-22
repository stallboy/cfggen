package configgen.value;

import configgen.schema.*;

import java.util.*;

import static configgen.value.CfgValue.*;

/** Resolves references against one final value snapshot; caches use occurrence identity. */
public final class ValueReferences {
    public static final class Occurrence {
        private final VTable table;
        private final VStruct root;
        private final VStruct value;
        private final Occurrence parent;
        private final Position position;

        private Occurrence(VTable table, VStruct root, VStruct value, Occurrence parent, Position position) {
            this.table = table;
            this.root = root;
            this.value = value;
            this.parent = parent;
            this.position = position;
        }

        public VTable table() { return table; }
        public VStruct root() { return root; }
        public VStruct value() { return value; }
        public String path() { return diagnosticPath(position); }
        public List<Object> jsonPath() { return ValueReferences.jsonPath(position); }

        public String recordId() {
            return table.name() + "-" + ValueUtil.extractPrimaryKeyValue(root, table.schema()).packStr();
        }
    }

    /** One serialized path segment; map entry keys are formatted only for diagnostics. */
    private record Position(Position parent, Object part, SimpleValue mapKey) {}

    public record Result(SimpleValue target, VStruct targetRecord) {}
    public record ReferenceError(String record, String path, String reference, String reason)
            implements CfgValueErrs.VErr {}

    private final CfgValue cfg;
    private final List<Occurrence> occurrences = new ArrayList<>();
    private final Map<VStruct, Occurrence> positions = new IdentityHashMap<>();
    private final Map<VList, Map<Value, SimpleValue>> indexes = new IdentityHashMap<>();
    private final Map<VStruct, Map<ForeignKeySchema, Result>> results = new IdentityHashMap<>();

    /** Query snapshots omit invalid links; legacy schemas need no object-path resolver. */
    public static ValueReferences forQuery(CfgValue cfg) {
        if (!cfg.schema().references().hasPathReferences()) return null;
        ValueReferences references = new ValueReferences(cfg);
        references.resolve(CfgValueErrs.of());
        return references;
    }

    public ValueReferences(CfgValue cfg) {
        this.cfg = cfg;
        cfg.schema().requireResolved();
        for (VTable table : cfg.tables()) {
            for (VStruct root : table.valueList()) walk(table, root, root, null, null);
        }
    }

    public List<Occurrence> occurrences() { return Collections.unmodifiableList(occurrences); }

    public Result result(VStruct source, ForeignKeySchema reference) {
        return results.getOrDefault(source, Map.of()).get(reference);
    }

    public Occurrence occurrence(VStruct value) {
        return positions.get(value);
    }

    /** JSON fieldChain for this snapshot; map entries use their serialization order. */
    public List<Object> jsonPath(SimpleValue target) {
        VStruct struct = switch (target) {
            case VStruct value -> value;
            case VInterface value -> value.child();
            default -> null;
        };
        Occurrence occurrence = struct == null ? null : positions.get(struct);
        return occurrence == null ? null : occurrence.jsonPath();
    }

    /** Object-level incoming links compare identity, not field values. */
    public List<Occurrence> referencesTo(SimpleValue target) {
        Objects.requireNonNull(target);
        return occurrences.stream().filter(at -> results.getOrDefault(at.value(), Map.of()).values()
                .stream().anyMatch(r -> r.target() == target)).toList();
    }

    public void resolve(CfgValueErrs errors) {
        indexes.clear();
        results.clear();
        for (Occurrence at : occurrences) buildIndexes(at, errors);
        for (Occurrence at : occurrences) {
            for (ForeignKeySchema fk : at.value().schema().foreignKeys()) {
                if (fk.pathReference() == null) continue;
                try {
                    Result result = bind(at, fk);
                    results.computeIfAbsent(at.value(), ignored -> new IdentityHashMap<>()).put(fk, result);
                } catch (IllegalArgumentException e) {
                    errors.addErr(new ReferenceError(at.recordId(), at.path(), fk.name(), e.getMessage()));
                }
            }
        }
    }

    private void walk(VTable table, VStruct root, Value value, Occurrence parent, Position position) {
        switch (value) {
            case VInterface choice -> walk(table, root, choice.child(), parent, position);
            case VStruct struct -> {
                Occurrence occurrence = new Occurrence(table, root, struct, parent, position);
                occurrences.add(occurrence);
                positions.put(struct, occurrence);
                for (int i = 0; i < struct.values().size(); i++) {
                    Value member = struct.values().get(i);
                    if (!(member instanceof PrimitiveValue)) {
                        walk(table, root, member, occurrence,
                                new Position(position, struct.schema().fields().get(i).name(), null));
                    }
                }
            }
            case VList list -> {
                for (int i = 0; i < list.valueList().size(); i++) {
                    SimpleValue item = list.valueList().get(i);
                    if (!(item instanceof PrimitiveValue)) {
                        walk(table, root, item, parent, new Position(position, i, null));
                    }
                }
            }
            case VMap map -> {
                int index = 0;
                for (var entry : map.valueMap().entrySet()) {
                    int entryIndex = index++;
                    if (entry.getKey() instanceof PrimitiveValue && entry.getValue() instanceof PrimitiveValue) continue;
                    Position entryPosition = new Position(position, entryIndex, entry.getKey());
                    if (!(entry.getKey() instanceof PrimitiveValue)) {
                        walk(table, root, entry.getKey(), parent, new Position(entryPosition, "key", null));
                    }
                    if (!(entry.getValue() instanceof PrimitiveValue)) {
                        walk(table, root, entry.getValue(), parent, new Position(entryPosition, "value", null));
                    }
                }
            }
            case PrimitiveValue ignored -> {}
        }
    }

    private static List<Object> jsonPath(Position position) {
        List<Object> result = new ArrayList<>();
        for (Position current = position; current != null; current = current.parent()) result.add(current.part());
        Collections.reverse(result);
        return List.copyOf(result);
    }

    private static String diagnosticPath(Position position) {
        List<Position> parts = new ArrayList<>();
        for (Position current = position; current != null; current = current.parent()) parts.add(current);
        StringBuilder result = new StringBuilder();
        for (Position part : parts.reversed()) {
            if (part.mapKey() != null) {
                result.append('[').append(part.mapKey().packStr()).append(']');
            } else if (part.part() instanceof Integer index) {
                result.append('[').append(index).append(']');
            } else if (part.parent() == null || part.parent().mapKey() == null || !part.part().equals("value")) {
                // The serialized map value wrapper is absent from diagnostic paths.
                result.append('.').append(part.part());
            }
        }
        return result.toString();
    }

    private void buildIndexes(Occurrence at, CfgValueErrs errors) {
        List<FieldSchema> fields = at.value().schema().fields();
        for (int i = 0; i < fields.size(); i++) {
            FieldSchema field = fields.get(i);
            KeySchema key = cfg.schema().references().index(field);
            if (key == null) continue;
            VList list = (VList) at.value().values().get(i);
            Map<Value, SimpleValue> index = new LinkedHashMap<>();
            for (SimpleValue item : list.valueList()) {
                VStruct struct = (VStruct) item;
                Value value = ValueUtil.extractKeyValue(struct, FindFieldIndex.findFieldIndices(struct.schema(), key));
                if (index.containsKey(value)) {
                    // A null entry marks an ambiguous key, including a third or later occurrence.
                    index.put(value, null);
                    errors.addErr(new ReferenceError(at.recordId(), at.path() + "." + field.name(),
                            "index", "duplicate local key " + value.packStr()));
                } else {
                    index.put(value, item);
                }
            }
            indexes.put(list, index);
        }
    }

    private Result bind(Occurrence at, ForeignKeySchema fk) {
        ReferencePlan plan = cfg.schema().references().plan(fk, at.table().schema());
        if (plan == null) throw new IllegalArgumentException("reference has no plan for source table");
        List<SimpleValue> values = new ArrayList<>();
        for (ReferencePlan.Source source : plan.sources()) {
            VStruct current = start(at, source.start());
            Value value = current;
            for (int index : source.indices()) value = ((VStruct) value).values().get(index);
            values.add((SimpleValue) value);
        }
        // Validate all source scopes and the target scope before any nullable lookup can short-circuit.
        VStruct record;
        int offset = 0;
        if (plan.target().kind() == ReferencePath.Start.TABLE) {
            VTable table = cfg.getTable(plan.target().type().name());
            int count = plan.recordKey().fields().size();
            Value key = key(values, offset, count);
            offset += count;
            Map<Value, VStruct> rows = table == null ? Map.of()
                    : fk.refKey() instanceof RefKey.RefUniq
                    ? table.uniqueKeyMaps().getOrDefault(plan.recordKey().fields(), new LinkedHashMap<>())
                    : table.primaryKeyMap();
            record = rows.get(key);
            if (record == null) return missing(fk, null, "record " + key.packStr());
        } else {
            record = start(at, plan.target());
        }
        VStruct targetRoot = plan.target().kind() == ReferencePath.Start.TABLE ? record : at.root();
        SimpleValue current = record;
        for (ReferencePlan.Step step : plan.steps()) {
            Value field = ((VStruct) current).values().get(step.fieldIndex());
            int count = step.keyTypes().size();
            if (field instanceof VList list) {
                Value key = key(values, offset, count);
                Map<Value, SimpleValue> index = indexes.get(list);
                current = index.get(key);
                if (current == null && index.containsKey(key)) {
                    throw new IllegalArgumentException("ambiguous local key " + key.packStr());
                }
            } else if (field instanceof VMap map) {
                current = map.valueMap().get(key(values, offset, count));
            } else {
                current = (SimpleValue) field;
            }
            offset += count;
            if (current == null) {
                return missing(fk, targetRoot, step.field().name() + " using " + values.stream().map(Value::packStr).toList());
            }
        }
        return new Result(current, targetRoot);
    }

    private static VStruct start(Occurrence at, ReferencePlan.Start start) {
        return switch (start.kind()) {
            case CURRENT -> at.value();
            case ROOT -> at.root();
            case ANCESTOR -> {
                VStruct match = null;
                for (Occurrence current = at; current != null; current = current.parent) {
                    if (current.value().schema() == start.type()) {
                        match = current.value();
                        break;
                    }
                }
                if (match == null) throw new IllegalArgumentException("missing ancestor " + start.type().fullName());
                yield match;
            }
            case TABLE -> throw new IllegalArgumentException("external table is not a source scope");
        };
    }

    private static Value key(List<SimpleValue> values, int offset, int count) {
        return count == 1 ? values.get(offset) : ValueUtil.createList(values.subList(offset, offset + count));
    }

    private static Result missing(ForeignKeySchema fk, VStruct record, String at) {
        if (((RefKey.RefSimple) fk.refKey()).nullable()) return new Result(null, record);
        throw new IllegalArgumentException("target " + fk.pathReference().target().expression() + " not found at " + at);
    }
}

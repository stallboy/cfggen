package configgen.genjava;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Load-time navigation over generated field adapters. Never follows derived references. */
public final class RelationResolver {
    public record Source(String scope, int[] fields) {}
    /** kind: fixed=0, indexed list=1, map=2. */
    public record Step(int field, int kind, int[] keys) {}
    public record Plan(String root, int slot, String name, boolean nullable, Source[] sources,
                       String target, int[] recordKey, Step[] steps) {}
    public record Index(int field, int[] keys) {}
    public record Type(String name, Class<?> javaType, Function<Object, Object[]> fields,
                       Index[] indexes, Plan[] plans, BiConsumer<Object, Object[]> bind) {}
    private record Occurrence(Object value, Object root, String table, Occurrence parent) {}

    private final Map<Class<?>, Type> types = new HashMap<>();
    private final Map<String, Collection<?>> tables = new LinkedHashMap<>();
    private final IdentityHashMap<Object, Object[]> fields = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Map<List<Integer>, Map<List<Object>, Object>>> indexes = new IdentityHashMap<>();
    private final List<Occurrence> occurrences = new ArrayList<>();
    private final LoadValueErrs errors;

    public RelationResolver(LoadValueErrs errors, Type... types) {
        this.errors = errors;
        for (Type type : types) this.types.put(type.javaType(), type);
    }

    public void table(String name, Collection<?> rows) { tables.put(name, rows); }

    public void resolve() {
        tables.forEach((table, rows) -> rows.forEach(row -> walk(row, row, table, null)));
        errors.checkErrors(); // Invalid indexes must never be hidden by nullable lookups.
        for (Occurrence at : occurrences) {
            Type type = types.get(at.value().getClass());
            Object[] references = new Object[Arrays.stream(type.plans()).mapToInt(Plan::slot).max().orElse(-1) + 1];
            for (Plan plan : type.plans()) {
                if (plan.root().equals(at.table())) references[plan.slot()] = bind(at, type, plan);
            }
            type.bind().accept(at.value(), references);
        }
    }

    private Object bind(Occurrence at, Type type, Plan plan) {
        List<Object> keys = new ArrayList<>(plan.sources().length);
        boolean valid = true;
        for (Source source : plan.sources()) {
            Object value = scope(source.scope(), at);
            if (value == null) {
                valid = false;
                continue;
            }
            for (int field : source.fields()) value = read(value)[field];
            keys.add(value);
        }
        boolean external = plan.target().startsWith("table:");
        Object target = external ? tables.get(plan.target().substring(6)) : scope(plan.target(), at);
        if (!valid || target == null) {
            errors.missingReference(type.name(), plan.name(), "scope", at.table());
            return null;
        }
        int used = 0;
        if (external) {
            target = index((Collection<?>) target, plan.recordKey(), plan.target()).get(keys.subList(0, plan.recordKey().length));
            used = plan.recordKey().length;
        }
        for (Step step : plan.steps()) {
            if (target == null) break;
            Object member = read(target)[step.field()];
            target = switch (step.kind()) {
                case 0 -> member;
                case 1 -> index((Collection<?>) member, step.keys(), type.name() + "." + plan.name())
                        .get(keys.subList(used, used + step.keys().length));
                case 2 -> ((Map<?, ?>) member).get(keys.get(used));
                default -> throw new AssertionError(step.kind());
            };
            used += step.kind() == 2 ? 1 : step.keys().length;
        }
        if (target == null && !plan.nullable()) errors.missingReference(type.name(), plan.name(), plan.target(), keys);
        return target;
    }

    private Object scope(String scope, Occurrence at) {
        if (scope.isEmpty()) return at.value();
        if (scope.equals("root")) return at.root();
        for (Occurrence current = at; current != null; current = current.parent()) {
            Object value = current.value();
            if (("ancestor:" + types.get(value.getClass()).name()).equals(scope)) return value;
        }
        return null;
    }

    private Object[] read(Object value) {
        return fields.computeIfAbsent(value, v -> types.get(v.getClass()).fields().apply(v));
    }

    private void walk(Object value, Object root, String table, Occurrence parent) {
        if (value == null) return;
        Type type = types.get(value.getClass());
        if (type != null) {
            Occurrence at = new Occurrence(value, root, table, parent);
            if (type.plans().length != 0) occurrences.add(at);
            Object[] members = read(value);
            for (Index index : type.indexes()) index((Collection<?>) members[index.field()], index.keys(), type.name());
            for (Object member : members) walk(member, root, table, at);
        } else if (value instanceof Collection<?> list) {
            for (Object item : list) walk(item, root, table, parent);
        } else if (value instanceof Map<?, ?> map) {
            map.forEach((key, item) -> { walk(key, root, table, parent); walk(item, root, table, parent); });
        }
    }

    private Map<List<Object>, Object> index(Collection<?> values, int[] keyFields, String name) {
        List<Integer> definition = Arrays.stream(keyFields).boxed().toList();
        return indexes.computeIfAbsent(values, ignored -> new HashMap<>()).computeIfAbsent(definition, ignored -> {
            Map<List<Object>, Object> result = new HashMap<>();
            for (Object value : values) {
                Object[] members = read(value);
                List<Object> key = Arrays.stream(keyFields).mapToObj(i -> members[i]).toList();
                if (result.putIfAbsent(key, value) != null) errors.duplicateKey(name, key);
            }
            return result;
        });
    }
}

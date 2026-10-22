package configgen.schema;

import java.util.*;

import static configgen.schema.FieldType.*;
import static configgen.schema.ReferencePath.Start.*;

/** Resolves reference syntax once, without depending on data or generated languages. */
public final class ReferenceSchema {
    private final CfgSchema cfg;
    private final Map<FieldSchema, KeySchema> indexes = new IdentityHashMap<>();
    private final Map<ForeignKeySchema, Map<TableSchema, ReferencePlan>> plans = new IdentityHashMap<>();
    private final Map<String, Nameable> types = new HashMap<>();
    private final Map<Structural, List<TableSchema>> roots = new IdentityHashMap<>();

    ReferenceSchema(CfgSchema cfg) {
        this.cfg = cfg;
        ForeachSchema.foreachNameable(n -> types.put(n.fullName(), n), cfg);
        for (TableSchema root : cfg.tableMap().values()) {
            for (Nameable included : IncludedStructs.findAllIncludedStructs(root).values()) {
                if (included instanceof Structural structural) {
                    roots.computeIfAbsent(structural, ignored -> new ArrayList<>()).add(root);
                }
            }
        }
    }

    public KeySchema index(FieldSchema field) {
        return indexes.get(field);
    }

    public ReferencePlan plan(ForeignKeySchema fk, TableSchema root) {
        Map<TableSchema, ReferencePlan> byRoot = plans.get(fk);
        return byRoot == null ? null : byRoot.get(root);
    }

    public Collection<ReferencePlan> plans(ForeignKeySchema fk) {
        return List.copyOf(plans.getOrDefault(fk, Map.of()).values());
    }

    public boolean hasPathReferences() {
        return !plans.isEmpty();
    }

    public boolean hasRelations() {
        return hasPathReferences() || !indexes.isEmpty();
    }

    void resolve(CfgSchemaErrs errors) {
        ForeachSchema.foreachStructural(owner -> {
            for (FieldSchema field : owner.fields()) {
                if (field.meta().get("index") != null) {
                    try {
                        resolveIndex(field);
                    } catch (IllegalArgumentException e) {
                        errors.addErr(new CfgSchemaErrs.InvalidIndex(owner.fullName(), field.name(), e.getMessage()));
                    }
                }
            }
        }, cfg);
        if (!errors.errs().isEmpty()) return;
        ForeachSchema.foreachStructural(owner -> {
            for (ForeignKeySchema fk : owner.foreignKeys()) {
                if (fk.pathReference() == null) continue;
                checked(owner, fk.name(), errors, () -> resolveReference(owner, fk));
            }
        }, cfg);
    }

    private static void checked(Structural owner, String name, CfgSchemaErrs errors, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            errors.addErr(new CfgSchemaErrs.InvalidReference(owner.fullName(), name, e.getMessage()));
        }
    }

    private void resolveIndex(FieldSchema field) {
        if (!(field.type() instanceof FList list && list.item() instanceof StructRef ref
                && ref.obj() instanceof StructSchema element)) {
            throw invalid("index requires list<concrete struct>");
        }
        if (!(field.meta().get("index") instanceof Metadata.MetaStr text)) {
            throw invalid("index requires a string of direct field names");
        }
        List<String> names = Arrays.stream(text.value().split(",", -1)).map(String::trim).toList();
        if (names.contains("") || new HashSet<>(names).size() != names.size()) {
            throw invalid("empty or duplicate index field");
        }
        KeySchema key = new KeySchema(names);
        key.setFieldSchemas(names.stream().map(n -> requireField(element, n)).toList());
        validateKey(key.fieldSchemas().stream().map(FieldSchema::type).toList());
        indexes.put(field, key);
    }

    private void resolveReference(Structural owner, ForeignKeySchema fk) {
        Map<TableSchema, ReferencePlan> byRoot = new IdentityHashMap<>();
        for (TableSchema root : roots.getOrDefault(owner, List.of())) {
            byRoot.put(root, compile(owner, root, fk));
        }
        // Unused declarations still get their static types and paths checked.
        if (byRoot.isEmpty()) byRoot.put(null, compile(owner, null, fk));
        Nameable result = byRoot.values().iterator().next().resultType();
        if (byRoot.values().stream().anyMatch(p -> p.resultType() != result)) {
            throw invalid("reference result type differs across root tables");
        }
        fk.setReferenceResultType(result);
        plans.put(fk, byRoot);
    }

    private ReferencePlan compile(Structural owner, TableSchema root, ForeignKeySchema fk) {
        PathReference syntax = fk.pathReference();
        List<ReferencePlan.Source> sources = syntax.sources().stream()
                .map(p -> compileSource(owner, root, p)).toList();
        ReferencePlan.Start start = start(owner, root, syntax.target());
        KeySchema recordKey = null;
        List<FieldType> keys = new ArrayList<>();
        if (start.kind() == TABLE) {
            TableSchema table = (TableSchema) start.type();
            if (fk.refKey() instanceof RefKey.RefUniq unique) {
                recordKey = table.findUniqueKey(unique.key());
                if (recordKey == null) throw invalid("target key is not a declared unique key");
            } else {
                recordKey = table.primaryKey();
            }
            keys.addAll(recordKey.fieldSchemas().stream().map(FieldSchema::type).toList());
            fk.setRefTableSchema(table);
        }
        List<ReferencePlan.Step> steps = new ArrayList<>();
        Nameable current = start.type();
        for (String name : syntax.target().fields()) {
            if (!(current instanceof Structural structural)) throw invalid("cannot traverse an interface");
            FieldSchema field = requireField(structural, name);
            List<FieldType> stepKeys = new ArrayList<>();
            KeySchema listKey = null;
            FieldType element = field.type();
            if (element instanceof FList list) {
                listKey = indexes.get(field);
                if (listKey == null) throw invalid("target list has no unique index: " + name);
                stepKeys.addAll(listKey.fieldSchemas().stream().map(FieldSchema::type).toList());
                element = list.item();
            } else if (element instanceof FMap map) {
                stepKeys.add(map.key());
                element = map.value();
            }
            if (!(element instanceof StructRef ref) || ref.obj() == null) {
                throw invalid("target path must select a struct or interface: " + name);
            }
            steps.add(new ReferencePlan.Step(field, structural.fields().indexOf(field), listKey, List.copyOf(stepKeys)));
            keys.addAll(stepKeys);
            current = ref.obj();
        }
        if (sources.size() != keys.size()) {
            throw invalid("expected " + keys.size() + " source values, got " + sources.size());
        }
        for (int i = 0; i < keys.size(); i++) {
            FieldType actual = sources.get(i).fields().getLast().type();
            if (!KeySchema.typesMatch(actual, keys.get(i))) throw invalid("source key type mismatch at position " + (i + 1));
        }
        return new ReferencePlan(root, List.copyOf(sources), start, recordKey, List.copyOf(steps), current);
    }

    private ReferencePlan.Source compileSource(Structural owner, TableSchema root, ReferencePath path) {
        ReferencePlan.Start start = start(owner, root, path);
        Structural current = start.type();
        List<FieldSchema> fields = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < path.fields().size(); i++) {
            FieldSchema field = requireField(current, path.fields().get(i));
            fields.add(field);
            indices.add(current.fields().indexOf(field));
            if (i + 1 < path.fields().size()) {
                if (!(field.type() instanceof StructRef ref && ref.obj() instanceof StructSchema struct)) {
                    throw invalid("source path can only traverse fixed concrete structs");
                }
                current = struct;
            }
        }
        if (fields.isEmpty()) throw invalid("source path is empty");
        validateKey(List.of(fields.getLast().type()));
        return new ReferencePlan.Source(start, List.copyOf(fields), List.copyOf(indices));
    }

    private ReferencePlan.Start start(Structural owner, TableSchema root, ReferencePath path) {
        Structural type = switch (path.start()) {
            case CURRENT -> owner;
            case ROOT -> {
                if (root == null) throw invalid("root-relative path requires a containing table");
                yield root;
            }
            case ANCESTOR -> {
                Nameable found = findType(owner, path.owner());
                if (!(found instanceof StructSchema struct)) throw invalid("ancestor type is not a concrete struct: " + path.owner());
                yield struct;
            }
            case TABLE -> {
                String namespace = namespace(owner);
                TableSchema table = namespace.isEmpty() ? null : cfg.findTable(namespace + "." + path.owner());
                if (table == null) table = cfg.findTable(path.owner());
                if (table == null) throw invalid("target table not found: " + path.owner());
                yield table;
            }
        };
        return new ReferencePlan.Start(path.start(), type);
    }

    private Nameable findType(Structural owner, String name) {
        if (owner instanceof StructSchema s && s.nullableInterface() != null) {
            Nameable impl = types.get(s.nullableInterface().name() + "." + name);
            if (impl != null) return impl;
        }
        String namespace = namespace(owner);
        Nameable local = namespace.isEmpty() ? null : types.get(namespace + "." + name);
        return local == null ? types.get(name) : local;
    }

    private static String namespace(Structural owner) {
        return owner instanceof StructSchema s && s.nullableInterface() != null
                ? s.nullableInterface().namespace() : owner.namespace();
    }

    private static FieldSchema requireField(Structural owner, String name) {
        FieldSchema field = owner.findField(name);
        if (field == null) throw invalid("field not found: " + owner.fullName() + "." + name);
        return field;
    }

    private static void validateKey(List<FieldType> types) {
        for (FieldType type : types) {
            if (types.size() == 1 ? KeySchema.isKeyType(type) : KeySchema.isPrimitiveKey(type)) continue;
            throw invalid("unsupported key type: " + type);
        }
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}

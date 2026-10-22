package configgen.gen;

import configgen.schema.*;

import java.util.*;

/** Language-neutral, resolved instructions for generated load-time binders. */
public final class Relations {
    public record Source(String scope, List<Integer> fields) {}
    public record Step(int field, int kind, List<Integer> keys) {}
    public record Index(int field, List<Integer> keys) {}
    public record Plan(String root, int slot, ForeignKeySchema reference, List<Source> sources,
                       String target, List<Integer> recordKey, List<Step> steps) {}
    public record Type(Structural owner, List<Index> indexes, List<ForeignKeySchema> references, List<Plan> plans) {}

    private Relations() {}

    public static List<Type> of(CfgSchema schema) {
        List<Type> types = new ArrayList<>();
        ForeachSchema.foreachStructural(owner -> {
            List<Index> indexes = new ArrayList<>();
            for (int i = 0; i < owner.fields().size(); i++) {
                FieldSchema field = owner.fields().get(i);
                KeySchema key = schema.references().index(field);
                if (key != null) indexes.add(new Index(i, listKey(field, key)));
            }
            List<ForeignKeySchema> refs = owner.foreignKeys().stream().filter(f -> f.pathReference() != null).toList();
            List<Plan> plans = new ArrayList<>();
            for (int slot = 0; slot < refs.size(); slot++) {
                ForeignKeySchema fk = refs.get(slot);
                for (ReferencePlan plan : schema.references().plans(fk)) {
                    List<Source> sources = plan.sources().stream().map(s -> new Source(scope(s.start()), s.indices())).toList();
                    List<Step> steps = plan.steps().stream().map(s -> new Step(s.fieldIndex(),
                            s.listKey() != null ? 1 : s.field().type() instanceof FieldType.FMap ? 2 : 0,
                            s.listKey() == null ? List.of() : listKey(s.field(), s.listKey()))).toList();
                    plans.add(new Plan(plan.root() == null ? "" : plan.root().name(), slot, fk, sources,
                            scope(plan.target()), plan.recordKey() == null ? List.of() : indices(plan.target().type(), plan.recordKey()), steps));
                }
            }
            plans.sort(Comparator.comparing(Plan::root).thenComparingInt(Plan::slot));
            types.add(new Type(owner, List.copyOf(indexes), refs, List.copyOf(plans)));
        }, schema);
        types.sort(Comparator.comparing(t -> t.owner().fullName()));
        return List.copyOf(types);
    }

    private static List<Integer> listKey(FieldSchema field, KeySchema key) {
        return indices((Structural) ((FieldType.StructRef) ((FieldType.FList) field.type()).item()).obj(), key);
    }

    private static List<Integer> indices(Structural owner, KeySchema key) {
        return key.fieldSchemas().stream().map(owner.fields()::indexOf).toList();
    }

    private static String scope(ReferencePlan.Start start) {
        return switch (start.kind()) {
            case CURRENT -> "";
            case ROOT -> "root";
            case ANCESTOR -> "ancestor:" + start.type().fullName();
            case TABLE -> "table:" + start.type().fullName();
        };
    }
}

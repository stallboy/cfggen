package configgen.genjava.code;

import configgen.gen.Relations;
import configgen.genjava.GenJavaUtil;
import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

/** Emits field adapters for the shared, resolved navigation instructions. */
final class GenRelations {
    private GenRelations() {}

    static String generate(GenCfg cfg, CfgSchema schema) {
        Relations.Model relations = Relations.forLoader(schema);
        if (relations.types().isEmpty()) return "";
        StringBuilder code = new StringBuilder(
                "    private static void resolveRelations(ConfigMgr mgr, LoadValueErrs errors) {\n"
                + "        var resolver = new RelationResolver(errors);\n");
        for (Relations.Type type : relations.types()) {
            code.append(adapter(cfg, type));
        }
        for (TableSchema table : relations.tables()) {
            String rows = !GenJavaUtil.needsTableData(table)
                    ? "Arrays.asList(" + Name.refType(cfg, table) + ".values())"
                    : "mgr._get_" + new NameableName(cfg, table).containerPrefix + "All()"
                    + (table.primaryKey().fields().size() == 1 && table.primaryKey().fieldSchemas().getFirst().isSeq() ? "" : ".values()");
            code.append("        resolver.table(\"").append(table.name()).append("\", ").append(rows).append(");\n");
        }
        return code.append("        resolver.resolve();\n    }\n").toString();
    }

    private static String adapter(GenCfg cfg, Relations.Type type) {
        Structural owner = type.owner();
        String javaType = owner instanceof TableSchema table && GenJavaUtil.needsTableData(table)
                ? Name.tableDataFullName(cfg, table) : Name.fullName(cfg, owner);
        String fields = owner.fields().stream().map(f -> "row.get" + StringUtil.upper1(f.name()) + "()")
                .collect(Collectors.joining(", "));
        StringBuilder code = new StringBuilder("        resolver.type(\"" + owner.fullName() + "\", " + javaType + ".class,\n"
                + "                row -> new Object[]{" + fields + "})");
        for (Relations.Index index : type.indexes()) {
            code.append("\n                .index(").append(index.field()).append(arguments(index.keys())).append(")");
        }
        for (Relations.Plan plan : type.plans()) {
            ForeignKeySchema fk = plan.reference();
            code.append("\n                .bind(").append(plan(plan)).append(",\n")
                    .append("                        (row, ref) -> row._bind").append(Name.refName(fk)).append("((")
                    .append(Name.refType(cfg, fk)).append(") ref))");
        }
        return code.append(";\n").toString();
    }

    private static String plan(Relations.Plan plan) {
        StringBuilder code = new StringBuilder("new Plan(\"" + plan.root() + "\", \"" + plan.reference().name() + "\", "
                + ((RefKey.RefSimple) plan.reference().refKey()).nullable() + ", \"" + plan.target() + "\")");
        for (Relations.Source source : plan.sources()) {
            code.append(".source(\"").append(source.scope()).append("\"").append(arguments(source.fields())).append(")");
        }
        if (!plan.recordKey().isEmpty()) code.append(".key(").append(ints(plan.recordKey())).append(")");
        for (Relations.Step step : plan.steps()) {
            String method = switch (step.kind()) {
                case 0 -> "field";
                case 1 -> "list";
                case 2 -> "map";
                default -> throw new AssertionError(step.kind());
            };
            code.append(".").append(method).append("(").append(step.field()).append(arguments(step.keys())).append(")");
        }
        return code.toString();
    }

    private static String arguments(List<Integer> values) {
        return values.isEmpty() ? "" : ", " + ints(values);
    }

    private static String ints(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}

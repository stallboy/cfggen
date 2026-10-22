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
        StringBuilder code = new StringBuilder(
                "    private static void resolveRelations(ConfigMgr mgr, LoadValueErrs errors) {\n"
                + "        var resolver = new RelationResolver(errors");
        for (Relations.Type type : Relations.of(schema)) code.append(",\n").append(adapter(cfg, type));
        code.append(");\n");
        for (TableSchema table : schema.tableMap().values()) {
            String rows = !GenJavaUtil.needsTableData(table)
                    ? "java.util.Arrays.asList(" + Name.refType(cfg, table) + ".values())"
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
        String indexes = type.indexes().stream().map(i -> "new RelationResolver.Index(" + i.field() + ", " + ints(i.keys()) + ")")
                .collect(Collectors.joining(", "));
        String plans = type.plans().stream().map(GenRelations::plan).collect(Collectors.joining(",\n"));
        StringBuilder code = new StringBuilder("            new RelationResolver.Type(\"" + owner.fullName() + "\", " + javaType + ".class,\n"
                + "                value -> { var row = (" + javaType + ") value; return new Object[]{" + fields + "}; },\n"
                + "                new RelationResolver.Index[]{" + indexes + "},\n"
                + "                new RelationResolver.Plan[]{" + plans + "},\n"
                + "                (value, refs) -> {\n");
        if (!type.references().isEmpty()) code.append("                    var row = (").append(javaType).append(") value;\n");
        for (int i = 0; i < type.references().size(); i++) {
            ForeignKeySchema fk = type.references().get(i);
            code.append("                    row._bind").append(Name.refName(fk)).append("((")
                    .append(Name.refType(cfg, fk)).append(") refs[").append(i).append("]);\n");
        }
        return code.append("                })").toString();
    }

    private static String plan(Relations.Plan plan) {
        String sources = plan.sources().stream().map(s -> "new RelationResolver.Source(\"" + s.scope() + "\", " + ints(s.fields()) + ")")
                .collect(Collectors.joining(", "));
        String steps = plan.steps().stream().map(s -> "new RelationResolver.Step(" + s.field() + ", " + s.kind() + ", " + ints(s.keys()) + ")")
                .collect(Collectors.joining(", "));
        return "new RelationResolver.Plan(\"" + plan.root() + "\", " + plan.slot() + ", \"" + plan.reference().name() + "\", "
                + ((RefKey.RefSimple) plan.reference().refKey()).nullable() + ", new RelationResolver.Source[]{" + sources
                + "}, \"" + plan.target() + "\", " + ints(plan.recordKey()) + ", new RelationResolver.Step[]{" + steps + "})";
    }

    private static String ints(List<Integer> values) {
        return "new int[]{" + values.stream().map(String::valueOf).collect(Collectors.joining(", ")) + "}";
    }
}

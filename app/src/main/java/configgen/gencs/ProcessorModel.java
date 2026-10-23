package configgen.gencs;

import configgen.gen.Relations;
import configgen.schema.*;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.List;

public class ProcessorModel {
    public final String topPkg;
    public final List<TableSchema> tableSchemas = new ArrayList<>();
    private final CsCodeGenerator gen;
    private final CfgValue cfgValue;
    public final boolean unity;
    public final boolean implicitUsings;
    public final List<Relations.Type> relations;
    public final List<TableSchema> relationTables;
    public final String languageLiterals;
    public final List<Structural> readyStructs = new ArrayList<>();
    public final List<InterfaceSchema> readyInterfaces = new ArrayList<>();

    public ProcessorModel(CsCodeGenerator gen, CfgValue cfgValue, List<String> languages) {
        this.gen = gen;
        this.cfgValue = cfgValue;
        this.topPkg = gen.pkg;
        cfgValue.schema().sortedTables().forEach(tableSchemas::add);
        this.unity = gen.unity;
        this.implicitUsings = gen.implicitUsings;
        var relations = Relations.forLoader(cfgValue.schema());
        this.relations = relations.types();
        this.relationTables = relations.tables();
        this.languageLiterals = languages.stream().map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(", "));
        ForeachSchema.foreachNameable(item -> {
            switch (item) {
                case Structural structural when StructModel.hasNested(structural) -> readyStructs.add(structural);
                case InterfaceSchema union when union.impls().stream().anyMatch(StructModel::hasNested) -> readyInterfaces.add(union);
                default -> { }
            }
        }, cfgValue.schema());
    }

    public String fullName(Nameable schema) {
        return "global::" + topPkg + "." + new Name(gen.pkg, gen.prefix, schema).fullName
                + (schema instanceof TableSchema table && table.entry() instanceof EntryType.EEnum ? "Info" : "");
    }

    public String nsLine() {
        return unity ? "namespace " + topPkg + "\n{" : "namespace " + topPkg + ";";
    }

    public StructModel tableModel(TableSchema table) {
        return new StructModel(gen, table, cfgValue.vTableMap().get(table.name()));
    }

    public String arguments(List<Integer> values) {
        return values.isEmpty() ? "" : ", " + ints(values);
    }

    private String ints(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
    }

    public String relationPlan(Relations.Plan plan) {
        StringBuilder code = new StringBuilder("new RelationResolver.Plan(\"" + plan.root() + "\", \"" + plan.reference().name() + "\", "
                + ((RefKey.RefSimple) plan.reference().refKey()).nullable() + ", \"" + plan.target() + "\")");
        for (Relations.Source source : plan.sources()) {
            code.append(".Source(\"").append(source.scope()).append("\"").append(arguments(source.fields())).append(")");
        }
        if (!plan.recordKey().isEmpty()) code.append(".Key(").append(ints(plan.recordKey())).append(")");
        for (Relations.Step step : plan.steps()) {
            String method = switch (step.kind()) {
                case 0 -> "Field";
                case 1 -> "List";
                case 2 -> "Map";
                default -> throw new AssertionError(step.kind());
            };
            code.append(".").append(method).append("(").append(step.field()).append(arguments(step.keys())).append(")");
        }
        return code.toString();
    }

    public String relationField(FieldSchema field) {
        String value = "row." + configgen.util.StringUtil.upper1(field.name());
        return field.type() instanceof FieldType.FMap ? "RelationResolver.WrapMap(" + value + ")" : value;
    }
}

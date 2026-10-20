package configgen.gencs;

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
    public final String schemaBase64;
    public final String languageLiterals;
    public final List<Structural> readyStructs = new ArrayList<>();
    public final List<InterfaceSchema> readyInterfaces = new ArrayList<>();

    public ProcessorModel(CsCodeGenerator gen, CfgValue cfgValue, String schemaBase64, List<String> languages) {
        this.gen = gen;
        this.cfgValue = cfgValue;
        this.topPkg = gen.pkg;
        cfgValue.schema().sortedTables().forEach(tableSchemas::add);
        this.unity = gen.unity;
        this.implicitUsings = gen.implicitUsings;
        this.schemaBase64 = schemaBase64;
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
        return new Name(gen.pkg, gen.prefix, schema).fullName
                + (schema instanceof TableSchema table && table.entry() instanceof EntryType.EEnum ? "Info" : "");
    }

    public String nsLine() {
        return unity ? "namespace " + topPkg + "\n{" : "namespace " + topPkg + ";";
    }

    public StructModel tableModel(TableSchema table) {
        return new StructModel(gen, table, cfgValue.vTableMap().get(table.name()));
    }
}

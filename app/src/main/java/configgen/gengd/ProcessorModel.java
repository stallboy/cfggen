package configgen.gengd;

import configgen.schema.*;
import configgen.value.CfgValue;

import java.util.*;

public class ProcessorModel {
    public final Iterable<TableSchema> tableSchemas;
    public final String relationClass;
    private final GdCodeGenerator gen;
    private final CfgValue value;

    public ProcessorModel(GdCodeGenerator gen, CfgValue value) {
        this.gen = gen;
        this.value = value;
        this.tableSchemas = value.schema().sortedTables();
        Set<String> classNames = new HashSet<>();
        ForeachSchema.foreachNameable(type -> classNames.add(fullName(type)), value.schema());
        String name = "_ObjectRelations";
        while (classNames.contains(name)) name += "_";
        relationClass = name;
    }

    public String fullName(Nameable nameable) {
        return new Name(gen.prefix, nameable).className;
    }

    public String languageArguments() {
        return gen.languageNames.stream().map(name -> "\"" + name + "\"").collect(java.util.stream.Collectors.joining(", "));
    }

    public boolean hasRelations() { return gen.hasRelations; }
    public List<configgen.gen.Relations.Type> relations() { return configgen.gen.Relations.of(value.schema()); }
    public String fields(Structural owner) { return owner.fields().stream().map(f -> "\"" + configgen.util.StringUtil.lower1(f.name()) + "\"").collect(java.util.stream.Collectors.joining(", ")); }
    public List<String> stateFields(TableSchema table) {
        List<String> fields = new ArrayList<>();
        fields.add(fullName(table) + "._data");
        for (KeySchema key : table.uniqueKeys()) fields.add(fullName(table) + "." + configgen.naming.GenNaming.uniqueKeyMapNameSnake(key));
        if (table.entry() instanceof EntryType.EntryBase) {
            for (String name : value.getTable(table.name()).enumNames()) fields.add(fullName(table) + "." + configgen.util.StringUtil.upper1(name));
        }
        return fields;
    }
}

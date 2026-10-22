package configgen.gengo;

import configgen.value.CfgValue;

public record CfgMgrModel(String pkg,
                          CfgValue cfgValue, java.util.List<String> languages) {
    public String languageArguments() {
        return languages.stream().map(name -> "\"" + name + "\"").collect(java.util.stream.Collectors.joining(", "));
    }

    public boolean hasRelations() { return cfgValue.schema().references().hasRelations(); }
    public java.util.List<configgen.gen.Relations.Type> relations() { return configgen.gen.Relations.of(cfgValue.schema()); }
    public String ints(java.util.List<Integer> values) { return "[]int{" + values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", ")) + "}"; }
    public String relationField(configgen.schema.FieldSchema field) {
        String value = "row." + configgen.util.StringUtil.lower1(field.name());
        return switch (field.type()) {
            case configgen.schema.FieldType.FList _ -> "func() any { list := &listView{}; for _, item := range " + value + " { list.rows = append(list.rows, item) }; return list }()";
            case configgen.schema.FieldType.FMap _ -> "func() any { view := &mapView{}; for key, item := range " + value + " { view.entries = append(view.entries, [2]any{key, item}) }; return view }()";
            default -> value;
        };
    }
}

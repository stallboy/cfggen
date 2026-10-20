package configgen.genjava.code;

import configgen.i18n.LangSwitchable;
import configgen.genjava.*;
import configgen.util.CachedIndentPrinter;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GenConfigCodeSchema {
    static void generateAll(JavaCodeGenerator gen, GenCfg cfg, int schemaNumPerFile, CfgValue cfgValue, LangSwitchable ls) {
        SchemaInterface schemaInterface = SchemaParser.parsePackage(cfgValue, ls);
        Set<String> packageOnly = new HashSet<>();
        for (var table : cfgValue.tables()) {
            if (GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(table.schema())) {
                packageOnly.add(table.name() + "_Detail");
            }
        }
        List<Map.Entry<String, Schema>> all = new ArrayList<>(schemaInterface.implementations.entrySet());
        List<Map.Entry<String, Schema>> main;
        List<List<Map.Entry<String, Schema>>> others = List.of();
        if (schemaNumPerFile == -1 || all.size() <= schemaNumPerFile) {
            main = all;
        } else {
            main = all.subList(0, schemaNumPerFile);
            int left = all.size() - schemaNumPerFile;
            int separateFileNum = (left + schemaNumPerFile - 1) / schemaNumPerFile;

            others = new ArrayList<>();
            for (int i = 0; i < separateFileNum; i++) {
                int start = (i + 1) * schemaNumPerFile;
                int end = Math.min(start + schemaNumPerFile, all.size());
                others.add(all.subList(start, end));
            }
        }

        generateFile(gen, cfg, 0, main, others, packageOnly);
        for (int i = 0; i < others.size(); i++) {
            generateFile(gen, cfg, i + 1, others.get(i), List.of(), packageOnly);
        }
    }

    private static String getClassName(int idx) {
        return idx > 0 ? "ConfigCodeSchema" + idx : "ConfigCodeSchema";
    }

    private static void generateFile(JavaCodeGenerator gen, GenCfg cfg, int idx,
                             List<Map.Entry<String, Schema>> schemas,
                             List<List<Map.Entry<String, Schema>>> others, Set<String> packageOnly) {

        String className = getClassName(idx);
        try (CachedIndentPrinter ps = gen.createCode(className + ".java")) {
            ps.println("package %s;", cfg.codeTopPkg());
            ps.println();
            ps.println("import configgen.genjava.*;");
            ps.println();

            ps.println("public class %s {", className);
            ps.println();

            if (idx == 0) {
                printMain(schemas, others, ps, packageOnly);
            }
            print(schemas, ps);
            ps.println("}");
        }
    }

    private static void printMain(List<Map.Entry<String, Schema>> main,
                                  List<List<Map.Entry<String, Schema>>> others,
                                  CachedIndentPrinter ip, Set<String> packageOnly) {

        ip.println1("public static SchemaInterface getCodeSchema() { return createSchema(false); }");
        ip.println1("public static SchemaInterface getPackageSchema() { return createSchema(true); }");
        ip.println1("private static SchemaInterface createSchema(boolean packageLayout) {");
        ip.inc();
        ip.inc();

        ip.println("SchemaInterface schema = new SchemaInterface();");
        for (int i = 0; i <= others.size(); i++) {
            var schemas = i == 0 ? main : others.get(i - 1);
            String prefix = i == 0 ? "" : getClassName(i) + ".";
            for (var entry : schemas) {
                String key = entry.getKey();
                boolean conditional = packageOnly.contains(key);
                if (conditional) {
                    ip.println("if (packageLayout) {");
                    ip.inc();
                }
                ip.println("schema.addImp(\"%s\", %s%s());", key, prefix, key.replace('.', '_'));
                if (conditional) {
                    ip.dec();
                    ip.println("}");
                }
            }
        }

        ip.dec();
        ip.dec();
        ip.println2("return schema;");
        ip.println1("}");
        ip.println();
    }


    private static void print(List<Map.Entry<String, Schema>> schemas,
                              CachedIndentPrinter ip) {

        for (Map.Entry<String, Schema> stringSchemaEntry : schemas) {
            String key = stringSchemaEntry.getKey();
            String func = key.replace('.', '_');

            ip.println1("static Schema %s() {", func);
            ip.inc();
            ip.inc();

            String name = "s" + ip.indent();
            printSchema(ip, stringSchemaEntry.getValue());

            ip.dec();
            ip.dec();
            ip.println2("return %s;", name);
            ip.println1("}");
            ip.println();
        }
    }

    private static void printSchema(CachedIndentPrinter ip, Schema schema) {
        switch (schema) {
            case SchemaBean schemaBean -> {
                String name = "s" + ip.indent();
                ip.println("SchemaBean %s = new SchemaBean(%s);", name, Boolean.toString(schemaBean.isTable));
                for (SchemaBean.Column column : schemaBean.columns) {
                    ip.println("%s.addColumn(\"%s\", %s);", name, column.name(), parse(column.schema()));
                }
            }
            case SchemaEnum schemaEnum -> {
                String name = "s" + ip.indent();
                ip.println("SchemaEnum %s = new SchemaEnum(%s, %s);", name, Boolean.toString(schemaEnum.isEnumPart),
                        Boolean.toString(schemaEnum.hasIntValue));
                for (Map.Entry<String, Integer> entry : schemaEnum.values.entrySet()) {
                    if (schemaEnum.hasIntValue) {
                        ip.println("%s.addValue(\"%s\", %d);", name, entry.getKey(), entry.getValue());
                    } else {
                        ip.println("%s.addValue(\"%s\");", name, entry.getKey());
                    }
                }
            }
            case SchemaInterface schemaInterface -> {
                String name = "s" + ip.indent();
                ip.println("SchemaInterface %s = new SchemaInterface();", name);
                for (Map.Entry<String, Schema> stringSchemaEntry : schemaInterface.implementations.entrySet()) {
                    ip.println("{");
                    ip.inc();
                    String subName = "s" + ip.indent();
                    printSchema(ip, stringSchemaEntry.getValue());
                    ip.println("%s.addImp(\"%s\", %s);", name, stringSchemaEntry.getKey(), subName);
                    ip.dec();
                    ip.println("}");
                }
            }
            default -> throw new IllegalStateException();
        }
    }

    private static String parse(Schema schema) {
        return switch (schema) {
            case SchemaList list -> "new SchemaList(" + parse(list.ele()) + ")";
            case SchemaMap map -> "new SchemaMap(" + parse(map.key()) + ", " + parse(map.value()) + ")";
            case SchemaPrimitive primitive -> "SchemaPrimitive." + primitive.name();
            case SchemaRef ref -> "new SchemaRef(\"" + ref.type + "\")";
            default -> throw new IllegalStateException();
        };
    }
}




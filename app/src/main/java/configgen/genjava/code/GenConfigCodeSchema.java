package configgen.genjava.code;

import configgen.i18n.LangSwitchable;
import configgen.genjava.*;
import configgen.util.CachedIndentPrinter;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class GenConfigCodeSchema {
    static void generateAll(GenJavaCode gen, int schemaNumPerFile, CfgValue cfgValue, LangSwitchable ls) {
        SchemaInterface schemaInterface = SchemaParser.parse(cfgValue, ls);
        List<Map.Entry<String, Schema>> all = new ArrayList<>(schemaInterface.implementations.entrySet());
        List<Map.Entry<String, Schema>> main;
        List<List<Map.Entry<String, Schema>>> nullableOthers = null;
        if (schemaNumPerFile == -1 || all.size() <= schemaNumPerFile) {
            main = all;
        } else {
            main = all.subList(0, schemaNumPerFile);
            int left = all.size() - schemaNumPerFile;
            int seperateFileNum = (left + schemaNumPerFile - 1) / schemaNumPerFile;

            nullableOthers = new ArrayList<>();
            for (int i = 0; i < seperateFileNum; i++) {
                int start = (i + 1) * schemaNumPerFile;
                int end = start + schemaNumPerFile;
                if (end > all.size()) {
                    end = all.size();
                }
                nullableOthers.add(all.subList(start, end));
            }
        }

        generateFile(gen, 0, main, nullableOthers);
        if (nullableOthers != null) {
            int idx = 0;
            for (List<Map.Entry<String, Schema>> nullableOther : nullableOthers) {
                idx++;
                generateFile(gen, idx, nullableOther, null);
            }
        }
    }

    static String getClassName(int idx) {
        String className = "ConfigCodeSchema";
        if (idx > 0) {
            className = String.format("%s%d", className, idx);
        }

        return className;
    }

    static void generateFile(GenJavaCode gen, int idx,
                             List<Map.Entry<String, Schema>> schemas,
                             List<List<Map.Entry<String, Schema>>> nullableOthers) {

        String className = getClassName(idx);
        try (CachedIndentPrinter ps = gen.createCodeFile(className + ".java")) {
            ps.println("package %s;", Name.codeTopPkg);
            ps.println();
            ps.println("import configgen.genjava.*;");
            ps.println();

            ps.println("public class %s {", className);
            ps.println();

            if (idx == 0) {
                printMain(schemas, nullableOthers, ps);
                print(schemas, ps);
            } else {
                print(schemas, ps);
            }
            ps.println("}");
        }
    }

    private static void printMain(List<Map.Entry<String, Schema>> main,
                                  List<List<Map.Entry<String, Schema>>> nullableOthers,
                                  CachedIndentPrinter ip) {

        ip.println1("public static Schema getCodeSchema() {");
        ip.inc();
        ip.inc();

        String name = "schema";
        ip.println("SchemaInterface %s = new SchemaInterface();", name);

        for (Map.Entry<String, Schema> stringSchemaEntry : main) {
            String key = stringSchemaEntry.getKey();
            String func = key.replace('.', '_');
            ip.println("%s.addImp(\"%s\", %s());", name, key, func);
        }

        if (nullableOthers != null) {
            int idx = 0;
            for (List<Map.Entry<String, Schema>> other : nullableOthers) {
                idx++;
                for (Map.Entry<String, Schema> stringSchemaEntry : other) {
                    String key = stringSchemaEntry.getKey();
                    String func = key.replace('.', '_');
                    ip.println("%s.addImp(\"%s\", %s.%s());", name, key, getClassName(idx), func);
                }
            }
        }

        ip.dec();
        ip.dec();
        ip.println2("return %s;", name);
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
                ip.println("SchemaBean %s = new SchemaBean(%s);", name, schemaBean.isTable ? "true" : "false");
                for (SchemaBean.Column column : schemaBean.columns) {
                    ip.println("%s.addColumn(\"%s\", %s);", name, column.name(), parse(column.schema()));
                }
            }
            case SchemaEnum schemaEnum -> {
                String name = "s" + ip.indent();
                ip.println("SchemaEnum %s = new SchemaEnum(%s, %s);", name, schemaEnum.isEnumPart ? "true" : "false",
                        schemaEnum.hasIntValue ? "true" : "false");
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
            default -> {
                throw new IllegalStateException();
            }
        }
    }

    private static String parse(Schema schema) {
        switch (schema) {
            case SchemaBean ignored -> {
                throw new IllegalStateException();
            }
            case SchemaEnum ignored -> {
                throw new IllegalStateException();
            }
            case SchemaInterface ignored -> {
                throw new IllegalStateException();
            }
            case SchemaList schemaList -> {
                return "new SchemaList(" + parse(schemaList.ele()) + ")";
            }
            case SchemaMap schemaMap -> {
                return "new SchemaMap(" + parse(schemaMap.key()) + ", " + parse(schemaMap.value()) + ")";
            }
            case SchemaPrimitive schemaPrimitive -> {
                return "SchemaPrimitive." + schemaPrimitive.name();
            }
            case SchemaRef schemaRef -> {
                return "new SchemaRef(\"" + schemaRef.type + "\")";
            }
        }
    }
}




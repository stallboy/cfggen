package configgen.genjava.code;

import configgen.gen.Generator;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static configgen.gen.Generator.lower1;
import static configgen.value.CfgValue.VTable;

class GenStructuralClassTablePart {

    static List<String> mapsInMgr = new ArrayList<>();

    static void generate(Structural structural, VTable vTable, boolean isTableAndNeedBuilder,
                         NameableName name, CachedIndentPrinter ps) {
        TableSchema table = vTable.schema();
        // static get
        generateMapGetBy(table.primaryKey(), name, ps, true);

        // static getByXxx
        for (KeySchema uk : table.uniqueKeys()) {
            generateMapGetBy(uk, name, ps, false);
        }

        // static all
        String primaryMapName = name.containerPrefix + "All";
        String functionAllName = "all" + Arrays.stream(structural.name().split("\\.")).map(Generator::upper1).collect(Collectors.joining());
        mapsInMgr.add(String.format("public java.util.Collection<%s> %s() { return %s.values(); }",
                name.fullName, functionAllName, primaryMapName));

        ps.println1("public static java.util.Collection<" + name.className + "> all() {");
        ps.println2("%s.ConfigMgr mgr = %s.ConfigMgr.getMgr();", Name.codeTopPkg, Name.codeTopPkg);
        ps.println2("return mgr.%s();", functionAllName);
        ps.println1("}");
        ps.println();


        // add _ConfigLoader class
        ps.println1("public static class _ConfigLoader implements %s.ConfigLoader {", Name.codeTopPkg);
        ps.println();

        // override createAll
        ps.println2("@Override");
        ps.println2("public void createAll(%s.ConfigMgr mgr, configgen.genjava.ConfigInput input) {", Name.codeTopPkg);
        ps.println3("for (int c = input.readInt(); c > 0; c--) {");
        ps.println4("%s self = %s._create(input);", name.className, name.className);
        generateAllMapPut(table, name, ps);
        ps.println3("}");
        ps.println2("}");
        ps.println();

        //static _resolveAll
        ps.println2("@Override");
        ps.println2("public void resolveAll(%s.ConfigMgr mgr) {", Name.codeTopPkg);
        if (HasRef.hasRef(structural) && !isTableAndNeedBuilder) {
            ps.println3("for (%s e : mgr.%sAll.values()) {", name.className, name.containerPrefix);
            ps.println4("e._resolve(mgr);");
            ps.println3("}");
        } else {
            ps.println3("// no resolve");
        }
        ps.println2("}");
        ps.println();

        ps.println1("}");
        ps.println();
    }


    private static void generateMapGetBy(KeySchema keySchema, NameableName name, CachedIndentPrinter ps, boolean isPrimaryKey) {
        if (keySchema.fields().size() > 1) {
            generateKeyClass(keySchema, ps);
        }

        String mapName = name.containerPrefix + (isPrimaryKey ? "All" : Name.uniqueKeyMapName(keySchema));
        String keyTypeName = Name.keyClassName(keySchema, name);

        mapsInMgr.add(String.format("public final java.util.Map<%s, %s> %s = new java.util.LinkedHashMap<>();", keyTypeName, name.fullName, mapName));

        var methodName = Name.GetByKeyFunctionNameInConfigMgr(keySchema, isPrimaryKey, name.nameable);
        mapsInMgr.add(String.format("public %s %s(%s) { return %s.get(%s); }", name.fullName, methodName,
                MethodStr.formalParams(keySchema.fieldSchemas()), mapName, MethodStr.actualParamsKey(keySchema, "", name)));

        String getByName = Name.GetByKeyFunctionName(keySchema, isPrimaryKey);
        ps.println1("public static " + name.className + " " + getByName + "(" + MethodStr.formalParams(keySchema.fieldSchemas()) + ") {");
        ps.println2("%s.ConfigMgr mgr = %s.ConfigMgr.getMgr();", Name.codeTopPkg, Name.codeTopPkg);
        ps.println2("return mgr." + methodName + "(" + MethodStr.actualParamsKeyRaw(keySchema, "") + ");");
        ps.println1("}");
        ps.println();
    }


    private static void generateAllMapPut(TableSchema table, NameableName name, CachedIndentPrinter ps) {
        generateMapPut(table.primaryKey(), name, ps, true);
        for (KeySchema uk : table.uniqueKeys()) {
            generateMapPut(uk, name, ps, false);
        }
    }

    private static void generateMapPut(KeySchema keySchema, NameableName name, CachedIndentPrinter ps, boolean isPrimaryKey) {
        String mapName = name.containerPrefix + (isPrimaryKey ? "All" : Name.uniqueKeyMapName(keySchema));
        ps.println4("mgr." + mapName + ".put(" + MethodStr.actualParamsKey(keySchema, "self.", null) + ", self);");
    }

    private static void generateKeyClass(KeySchema keySchema, CachedIndentPrinter ps) {
        String keyClassName = Name.keyClassName(keySchema);
        //static Key class
        ps.println1("public static class " + keyClassName + " {");
        for (FieldSchema f : keySchema.fieldSchemas()) {
            ps.println2("private final " + TypeStr.type(f.type()) + " " + lower1(f.name()) + ";");
        }
        ps.println();

        ps.println2("public " + keyClassName + "(" + MethodStr.formalParams(keySchema.fieldSchemas()) + ") {");
        for (FieldSchema f : keySchema.fieldSchemas()) {
            ps.println3("this." + lower1(f.name()) + " = " + lower1(f.name()) + ";");
        }
        ps.println2("}");
        ps.println();

        ps.println2("@Override");
        ps.println2("public int hashCode() {");
        ps.println3("return " + MethodStr.hashCodes(keySchema.fieldSchemas()) + ";");
        ps.println2("}");
        ps.println();

        ps.println2("@Override");
        ps.println2("public boolean equals(Object other) {");
        ps.println3("if (!(other instanceof " + keyClassName + "))");
        ps.println4("return false;");
        ps.println3(keyClassName + " o = (" + keyClassName + ") other;");
        ps.println3("return " + MethodStr.equals(keySchema.fieldSchemas()) + ";");
        ps.println2("}");

        ps.println1("}");
        ps.println();
    }


    static void generateTableBuilder(TableSchema table, NameableName name, CachedIndentPrinter ps) {
        ps.println("package %s;", name.pkg);
        ps.println();

        ps.println("public class %sBuilder {", name.className);
        // field, public，并且都不设置默认值，如果要做，可以自己来包装
        for (FieldSchema field : table.fields()) {
            ps.println1("public %s %s;", TypeStr.type(field.type()), lower1(field.name()));
        }
        ps.println();

        // build
        ps.println1("public %s build() {", name.className);
        for (FieldSchema field : table.fields()) {
            if (!TypeStr.isJavaPrimitive(field.type())) {
                String fn = lower1(field.name());
                if (field.type() instanceof FieldType.StructRef) {
                    ps.println2("java.util.Objects.requireNonNull(%s);", fn);
                } else {
                    ps.println2("if (%s == null) {", fn);
                    ps.println3("%s = %s;", fn, TypeStr.defaultValue(field.type()));
                    ps.println2("}");
                }
            }
        }
        ps.println2("return new %s(this);", name.className);
        ps.println1("}");
        ps.println();
        ps.println("}");
    }

    static void generateTableBuild(Structural struct, NameableName name, CachedIndentPrinter ps) {
        // package访问级别，通过builder.build()来构建
        ps.println1("%s(%sBuilder b) {", name.className, name.className);
        for (FieldSchema field : struct.fields()) {
            String ln = lower1(field.name());
            ps.println2("this.%s = b.%s;", ln, ln);
        }
        ps.println1("}");
        ps.println();
    }
}

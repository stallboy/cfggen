package configgen.gencs;

import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.value.CfgValue;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.VTable;

public class GenCs extends Generator {
    private final String dir;
    private final String pkg;
    private final String encoding;
    private final String prefix;
    private File dstDir;
    private CfgSchema cfgSchema;
    private boolean isLangSwitch;

    public GenCs(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "Config");
        pkg = parameter.get("pkg", "Config");
        encoding = parameter.get("encoding", "GBK");
        prefix = parameter.get("prefix", "Data");
        parameter.end();
    }

    @Override
    public void generate(Context ctx) throws IOException {
        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/')).toFile();
        CfgValue cfgValue = ctx.makeValue(tag);  // 这里只需要schema，生成value只用于检验数据
        cfgSchema = cfgValue.schema();

        isLangSwitch = ctx.nullableLangSwitch() != null;
        //copyFile("CSV.cs");
        //copyFile("CSVLoader.cs");
        //copyFile("LoadErrors.cs");
        //copyFile("KeyedList.cs");
        genCSVProcessor();

        for (Fieldable fieldable : cfgSchema.sortedFieldables()) {
            switch (fieldable) {
                case StructSchema structSchema -> {
                    generateStructClass(structSchema, null);
                }
                case InterfaceSchema interfaceSchema -> {
                    generateInterface(interfaceSchema);
                    for (StructSchema impl : interfaceSchema.impls()) {
                        generateStructClass(impl, null);
                    }
                }
            }
        }

        for (VTable vTable : cfgValue.sortedTables()) {
            generateStructClass(vTable.schema(), vTable);
        }

        if (isLangSwitch) { //生成Text这个Bean
            try (CachedIndentPrinter ps = createCode(new File(dstDir, "Text.cs"), encoding)) {
                GenText.generate(ctx.nullableLangSwitch(), pkg, ps);
            }
        }


        CachedFiles.keepMetaAndDeleteOtherFiles(dstDir);
    }

    private static class Name {
        final String pkg;
        final String className;
        final String fullName;
        final String path;

        Name(String topPkg, String prefix, Nameable nameable) {
            String name;
            InterfaceSchema nullableInterface = nameable instanceof StructSchema struct ? struct.nullableInterface() : null;
            if (nullableInterface != null) {
                name = nullableInterface.name().toLowerCase() + "." + nameable.name();
            } else {
                name = nameable.name();
            }
            String[] seps = name.split("\\.");
            String[] pks = new String[seps.length - 1];
            for (int i = 0; i < pks.length; i++)
                pks[i] = upper1Only(seps[i]);
            className = prefix + upper1Only(seps[seps.length - 1]);

            if (pks.length == 0)
                pkg = topPkg;
            else
                pkg = topPkg + "." + String.join(".", pks);

            if (pkg.isEmpty())
                fullName = className;
            else
                fullName = pkg + "." + className;

            if (pks.length == 0)
                path = className + ".cs";
            else
                path = String.join("/", pks) + "/" + className + ".cs";
        }
    }

    private void generateStructClass(Structural structural, VTable vTable) {
        Name name = new Name(pkg, prefix, structural);
        File csFile = dstDir.toPath().resolve(name.path).toFile();
        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
            generateStructClass(structural, vTable, name, ps);
        }
    }

    private void generateInterface(InterfaceSchema sInterface) {
        Name name = new Name(pkg, prefix, sInterface);
        File csFile = dstDir.toPath().resolve(name.path).toFile();
        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
            generateInterface(sInterface, name, ps);
        }
    }

    private void generateInterface(InterfaceSchema sInterface, Name name, CachedIndentPrinter ps) {
        ps.println("using System;");
        ps.println("using System.Collections.Generic;");
        if (!pkg.equals("Config")) {
            ps.println("using Config;");
        }
        ps.println("namespace " + name.pkg);
        ps.println("{");
        ps.println("public abstract class " + name.className);
        ps.println("{");
        if (sInterface.nullableEnumRefTable() != null) {
            ps.println1("public abstract " + fullName(sInterface.nullableEnumRefTable()) + " type();");
            ps.println();
        }

        if (HasRef.hasRef(sInterface)) {
            ps.println1("internal virtual void _resolve(Config.LoadErrors errors)");
            ps.println1("{");
            ps.println1("}");
            ps.println();
        }

        ps.println1("internal static " + name.className + " _create(Config.Stream os) {");
        ps.println2("switch(os.ReadString()) {");
        for (StructSchema impl : sInterface.impls()) {
            ps.println3("case \"" + impl.name() + "\":");
            ps.println4("return " + fullName(impl) + "._create(os);");
        }

        ps.println2("}");
        ps.println2("return null;");
        ps.println1("}");
        ps.println("}");
        ps.println("}");
    }

    private void generateStructClass(Structural structural, VTable vTable, Name name, CachedIndentPrinter ps) {
        TableSchema table = vTable != null ? vTable.schema() : null;
        ps.println("using System;");
        ps.println("using System.Collections.Generic;");

        if (!pkg.equals("Config")) {
            ps.println("using Config;");
        }
        ps.println();

        ps.println("namespace " + name.pkg);
        ps.println("{");

        InterfaceSchema nullableInterface = structural instanceof StructSchema struct ? struct.nullableInterface() : null;
        boolean isImpl = nullableInterface != null;
        boolean hasNoFields = structural.fields().isEmpty();

        if (isImpl) {
            ps.println1("public partial class " + name.className + " : " + fullName(nullableInterface));
            ps.println1("{");

            TableSchema enumRefTable = nullableInterface.nullableEnumRefTable();
            if (enumRefTable != null) {
                ps.println2("public override " + fullName(enumRefTable) + " type() {");
                ps.println3("return " + fullName(enumRefTable) + "." + structural.name() + ";");
                ps.println2("}");
                ps.println();
            }
        } else {
            ps.println1("public partial class " + name.className);
            ps.println1("{");
        }


        boolean isTableEnumOrEntry = (table != null && table.entry() instanceof EntryType.EntryBase);
        // static enum
        if (isTableEnumOrEntry) {
            for (String e : vTable.enumNames()) {
                ps.println2("public static " + name.className + " " + upper1(e) + " { get; private set; }");
            }
            ps.println();
        }

        // field property
        for (FieldSchema fieldSchema : structural.fields()) {
            String c = fieldSchema.comment().isEmpty() ? "" : " /* " + fieldSchema.comment() + "*/";
            ps.println2("public " + type(fieldSchema.type()) + " " + upper1(fieldSchema.name()) + " { get; private set; }" + c);
        }

        // ref property
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            ps.println2("public " + refType(fk) + " " + refName(fk) + " { get; private set; }");
        }
        ps.println();

        //constructor
        if (table == null) {
            ps.println2("public " + name.className + "() {");
            ps.println2("}");
            ps.println();

            if (!hasNoFields) { //避免重复
                ps.println2("public " + name.className + "(" + formalParams(structural.fields()) + ") {");
                for (FieldSchema f : structural.fields()) {
                    ps.println3("this." + upper1(f.name()) + " = " + lower1(f.name()) + ";");
                }
                ps.println2("}");
                ps.println();
            }
        }

        List<FieldSchema> keys = table != null ? table.primaryKey().fieldSchemas() : structural.fields();

        if (hasNoFields) {
            //hash
            ps.println2("public override int GetHashCode()");
            ps.println2("{");
            ps.println3("return this.GetType().GetHashCode();");
            ps.println2("}");
            ps.println();

            //equal
            ps.println2("public override bool Equals(object obj)");
            ps.println2("{");
            ps.println3("if (obj == null) return false;");
            ps.println3("if (obj == this) return true;");
            ps.println3("var o = obj as " + name.className + ";");
            ps.println3("return o != null;");
            ps.println2("}");
            ps.println();
        } else {
            //hash
            ps.println2("public override int GetHashCode()");
            ps.println2("{");
            ps.println3("return " + hashCodes(keys) + ";");
            ps.println2("}");
            ps.println();

            //equal
            ps.println2("public override bool Equals(object obj)");
            ps.println2("{");
            ps.println3("if (obj == null) return false;");
            ps.println3("if (obj == this) return true;");
            ps.println3("var o = obj as " + name.className + ";");
            ps.println3("return o != null && " + equals(keys) + ";");
            ps.println2("}");
            ps.println();

            //toString
            ps.println2("public override string ToString()");
            ps.println2("{");
            ps.println3("return \"(\" + " + toStrings(structural.fields()) + " + \")\";");
            ps.println2("}");
            ps.println();
        }


        String csv = "\"" + structural.name() + "\"";
        if (table != null) {
            generateMapGetBy(table.primaryKey(), name, ps, true);
            for (KeySchema uk : table.uniqueKeys()) {
                generateMapGetBy(uk, name, ps, false);
            }

            //static all
            ps.println2("public static List<" + name.className + "> All()");
            ps.println2("{");
            ps.println3("return all.OrderedValues;");
            ps.println2("}");
            ps.println();

            // static filter
            ps.println2("public static List<" + name.className + "> Filter(Predicate<" + name.className + "> predicate)");
            ps.println2("{");
            ps.println3("var r = new List<" + name.className + ">();");
            ps.println3("foreach (var e in all.OrderedValues)");
            ps.println3("{");
            ps.println4("if (predicate(e))");
            ps.println5("r.Add(e);");
            ps.println3("}");
            ps.println3("return r;");
            ps.println2("}");
            ps.println();

            //static initialize
            ps.println2("internal static void Initialize(Config.Stream os, Config.LoadErrors errors)");
            ps.println2("{");
            ps.println3("all = new Config.KeyedList<" + keyClassName(table.primaryKey()) + ", " + name.className + ">();");
            for (KeySchema uk : table.uniqueKeys()) {
                ps.println3(uniqueKeyMapName(uk) + " = new Config.KeyedList<" + keyClassName(uk) + ", " + name.className + ">();");
            }

            ps.println3("for (var c = os.ReadInt32(); c > 0; c--) {");
            ps.println4("var self = _create(os);");
            generateAllMapPut(table, ps);

            if (table.entry() instanceof EntryType.EntryBase entryBase) {
                String ef = upper1(entryBase.field());
                ps.println4("if (self." + ef + ".Trim().Length == 0)");
                ps.println5("continue;");
                ps.println4("switch(self." + ef + ".Trim())");
                ps.println4("{");
                for (String e : vTable.enumNames()) {
                    ps.println5("case \"" + e + "\":");
                    ps.println6("if (" + upper1(e) + " != null)");
                    ps.println7("errors.EnumDup(" + csv + ", self.ToString());");
                    ps.println6(upper1(e) + " = self;");
                    ps.println6("break;");
                }
                ps.println5("default:");
                ps.println6("errors.EnumDataAdd(" + csv + ", self.ToString());");
                ps.println6("break;");
                ps.println4("}");
            }
            ps.println3("}");

            if (table.entry() instanceof EntryType.EntryBase) {
                for (String e : vTable.enumNames()) {
                    ps.println3("if (" + upper1(e) + " == null)");
                    ps.println4("errors.EnumNull(" + csv + ", \"" + e + "\");");
                }
            }
            ps.println2("}");
            ps.println();

            //static resolve
            if (HasRef.hasRef(structural)) {
                ps.println2("internal static void Resolve(Config.LoadErrors errors) {");
                ps.println3("foreach (var v in All())");
                ps.println4("v._resolve(errors);");
                ps.println2("}");
                ps.println();
            }
        } // end cfg != null

        //static create
        String pre = isImpl ? "internal new static " : "internal static ";
        ps.println2(pre + name.className + " _create(Config.Stream os)");
        ps.println2("{");
        ps.println3("var self = new " + name.className + "();");
        for (FieldSchema fieldSchema : structural.fields()) {
            String n = fieldSchema.name();
            FieldType t = fieldSchema.type();
            if (t instanceof FList flist) {
                ps.println3("self." + upper1(n) + " = new " + type(t) + "();");
                ps.println3("for (var c = os.ReadInt32(); c > 0; c--)");
                ps.println4("self." + upper1(n) + ".Add(" + _create(flist.item()) + ");");
            } else if (t instanceof FMap fMap) {
                ps.println3("self." + upper1(n) + " = new " + type(t) + "();");
                ps.println3("for (var c = os.ReadInt32(); c > 0; c--)");
                ps.println4("self." + upper1(n) + ".Add(" + _create((fMap.key())) + ", " + _create(fMap.value()) + ");");
            } else {
                ps.println3("self." + upper1(n) + " = " + _create(t) + ";");
            }
        }
        ps.println3("return self;");
        ps.println2("}");
        ps.println();

        //resolve
        if (HasRef.hasRef(structural)) {
            pre = isImpl ? "internal override " : "internal ";
            ps.println2(pre + "void _resolve(Config.LoadErrors errors)");
            ps.println2("{");


            // 1,先调用子_resolve
            for (FieldSchema field : structural.fields()) {
                FieldType type = field.type();
                if (!HasRef.hasRef(type)) {
                    continue;
                }
                switch (type) {
                    case StructRef ignored -> {
                        ps.println3(upper1(field.name()) + "._resolve(errors);");
                    }
                    case FList ignored -> {
                        ps.println3("foreach (var e in " + upper1(field.name()) + ")");
                        ps.println3("{");
                        ps.println4("e._resolve(errors);");
                        ps.println3("}");
                    }
                    case FMap ignored -> {
                        ps.println3("foreach (var kv in " + upper1(field.name()) + ".Map)");
                        ps.println3("{");
                        ps.println4("kv.Value._resolve(errors);");
                        ps.println3("}");
                    }
                    case Primitive ignored -> {
                    }
                }
            }

            // 2,处理本struct里的refSimple，
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                if (!(fk.refKey() instanceof RefKey.RefSimple refSimple)) {
                    continue;
                }
                FieldSchema firstField = fk.key().fieldSchemas().getFirst();
                String refName = refName(fk);
                String fkStr = "\"" + fk.name() + "\"";

                switch (firstField.type()) {
                    case SimpleType ignored -> {
                        ps.println3(refName + " = " + tableGet(fk.refTableSchema(), refSimple, actualParams(fk.key())));
                        if (!refSimple.nullable()) {
                            ps.println3("if (" + refName + " == null) errors.RefNull(" + csv + ", ToString(), " + fkStr + ");");
                        }
                    }
                    case FList ignored -> {
                        ps.println3(refName + " = new " + refType(fk) + "();");
                        ps.println3("foreach (var e in " + upper1(firstField.name()) + ")");
                        ps.println3("{");
                        ps.println4("var r = " + tableGet(fk.refTableSchema(), refSimple, "e"));
                        ps.println4("if (r == null) errors.RefNull(" + csv + ", ToString() , " + fkStr + ");");
                        ps.println4(refName + ".Add(r);");
                        ps.println3("}");
                    }
                    case FMap ignored -> {
                        ps.println3(refName + " = new " + refType(fk) + "();");
                        ps.println3("foreach (var kv in " + upper1(firstField.name()) + ".Map)");
                        ps.println3("{");
                        ps.println4("var k = kv.Key;");
                        ps.println4("var v = " + tableGet(fk.refTableSchema(), refSimple, "kv.Value"));
                        ps.println4("if (v == null) errors.RefNull(" + csv + ", ToString(), " + fkStr + ");");
                        ps.println4(refName + ".Add(k, v);");
                        ps.println3("}");
                    }
                }
            }

            // 3,处理本struct里的refList
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                if (!(fk.refKey() instanceof RefKey.RefList refList)) {
                    continue;
                }
                ps.println3(refName(fk) + " = new List<" + fullName(fk.refTableSchema()) + ">();");
                ps.println3("foreach (var v in " + fullName(fk.refTableSchema()) + ".All())");
                ps.println3("{");

                List<String> eqs = new ArrayList<>();
                for (int i = 0; i < fk.key().fields().size(); i++) {
                    String k = fk.key().fields().get(i);
                    String rk = refList.keyNames().get(i); // refKey不可能是refTable的primary key，所以可以直接调用keyNames
                    eqs.add("v." + upper1(rk) + ".Equals(" + upper1(k) + ")");
                }
                ps.println3("if (" + String.join(" && ", eqs) + ")");
                ps.println4(refName(fk) + ".Add(v);");
                ps.println3("}");
            }


            ps.println("	    }");
            ps.println();
        }

        ps.println("    }");
        ps.println("}");

    }

    private void generateMapGetBy(KeySchema keySchema, GenCs.Name name, CachedIndentPrinter ps, boolean isPrimaryKey) {
        generateKeyClassIf(keySchema, ps);

        //static all
        String mapName = isPrimaryKey ? "all" : uniqueKeyMapName(keySchema);
        String allType = "Config.KeyedList<" + keyClassName(keySchema) + ", " + name.className + ">";
        ps.println2("static " + allType + " " + mapName + " = null;");
        ps.println();

        //static get
        String getByName = isPrimaryKey ? "Get" : uniqueKeyGetByName(keySchema);
        ps.println2("public static " + name.className + " " + getByName + "(" + formalParams(keySchema.fieldSchemas()) + ")");
        ps.println2("{");
        ps.println3(name.className + " v;");
        ps.println3("return " + mapName + ".TryGetValue(" + actualParamsKey(keySchema) + ", out v) ? v : null;");
        ps.println2("}");
        ps.println();
    }

    private void generateAllMapPut(TableSchema table, CachedIndentPrinter ps) {
        generateMapPut(table.primaryKey(), ps, true);
        for (KeySchema uniqueKey : table.uniqueKeys()) {
            generateMapPut(uniqueKey, ps, false);
        }
    }

    private void generateMapPut(KeySchema keySchema, CachedIndentPrinter ps, boolean isPrimaryKey) {
        String mapName = isPrimaryKey ? "all" : uniqueKeyMapName(keySchema);
        ps.println4(mapName + ".Add(" + actualParamsKeySelf(keySchema) + ", self);");
    }


    private void generateKeyClassIf(KeySchema keySchema, CachedIndentPrinter ps) {
        if (keySchema.fields().size() > 1) {
            String keyClassName = keyClassName(keySchema);
            //static Key class
            ps.println2("class " + keyClassName);
            ps.println2("{");
            for (FieldSchema f : keySchema.fieldSchemas()) {
                ps.println3("readonly " + type(f.type()) + " " + upper1(f.name()) + ";");
            }
            ps.println();

            ps.println3("public " + keyClassName + "(" + formalParams(keySchema.fieldSchemas()) + ")");
            ps.println3("{");
            for (FieldSchema f : keySchema.fieldSchemas()) {
                ps.println4("this." + upper1(f.name()) + " = " + lower1(f.name()) + ";");
            }
            ps.println3("}");
            ps.println();

            ps.println3("public override int GetHashCode()");
            ps.println3("{");
            ps.println4("return " + hashCodes(keySchema.fieldSchemas()) + ";");
            ps.println3("}");

            ps.println3("public override bool Equals(object obj)");
            ps.println3("{");
            ps.println4("if (obj == null) return false;");
            ps.println4("if (obj == this) return true;");
            ps.println4("var o = obj as " + keyClassName + ";");
            ps.println4("return o != null && " + equals(keySchema.fieldSchemas()) + ";");
            ps.println3("}");

            ps.println2("}");
            ps.println();
        }
    }

    private String uniqueKeyGetByName(KeySchema keySchema) {
        return "GetBy" + keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining());
    }

    private String uniqueKeyMapName(KeySchema keySchema) {
        return lower1(keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining()) + "Map");
    }

    private String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining()) + "Key";
        else
            return type(keySchema.fieldSchemas().getFirst().type());
    }

    private String formalParams(List<FieldSchema> fs) {
        return fs.stream().map(f -> type(f.type()) + " " + lower1(f.name())).collect(Collectors.joining(", "));
    }

    private String actualParams(KeySchema keySchema) {
        return keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining(", "));
    }

    private String actualParamsKey(KeySchema keySchema) {
        String p = keySchema.fields().stream().map(Generator::lower1).collect(Collectors.joining(", "));
        return keySchema.fields().size() > 1 ? "new " + keyClassName(keySchema) + "(" + p + ")" : p;
    }

    private String actualParamsKeySelf(KeySchema keySchema) {
        String p = keySchema.fields().stream().map(n -> "self." + upper1(n)).collect(Collectors.joining(", "));
        return keySchema.fields().size() > 1 ? "new " + keyClassName(keySchema) + "(" + p + ")" : p;
    }

    private String equals(List<FieldSchema> fs) {
        return fs.stream().map(f -> upper1(f.name()) + ".Equals(o." + upper1((f.name())) + ")").collect(Collectors.joining(" && "));
    }

    private String hashCodes(List<FieldSchema> fs) {
        return fs.stream().map(f -> upper1(f.name()) + ".GetHashCode()").collect(Collectors.joining(" + "));
    }

    private String toStrings(List<FieldSchema> fs) {
        return fs.stream().map(f -> toString(f.name(), f.type())).collect(Collectors.joining(" + \",\" + "));
    }

    private String toString(String n, FieldType t) {
        if (t instanceof FList)
            return "CSV.ToString(" + upper1(n) + ")";
        else
            return upper1(n);
    }

    private String tableGet(TableSchema refTable, RefKey.RefSimple refSimple, String actualParam) {
        switch (refSimple) {
            case RefKey.RefPrimary ignored -> {
                return fullName(refTable) + ".Get(" + actualParam + ");";
            }
            case RefKey.RefUniq refUniq -> {
                return fullName(refTable) + ".GetBy" + refUniq.keyNames().stream().map(Generator::upper1).
                        collect(Collectors.joining()) + "(" + actualParam + ");";
            }
        }
    }


    private String refType(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "List<" + fullName(fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return fullName(fk.refTableSchema());
                    }
                    case FList ignored2 -> {
                        return "List<" + fullName(fk.refTableSchema()) + ">";
                    }
                    case FMap fMap -> {
                        return "KeyedList<" + type(fMap.key()) + ", " + fullName(fk.refTableSchema()) + ">";
                    }
                }
            }
        }
    }

    private String refName(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "ListRef" + upper1(fk.name());
            }
            case RefKey.RefSimple refSimple -> {
                if (refSimple.nullable()) {
                    return "NullableRef" + upper1(fk.name());
                } else {
                    return "Ref" + upper1(fk.name());
                }
            }
        }
    }

    private String fullName(Nameable nameable) {
        return new Name(pkg, prefix, nameable).fullName;
    }


    private String type(FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int";
            case LONG -> "long";
            case FLOAT -> "float";
            case STRING -> "string";
            case TEXT -> isLangSwitch ? pkg + ".Text" : "string";
            case StructRef structRef -> fullName(structRef.obj());
            case FList fList -> "List<" + type(fList.item()) + ">";
            case FMap fMap -> "KeyedList<" + type(fMap.key()) + ", " + type(fMap.value()) + ">";
        };
    }

    private String _create(FieldType t) {
        return switch (t) {
            case BOOL -> "os.ReadBool()";
            case INT -> "os.ReadInt32()";
            case LONG -> "os.ReadInt64()";
            case FLOAT -> "os.ReadSingle()";
            case STRING -> "os.ReadString()";
            case TEXT -> isLangSwitch ? pkg + ".Text._create(os)" : "os.ReadString()";
            case StructRef structRef -> fullName(structRef.obj()) + "._create(os)";
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }

    private void copyFile(String file) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/support/" + file);
             BufferedReader br = new BufferedReader(new InputStreamReader(is != null ? is : new FileInputStream("src/support/" + file), "GBK"));
             CachedIndentPrinter ps = createCode(new File(dstDir, file), encoding)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ps.println(line);
            }
        }
    }

    private void genCSVProcessor() {
        try (CachedIndentPrinter ps = createCode(new File(dstDir, "CSVProcessor.cs"), encoding)) {
            ps.println("using System.Collections.Generic;");
            if (!pkg.equals("Config")) {
                ps.println("using Config;");
            }
            ps.println();
            ps.println("namespace " + pkg);
            ps.println("{");

            ps.println1("public static class CSVProcessor");
            ps.println1("{");
            ps.println2("public static readonly LoadErrors Errors = new LoadErrors();");
            ps.println();
            ps.println2("public static void Process(Config.Stream os)");
            ps.println2("{");
            ps.println3("var configNulls = new List<string>");
            ps.println3("{");
            Iterable<TableSchema> tableSchemas = cfgSchema.sortedTables();
            for (TableSchema table : tableSchemas) {
                ps.println4("\"" + table.name() + "\",");
            }
            ps.println3("};");

            ps.println3("for(;;)");
            ps.println3("{");
            ps.println4("var csv = os.ReadCfg();");
            ps.println4("if (csv == null)");
            ps.println5("break;");

            ps.println4("switch(csv)");
            ps.println4("{");
            for (TableSchema table : tableSchemas) {
                ps.println5("case \"" + table.name() + "\":");
                ps.println6("configNulls.Remove(csv);");
                ps.println6(fullName(table) + ".Initialize(os, Errors);");
                ps.println6("break;");
            }
            ps.println5("default:");
            ps.println6("Errors.ConfigDataAdd(csv);");
            ps.println6("break;");
            ps.println4("}");
            ps.println3("}");

            ps.println3("foreach (var csv in configNulls)");
            ps.println4("Errors.ConfigNull(csv);");

            for (TableSchema table : tableSchemas) {
                if (HasRef.hasRef(table)) {
                    ps.println3(fullName(table) + ".Resolve(Errors);");
                }
            }

            ps.println2("}");
            ps.println();
            ps.println1("}");
            ps.println("}");
            ps.println();
        }
    }

}

package configgen.gengo;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.gencs.GenCs;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;
import configgen.value.CfgValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;
import static configgen.schema.FieldType.Primitive.TEXT;

public class GenGo extends GeneratorWithTag {
    private final String dir;
    private File dstDir;
    private final String pkg;
    private CfgSchema cfgSchema;
    private final String encoding;
    private boolean isLangSwitch;
    private final String prefix;

    public GenGo(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "config");
        pkg = parameter.get("pkg", "config");
        encoding = parameter.get("encoding", "GBK");
        prefix = parameter.get("prefix", null);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/')).toFile();
        CfgValue cfgValue = ctx.makeValue(tag);
        cfgSchema = cfgValue.schema();

        for (Fieldable fieldable : cfgSchema.sortedFieldables()) {
            switch (fieldable) {
                case StructSchema structSchema -> {
                    generateStruct(structSchema, null);
                }
                case InterfaceSchema interfaceSchema -> {
                    generateInterface(interfaceSchema);
                    for (StructSchema impl : interfaceSchema.impls()) {
                        generateStruct(impl, null);
                    }
                }
            }
        }

        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            generateStruct(vTable.schema(), vTable);
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
        ps.println("package %s", name.pkg);
        if (!name.pkg.equals("config")) {
            ps.println("import (");
            ps.println1("\"config\"");
            ps.println(")");
        }

        ps.println("type %s interface", name.className);
        ps.println("{");
        if (sInterface.nullableEnumRefTable() != null) {
            ps.println1("type() " + fullName(sInterface.nullableEnumRefTable()));
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
    }

    private void generateStruct(Structural structural, CfgValue.VTable vTable) {
        Name name = new Name(pkg, prefix, structural);
        File csFile = dstDir.toPath().resolve(name.path).toFile();
        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
            generateStructClass(structural, vTable, name, ps);
        }
    }

    private void generateStructClass(Structural structural, CfgValue.VTable vTable, Name name, CachedIndentPrinter ps) {
        TableSchema table = vTable != null ? vTable.schema() : null;
        ps.println("package %s", name.shortPkg);
        ps.println();

        InterfaceSchema nullableInterface = structural instanceof StructSchema struct ? struct.nullableInterface() : null;
        boolean isImpl = nullableInterface != null;
        boolean hasNoFields = structural.fields().isEmpty();

        //import
        if (!structural.foreignKeys().isEmpty()) {
            HashSet<Name> importNames = new HashSet<>();
            for (ForeignKeySchema foreignKey : structural.foreignKeys()) {
                Name refTableName = new Name(pkg, prefix, foreignKey.refTableSchema());
                if (!refTableName.pkg.equals(name.pkg)) {
                    importNames.add(refTableName);
                }
            }
            if (importNames.size() > 0) {
                ps.println("import (");
                for (Name importName : importNames) {
                    ps.println1("%s \"%s\"", importName.importAlias, importName.importPath);
                }
                ps.println(")");
                ps.println();
            }
        }

        ps.println("type %s struct {", name.className);

        // field property
        if (!structural.fields().isEmpty()) {
            for (FieldSchema fieldSchema : structural.fields()) {
                printGoVar(ps, fieldSchema.name(), type(fieldSchema.type()), fieldSchema.comment());
            }
        }

        // ref property
        if (!structural.foreignKeys().isEmpty()) {
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                printGoVar(ps, refName(fk), refType(fk), null);
            }
        }

        ps.println("}");
        ps.println();


        //impl type
        if (isImpl) {
            ps.println("//is %s", fullName(nullableInterface));
            TableSchema enumRefTable = nullableInterface.nullableEnumRefTable();
            if (enumRefTable != null) {
                ps.println("func getType() string {");
                ps.println1("return \"%s\"", name.className);
                ps.println("}");
                ps.println();
            }
        }

        // entry
        boolean isTableEnumOrEntry = (table != null && table.entry() instanceof EntryType.EntryBase);
        if (isTableEnumOrEntry) {
            ps.println("//entries");
            ps.println("var (");
            for (String e : vTable.enumNames()) {
                printGoVar(ps, e, name.className, null);
            }
            ps.println(")");
            ps.println();
        }


        //getter
        if (!structural.fields().isEmpty()) {
            ps.println("//getters");
            for (FieldSchema fieldSchema : structural.fields()) {
                printGoVarGetter(ps, name.className, fieldSchema.name(), type(fieldSchema.type()), fieldSchema.comment());
                ps.println();
            }
        }

        // ref property
        if (!structural.foreignKeys().isEmpty()) {
            ps.println("//ref properties");
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                printGoVarGetter(ps, structural.name(), refName(fk), refType(fk), null);
            }
            ps.println();
        }


        List<FieldSchema> keys = table != null ? table.primaryKey().fieldSchemas() : structural.fields();

        String csv = "\"" + structural.name() + "\"";
        if (table != null) {
            generateMapGetBy(table.primaryKey(), name, ps, true);

            //todo 联合主键

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

//        //static create
//        String pre = isImpl ? "internal new static " : "internal static ";
//        ps.println2(pre + name.className + " _create(Config.Stream os)");
//        ps.println2("{");
//        ps.println3("var self = new " + name.className + "();");
//        for (FieldSchema fieldSchema : structural.fields()) {
//            String n = fieldSchema.name();
//            FieldType t = fieldSchema.type();
//            if (t instanceof FieldType.FList(FieldType.SimpleType item)) {
//                ps.println3("self." + upper1(n) + " = new " + type(t) + "();");
//                ps.println3("for (var c = os.ReadInt32(); c > 0; c--)");
//                ps.println4("self." + upper1(n) + ".Add(" + _create(item) + ");");
//            } else if (t instanceof FieldType.FMap(FieldType.SimpleType key, FieldType.SimpleType value)) {
//                ps.println3("self." + upper1(n) + " = new " + type(t) + "();");
//                ps.println3("for (var c = os.ReadInt32(); c > 0; c--)");
//                ps.println4("self." + upper1(n) + ".Add(" + _create((key)) + ", " + _create(value) + ");");
//            } else {
//                ps.println3("self." + upper1(n) + " = " + _create(t) + ";");
//            }
//        }
//        ps.println3("return self;");
//        ps.println2("}");
//        ps.println();
//
//        //resolve
//        if (HasRef.hasRef(structural)) {
//            pre = isImpl ? "internal override " : "internal ";
//            ps.println2(pre + "void _resolve(Config.LoadErrors errors)");
//            ps.println2("{");
//
//
//            // 1,先调用子_resolve
//            for (FieldSchema field : structural.fields()) {
//                FieldType type = field.type();
//                if (!HasRef.hasRef(type)) {
//                    continue;
//                }
//                switch (type) {
//                    case FieldType.StructRef ignored -> {
//                        ps.println3(upper1(field.name()) + "._resolve(errors);");
//                    }
//                    case FieldType.FList ignored -> {
//                        ps.println3("foreach (var e in " + upper1(field.name()) + ")");
//                        ps.println3("{");
//                        ps.println4("e._resolve(errors);");
//                        ps.println3("}");
//                    }
//                    case FieldType.FMap ignored -> {
//                        ps.println3("foreach (var kv in " + upper1(field.name()) + ".Map)");
//                        ps.println3("{");
//                        ps.println4("kv.Value._resolve(errors);");
//                        ps.println3("}");
//                    }
//                    case FieldType.Primitive ignored -> {
//                    }
//                }
//            }
//
//            // 2,处理本struct里的refSimple，
//            for (ForeignKeySchema fk : structural.foreignKeys()) {
//                if (!(fk.refKey() instanceof RefKey.RefSimple refSimple)) {
//                    continue;
//                }
//                FieldSchema firstField = fk.key().fieldSchemas().getFirst();
//                String refName = refName(fk);
//                String fkStr = "\"" + fk.name() + "\"";
//
//                switch (firstField.type()) {
//                    case FieldType.SimpleType ignored -> {
//                        ps.println3(refName + " = " + tableGet(fk.refTableSchema(), refSimple, actualParams(fk.key())));
//                        if (!refSimple.nullable()) {
//                            ps.println3("if (" + refName + " == null) errors.RefNull(" + csv + ", ToString(), " + fkStr + ");");
//                        }
//                    }
//                    case FieldType.FList ignored -> {
//                        ps.println3(refName + " = new " + refType(fk) + "();");
//                        ps.println3("foreach (var e in " + upper1(firstField.name()) + ")");
//                        ps.println3("{");
//                        ps.println4("var r = " + tableGet(fk.refTableSchema(), refSimple, "e"));
//                        ps.println4("if (r == null) errors.RefNull(" + csv + ", ToString() , " + fkStr + ");");
//                        ps.println4(refName + ".Add(r);");
//                        ps.println3("}");
//                    }
//                    case FieldType.FMap ignored -> {
//                        ps.println3(refName + " = new " + refType(fk) + "();");
//                        ps.println3("foreach (var kv in " + upper1(firstField.name()) + ".Map)");
//                        ps.println3("{");
//                        ps.println4("var k = kv.Key;");
//                        ps.println4("var v = " + tableGet(fk.refTableSchema(), refSimple, "kv.Value"));
//                        ps.println4("if (v == null) errors.RefNull(" + csv + ", ToString(), " + fkStr + ");");
//                        ps.println4(refName + ".Add(k, v);");
//                        ps.println3("}");
//                    }
//                }
//            }
//
//            // 3,处理本struct里的refList
//            for (ForeignKeySchema fk : structural.foreignKeys()) {
//                if (!(fk.refKey() instanceof RefKey.RefList refList)) {
//                    continue;
//                }
//                ps.println3(refName(fk) + " = new List<" + fullName(fk.refTableSchema()) + ">();");
//                ps.println3("foreach (var v in " + fullName(fk.refTableSchema()) + ".All())");
//                ps.println3("{");
//
//                List<String> eqs = new ArrayList<>();
//                for (int i = 0; i < fk.key().fields().size(); i++) {
//                    String k = fk.key().fields().get(i);
//                    String rk = refList.keyNames().get(i); // refKey不可能是refTable的primary key，所以可以直接调用keyNames
//                    eqs.add("v." + upper1(rk) + ".Equals(" + upper1(k) + ")");
//                }
//                ps.println3("if (" + String.join(" && ", eqs) + ")");
//                ps.println4(refName(fk) + ".Add(v);");
//                ps.println3("}");
//            }
//
//
//            ps.println("	    }");
//            ps.println();
//        }
//
//        ps.println("    }");
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
            case FList fList -> "[]" + type(fList.item());
            case FMap fMap -> "KeyedList<" + type(fMap.key()) + ", " + type(fMap.value()) + ">";
        };
    }

    private void printGoVar(CachedIndentPrinter ps, String varName, String t, String comment) {
        ps.println1("%s %s%s", lower1(varName), t, comment != null && !comment.isEmpty() ? " //" + comment : "");
    }

    private void printGoVarGetter(CachedIndentPrinter ps, String className, String varName, String t, String comment) {
        ps.println("func (t *%s) Get%s() %s {", className, upper1(varName), t);
        ps.println1("return t.%s", lower1(varName));
        ps.println("}");
    }

    private String fullName(Nameable nameable) {
        return new Name(pkg, prefix, nameable).fullName;
    }

    private String refType(ForeignKeySchema fk) {
        Name refTableName = new Name(pkg, prefix, fk.refTableSchema());

        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "List<" + fullName(fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return refTableName.importAlias + '.' + refTableName.className;
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

    private String formalParams(List<FieldSchema> fs) {
        return fs.stream().map(f -> type(f.type()) + " " + lower1(f.name())).collect(Collectors.joining(", "));
    }

    private void generateMapGetBy(KeySchema keySchema, Name name, CachedIndentPrinter ps, boolean isPrimaryKey) {
        //todo 多主键
        //generateKeyClassIf(keySchema, ps);

        //static all，list
        String mapName = isPrimaryKey ? "all" : uniqueKeyMapName(keySchema);

        ps.println("var all []%s",name.className);
        ps.println("func GetAll() []%s {",name.className);
        ps.println1("return all[:len(all)]:len(all)]");
        ps.println("}");
        ps.println();

        //static get
        ps.println("var allMap map[%s]%s", keyClassName(keySchema), name.className);
        ps.println("func Get(key %s) (%s,bool){", keyClassName(keySchema), name.className);
        ps.println1("return allMap[key]");
        ps.println("}");
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

    private String uniqueKeyGetByName(KeySchema keySchema) {
        return "GetBy" + keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining());
    }

    private String actualParamsKey(KeySchema keySchema) {
        String p = keySchema.fields().stream().map(Generator::lower1).collect(Collectors.joining(", "));
        return keySchema.fields().size() > 1 ? "new " + keyClassName(keySchema) + "(" + p + ")" : p;
    }

    private String actualParamsKeySelf(KeySchema keySchema) {
        String p = keySchema.fields().stream().map(n -> "self." + upper1(n)).collect(Collectors.joining(", "));
        return keySchema.fields().size() > 1 ? "new " + keyClassName(keySchema) + "(" + p + ")" : p;
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

    private static class Name {
        final String pkg;
        final String shortPkg;
        final String className;
        final String fullName;
        final String path;
        final String importPath;
        final String importAlias;

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

            if (prefix != null)
                className = prefix + upper1(seps[seps.length - 1]);
            else
                className = upper1(seps[seps.length - 1]);

            if (pks.length == 0) {
                pkg = topPkg;
                shortPkg = topPkg.toLowerCase();
            } else {
                pkg = topPkg + "." + String.join(".", pks);
                shortPkg = pks[pks.length - 1].toLowerCase();
            }

            if (pkg.isEmpty())
                fullName = className;
            else
                fullName = pkg + "." + className;

            if (pks.length == 0) {
                path = className + ".go";
            } else {
                path = String.join("/", pks) + "/" + className + ".go";
            }

            importPath = pkg.toLowerCase().replace('.', '/');
            importAlias = importPath.replace('/', '_');
        }
    }
}
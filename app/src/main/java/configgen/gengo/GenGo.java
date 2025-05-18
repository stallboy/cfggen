package configgen.gengo;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;
import configgen.value.CfgValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
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
        Name.modName = parameter.get("mod", null);
        Name.topPkg = pkg;
        Name.prefix = prefix;
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
        Name name = new Name(sInterface);
        File csFile = dstDir.toPath().resolve(name.path).toFile();
        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
            generateInterface(sInterface, name, ps);
        }
    }

    private void generateInterface(InterfaceSchema sInterface, Name name, CachedIndentPrinter ps) {
        ps.println("package %s", name.shortPkg);
        ps.println("type %s interface", name.className);
        ps.println("{");
        ps.println("}");
    }

    private void generateStruct(Structural structural, CfgValue.VTable vTable) {
        Name name = new Name(structural);
        File csFile = dstDir.toPath().resolve(name.path).toFile();
        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
            generateStructClass(structural, vTable, name, ps);
        }
    }

    private void AddImportPkg(Name className, HashMap<String, Name> importNames, Nameable nameable) {
        var name = new Name(nameable);
        if (className.importPkgPath.equals(name.importPkgPath))
            return;
        if (importNames.containsKey(name.importPkgPath)) return;
        importNames.put(name.importPkgPath, name);

    }

    private void generateStructClass(Structural structural, CfgValue.VTable vTable, Name name, CachedIndentPrinter ps) {
        TableSchema table = vTable != null ? vTable.schema() : null;
        ps.println("package %s", name.shortPkg);
        ps.println();

        InterfaceSchema nullableInterface = structural instanceof StructSchema struct ? struct.nullableInterface() : null;
        boolean isImpl = nullableInterface != null;
        boolean hasNoFields = structural.fields().isEmpty();

        //import
        HashMap<String, Name> importNames = new HashMap();
        if (!structural.foreignKeys().isEmpty()) {
            for (ForeignKeySchema foreignKey : structural.foreignKeys()) {
                AddImportPkg(name, importNames, foreignKey.refTableSchema());
            }
        }
        if (!structural.fields().isEmpty()) {
            for (FieldSchema fieldSchema : structural.fields()) {
                FieldType fieldType = fieldSchema.type();
                switch (fieldType) {
                    case FieldType.Primitive ignored -> {
                        continue;
                    }
                    case FieldType.StructRef structRef -> {
                        AddImportPkg(name, importNames, structRef.obj());
                    }
                    case FieldType.FList fList -> {
                        if (fList.item() instanceof FieldType.StructRef structRef) {
                            AddImportPkg(name, importNames, structRef.obj());
                        }
                    }
                    case FieldType.FMap fMap -> {
                        if (fMap.key() instanceof FieldType.StructRef structRef) {
                            AddImportPkg(name, importNames, structRef.obj());
                        }
                        if (fMap.value() instanceof FieldType.StructRef structRef) {
                            AddImportPkg(name, importNames, structRef.obj());
                        }
                    }
                }
            }

            if (importNames.size() > 0) {
                ps.println("import (");
                for (Name importName : importNames.values()) {
                    ps.println1("%s \"%s\"", importName.importPkgAlias, importName.importPkgPath);
                }
                ps.println(")");
                ps.println();
            }
        }

        ps.println("type %s struct {", name.className);

        // field property
        if (!structural.fields().isEmpty()) {
            for (FieldSchema fieldSchema : structural.fields()) {
                printGoVar(ps, fieldSchema.name(), type(name, fieldSchema.type()), fieldSchema.comment());
            }
        }

        // ref property
        if (!structural.foreignKeys().isEmpty()) {
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                printGoVar(ps, refName(fk), refType(name, fk), null);
            }
        }

        ps.println("}");
        ps.println();


        //impl type
        if (isImpl) {
            ps.println("//is %s", RefName(name, nullableInterface));
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
                printGoVarGetter(ps, name.className, fieldSchema.name(), type(name, fieldSchema.type()), fieldSchema.comment());
                ps.println();
            }
        }

        // ref property
        if (!structural.foreignKeys().isEmpty()) {
            ps.println("//ref properties");
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                printGoVarGetter(ps, structural.name(), refName(fk), refType(name, fk), null);
            }
            ps.println();
        }


        List<FieldSchema> keys = table != null ? table.primaryKey().fieldSchemas() : structural.fields();

        String csv = "\"" + structural.name() + "\"";
        if (table != null) {
        }
    }

    private String type(Name className, FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int";
            case LONG -> "long";
            case FLOAT -> "float";
            case STRING -> "string";
            case TEXT -> isLangSwitch ? pkg + ".Text" : "string";
            case StructRef structRef -> RefName(className, structRef.obj());
            case FList fList -> "[]" + type(className, fList.item());
            case FMap fMap -> "KeyedList<" + type(className, fMap.key()) + ", " + type(className, fMap.value()) + ">";
        };
    }

    private void printGoVar(CachedIndentPrinter ps, String varName, String t, String comment) {
        //举例: taskid int //任务完成条件类型（id的范围为1-100）
        ps.println1("%s %s%s", lower1(varName), t, comment != null && !comment.isEmpty() ? " //" + comment : "");
    }

    private void printGoVarGetter(CachedIndentPrinter ps, String className, String varName, String t, String comment) {
        ps.println("func (t *%s) Get%s() %s {", className, upper1(varName), t);
        ps.println1("return t.%s", lower1(varName));
        ps.println("}");
    }

    private String RefName(Name className, Nameable variable) {
        var varName = new Name(variable);
        if (varName.importPkgPath.equals(className.importPkgPath)) {
            return varName.className;
        }
        return varName.refName;
    }

    private String refType(Name className, ForeignKeySchema fk) {
        Name refTableName = new Name(fk.refTableSchema());
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "List<" + RefName(className, fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return refTableName.importPkgAlias + '.' + refTableName.className;
                    }
                    case FList ignored2 -> {
                        return "List<" + RefName(className, fk.refTableSchema()) + ">";
                    }
                    case FMap fMap -> {
                        return "KeyedList<" + type(className, fMap.key()) + ", " + RefName(className, fk.refTableSchema()) + ">";
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

    private void generateMapGetBy(KeySchema keySchema, Name name, CachedIndentPrinter ps, boolean isPrimaryKey) {
        //todo 多主键
        //generateKeyClassIf(keySchema, ps);

        //static all，list
        String mapName = isPrimaryKey ? "all" : uniqueKeyMapName(keySchema);

        ps.println("var all []%s", name.className);
        ps.println("func GetAll() []%s {", name.className);
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
//        else return type(keySchema.fieldSchemas().getFirst().type());
        else return null;
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
        static String modName;
        static String topPkg;
        static String prefix;

        final String pkg;
        final String shortPkg;
        final String className;
        final String fullName;
        final String path;
        final String importPkgPath;
        final String importPkgAlias;
        final String refName;

        Name(Nameable nameable) {
            String name;
            InterfaceSchema nullableInterface = nameable instanceof StructSchema struct ? struct.nullableInterface() : null;
            if (nullableInterface != null) {
                name = nullableInterface.name().toLowerCase() + "." + nameable.name();
            } else {
                name = nameable.name();
            }
            String[] seps = name.split("\\.");
            String[] pks = new String[seps.length - 1];
            for (int i = 0; i < pks.length; i++) {
                pks[i] = lower1(seps[i]);
            }

            if (prefix != null) className = prefix + upper1(seps[seps.length - 1]);
            else className = upper1(seps[seps.length - 1]);

            if (pks.length == 0) {
                pkg = topPkg;
                shortPkg = topPkg.toLowerCase();
            } else {
                pkg = topPkg + "." + String.join(".", pks);
                shortPkg = pks[pks.length - 1].toLowerCase();
            }

            if (pkg.isEmpty()) fullName = className;
            else fullName = pkg + "." + className;

            if (pks.length == 0) {
                path = className + ".go";
            } else {
                path = String.join("/", pks) + "/" + className + ".go";
            }

            String path = pkg.toLowerCase().replace('.', '/');
            importPkgAlias = path.replace('/', '_');
            importPkgPath = String.format("%s/%s", modName, path);
            refName = importPkgAlias + "." + className;
        }
    }
}
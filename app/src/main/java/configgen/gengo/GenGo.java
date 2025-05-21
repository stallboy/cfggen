package configgen.gengo;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.gencs.GenCs;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static configgen.schema.FieldType.Primitive.*;
import static configgen.schema.FieldType.Primitive.TEXT;

public class GenGo extends GeneratorWithTag {
    private static final Logger log = LoggerFactory.getLogger(GenGo.class);
    private final String dir;
    private File dstDir;
    private final String pkg;
    private CfgSchema cfgSchema;
    private final String encoding;
    private boolean isLangSwitch;

    public GenGo(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "config");
        pkg = parameter.get("pkg", "config");
        encoding = parameter.get("encoding", "GBK");
        GoName.modName = parameter.get("mod", null);
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
        GoName name = new GoName(sInterface);
//        File csFile = dstDir.toPath(). .toFile();
        File file = dstDir.toPath().resolve(name.filePath).toFile();
        try (CachedIndentPrinter ps = createCode(file, encoding)) {
            generateInterface(sInterface, name, ps);
        }
    }

    private void generateInterface(InterfaceSchema sInterface, GoName name, CachedIndentPrinter ps) {
        ps.println("package %s", pkg);
        ps.println("type %s interface", name.className);
        ps.println("{");
        ps.println("}");
    }

    private void generateStruct(Structural structural, CfgValue.VTable vTable) {
        GoName name = new GoName(structural);
        File csFile = dstDir.toPath().resolve(name.filePath).toFile();
        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
            generateStructClass(structural, vTable, name, ps);
        }
    }

    private void generateStructClass(Structural structural, CfgValue.VTable vTable, GoName name, CachedIndentPrinter ps) {
        TableSchema table = vTable != null ? vTable.schema() : null;
        ps.println("package %s", pkg);
        ps.println();
        ps.println("""                
                import (
                	"cfgtest/stream"
                	"fmt"
                	"os"
                )
                """);

        InterfaceSchema nullableInterface = structural instanceof StructSchema struct ? struct.nullableInterface() : null;
        boolean isImpl = nullableInterface != null;
        boolean hasNoFields = structural.fields().isEmpty();

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
                printGoVarGetter(ps, name.className, refName(fk), refType(fk), null);
            }
            ps.println();
        }

        if (table != null) {
            List<KeySchema> keySchemas = new ArrayList<>();
            keySchemas.add(table.primaryKey());
            keySchemas.addAll(table.uniqueKeys());

            for (KeySchema keySchema : keySchemas) {
                genKeyClassIf(keySchema, ps);
            }

            String mapTemplate = """
                        ${mapName}Map map[${IdType}]*${className}
                    """;

            StringBuilder mapDefines = new StringBuilder();
            for (KeySchema keySchema : keySchemas) {
                var mapDefine = mapTemplate.
                        replace("${mapName}", mapName(keySchema)).
                        replace("${IdType}", keyClassName(keySchema)).
                        replace("${className}", name.className);
                mapDefines.append(mapDefine);
            }

            String varDefineTemplate = "${varName} ${varType}";
            String mapGetTemplate_Multi = """
                    func(t *${className}Mgr) GetBy${IdType}(${varDefines}) (*${className},bool) {
                        v, ok := t.${mapName}Map[${IdType}{${paramVars}}]
                        return v, ok
                    }
                    
                    """;
            String mapGetTemplate_Single = """
                    func(t *${className}Mgr) GetBy${paramVars}(${varDefines}) (*${className},bool) {
                        v, ok := t.${mapName}Map[${paramVars}]
                        return v, ok
                    }
                    
                    """;
            StringBuilder mapGetBy = new StringBuilder();
            for (KeySchema keySchema : keySchemas) {
                StringBuilder varDefines = new StringBuilder();
                StringBuilder paramVars = new StringBuilder();
                var fieldSchemas = keySchema.fieldSchemas();
                for (int i = 0; i < fieldSchemas.size(); i++) {
                    var fieldSchema = fieldSchemas.get(i);
                    String varName = fieldSchema.name();
                    String varType = type(fieldSchema.type());
                    String varDefine = varDefineTemplate.replace("${varName}", varName).replace("${varType}", varType);
                    varDefines.append(varDefine);
                    paramVars.append(fieldSchema.name());
                    if (i < fieldSchemas.size() - 1) {
                        varDefines.append(", ");
                        paramVars.append(", ");
                    }
                }
                var template = fieldSchemas.size() > 1 ? mapGetTemplate_Multi : mapGetTemplate_Single;
                mapGetBy.append(template.
                        replace("${className}", name.className).
                        replace("${IdType}", keyClassName(keySchema)).
                        replace("${mapName}", mapName(keySchema)).
                        replace("${varDefines}", varDefines).
                        replace("${paramVars}", paramVars));
            }

            //gen all,GenAll
            ps.println("""
                    type ${className}Mgr struct {
                        all []*${className}
                    ${mapDefines}}
                    
                    func(t *${className}Mgr) GetAll() []*${className} {
                        return t.all
                    }
                    
                    ${mapGetBy}
                    """.
                    replace("${className}", name.className).
                    replace("${IdType}", keyClassName(table.primaryKey())).
                    replace("${mapDefines}", mapDefines.toString()).
                    replace("${mapGetBy}", mapGetBy.toString())
            );

            //gen Init
            String initTemplate = """
    func (t *${className}Mgr) Init(file *os.File) {
        cnt := stream.ReadInt32(file)
        t.all = make([]*AiAi, 0, cnt)
        for i := 0; i < int(cnt); i++ {
            v := &AiAi{}
    ${ReadValue}
            break
        }
    }
                    """;
            StringBuilder readValue = new StringBuilder();
            String readValueTemplate = """
                            v.${varName} = stream.Read${varType}(file)
                    """;
            for (FieldSchema fieldSchema : structural.fields()) {
                String varName = Generator.lower1(fieldSchema.name());
                String varType = Generator.upper1(type(fieldSchema.type()));
                String readValueLine = readValueTemplate.replace("${varName}", varName).replace("${varType}", varType);
                readValue.append(readValueLine);
            }

            ps.println(initTemplate.replace("${ReadValue}", readValue).replace("${className}", name.className));
        }
    }

    private void genKeyClassIf(KeySchema keySchema, CachedIndentPrinter ps) {
        if (keySchema.fieldSchemas().size() > 1) {
            ps.println("type %s struct {", keyClassName(keySchema));
            for (FieldSchema field : keySchema.fieldSchemas()) {
                printGoVar(ps, field.name(), type(field.type()), null);
            }
            ps.println("}");
            ps.println();
        }
    }

    private String type(FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int32";
            case LONG -> "long";
            case FLOAT -> "float64";
            case STRING -> "string";
            case TEXT -> isLangSwitch ? pkg + ".Text" : "string";
            case StructRef structRef -> ClassName(structRef.obj());
            case FList fList -> "[]" + type(fList.item());
            case FMap fMap -> String.format("map[%s]%s", type(fMap.key()), type(fMap.value()));
        };
    }

    private void printGoVar(CachedIndentPrinter ps, String varName, String t, String comment) {
        //举例: taskid int //任务完成条件类型（id的范围为1-100）
        ps.println1("%s %s%s", lower1(varName), t, comment != null && !comment.isEmpty() ? " //" + comment : "");
    }

    private void printGoVarGetter(CachedIndentPrinter ps, String className, String varName, String t, String
            comment) {
        ps.println("func (t *%s) Get%s() %s {", className, upper1(varName), t);
        ps.println1("return t.%s", lower1(varName));
        ps.println("}");
    }

    private String ClassName(Nameable variable) {
        var varName = new GoName(variable);
        return varName.className;
    }

    private String refType(ForeignKeySchema fk) {
        GoName refTableName = new GoName(fk.refTableSchema());
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "[]" + ClassName(fk.refTableSchema());
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return "*" + refTableName.className;
                    }
                    case FList ignored2 -> {
                        return "[]" + ClassName(fk.refTableSchema());
                    }
                    case FMap fMap -> {
                        return String.format("map[%s]%s", type(fMap.key()), ClassName(fk.refTableSchema()));
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

    private String uniqueKeyMapName(KeySchema keySchema) {
        return lower1(keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining()) + "Map");
    }

    private String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return "Key" + keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining());
        else return type(keySchema.fieldSchemas().getFirst().type());
    }

    private String mapName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1) {
            return Generator.lower1(keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining()));
        } else {
            return Generator.lower1(keySchema.fields().getFirst());
        }
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
}


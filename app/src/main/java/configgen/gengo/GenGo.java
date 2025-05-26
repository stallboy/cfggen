package configgen.gengo;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
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

        GenCfgMgrFile(cfgValue);

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
        String template = """
                package ${pkg}
                type ${className} interface {}
                func create${className}(stream *Stream) ${className}{
                    var typeName = stream.ReadString()
                    switch typeName {
                ${caseImpls}
                    default:
                            panic("unexpected ${className} type: " + typeName)
                    }
                }
                """;

        StringBuilder caseImpls = new StringBuilder();
        String caseImpl = """
                    case "${implName}":
                        return create${implClassName}(stream)
                """;
        for (StructSchema impl : sInterface.impls()) {
            caseImpls.append(caseImpl.
                    replace("${implName}", impl.name()).
                    replace("${implClassName}", new GoName(impl).className)
            );
        }

        ps.println(template.
                replace("${pkg}", pkg).
                replace("${className}", name.className).
                replace("${caseImpls}", caseImpls)
        );
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
                ps.println1("%s %s", lower1(refName(fk)), refType(fk));
            }
        }

        ps.println("}");
        ps.println();

        String createStructCode = genCreateStruct(structural, name);
        ps.println(createStructCode);

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

            //list ref 和 map ref
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                FieldSchema keyShema = fk.key().fieldSchemas().getFirst();
                switch (keyShema.type()) {
                    case FMap ign -> {
                        MapRefCode mapRefCode = new MapRefCode(name, fk);
                        ps.println(mapRefCode.codeGetByDefine);
                    }
                    case FList ign -> {
                        ListRefCode listRefCode = new ListRefCode(name, fk);
                        ps.println(listRefCode.codeGetByDefine);
                    }
                    default -> {
                        continue;
                    }
                }
            }

            for (ForeignKeySchema fk : structural.foreignKeys()) {
                String codeGetFuncName;
                switch (fk.refKey()) {
                    case RefKey.RefPrimary ignored -> {
                        switch (fk.key().fieldSchemas().getFirst().type()) {
                            case FMap ign -> {
                                continue;
                            }
                            case FList ign ->{
                                continue;
                            }
                            default -> {
                                KeySchemaCode keySchemaCode = new KeySchemaCode(fk.key(), name, true);
                                codeGetFuncName = keySchemaCode.codeGetFuncName;
                            }
                        }
                    }
                    case RefKey.RefUniq ignored -> {
                        codeGetFuncName = "RefUniq";
                    }
                    case RefKey.RefList ignored -> {
                        // => ref到联合键，返回符合key的集合
                        PrimarySubKeyCode primarySubKeyCode = new PrimarySubKeyCode(name, fk.key().fieldSchemas().getFirst());
                        codeGetFuncName = primarySubKeyCode.codeGetByFuncName;
                    }
                }
                GoName refTbName = new GoName(fk.refTableSchema());
                String getRefCode = """
                        func (t *${className}) Get${refName}() ${refType} {
                            if t.${refName} == nil {
                                t.${refName} = Get${refTableClassName}Mgr().${codeGetFuncName}(t.${varName})
                            }
                            return t.${refName}
                        }
                        """.replace("${className}", name.className).
                        replace("${refName}", Generator.lower1(refName(fk))).
                        replace("${refTableClassName}", refTbName.className).
                        replace("${codeGetFuncName}", codeGetFuncName).
                        replace("${varName}", Generator.lower1(fk.name())).
                        replace("${refType}", refType(fk));
                ;
                ps.println(getRefCode);
            }
            ps.println();
        }

        if (table != null) {
            GenMapGetCode(name, ps, table);
        }
    }

    private void GenMapGetCode(GoName name, CachedIndentPrinter ps, TableSchema table) {
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

        StringBuilder subKeyDefines = new StringBuilder();
        StringBuilder subKeyGetBy = new StringBuilder();
        if (table.primaryKey().fieldSchemas().size() > 1) {
            for (FieldSchema fieldSchema : table.primaryKey().fieldSchemas()) {
                PrimarySubKeyCode primarySubKeyCode = new PrimarySubKeyCode(name, fieldSchema);
                subKeyDefines.append(primarySubKeyCode.codeMapDefine);
                subKeyGetBy.append(primarySubKeyCode.codeGetByDefine);
            }
        }

        StringBuilder mapGetBy = new StringBuilder();
        StringBuilder createMaps = new StringBuilder();
        StringBuilder setMaps = new StringBuilder();
        for (KeySchema keySchema : keySchemas) {
            KeySchemaCode keySchemaCode = new KeySchemaCode(keySchema, name, keySchema.equals(table.primaryKey()));
            mapGetBy.append(keySchemaCode.codeGetBy);
            createMaps.append(keySchemaCode.codeCreateMap);
            setMaps.append(keySchemaCode.codeSetMap);
        }

        //gen all,GenAll
        ps.println("""
                type ${className}Mgr struct {
                    all []*${className}
                ${mapDefines}
                ${subKeyDefines}}
                
                func(t *${className}Mgr) GetAll() []*${className} {
                    return t.all
                }
                
                ${mapGetBy}${subKeyGetBy}
                """.
                replace("${className}", name.className).
                replace("${IdType}", keyClassName(table.primaryKey())).
                replace("${mapDefines}", mapDefines).
                replace("${mapGetBy}", mapGetBy).
                replace("${subKeyDefines}", subKeyDefines).
                replace("${subKeyGetBy}", subKeyGetBy)
        );

        //gen Init
        ps.println("""
                func (t *${className}Mgr) Init(stream *Stream) {
                    cnt := stream.ReadInt32()
                    t.all = make([]*${className}, 0, cnt)
                ${createMaps}
                    for i := 0; i < int(cnt); i++ {
                        v := create${className}(stream)
                        t.all = append(t.all, v)
                ${setMaps}    }
                }
                """.
                replace("${className}", name.className).
                replace("${createMaps}", createMaps).
                replace("${setMaps}", setMaps)
        );
    }

    private String genCreateStruct(Structural structural, GoName name) {
        String templateCreate = """
                func create${className}(${streamIf} *Stream) *${className} {
                    v := &${className}{}
                ${readValues}    return v
                }
                """;
        StringBuilder readValues = new StringBuilder();
        for (FieldSchema fieldSchema : structural.fields()) {
            readValues.append(genReadValue(fieldSchema));
        }
        return templateCreate.replace("${className}", name.className).
                replace("${readValues}", readValues).
                replace("${streamIf}", structural.fields().size() > 0 ? "stream" : "_");
    }

    private String genReadValue(FieldSchema fieldSchema) {
        String varName = Generator.lower1(fieldSchema.name());
        String varType = Generator.upper1(type(fieldSchema.type()));
        switch (fieldSchema.type()) {
            case StructRef structRef:
                return """
                            v.${varName} = ${genReadField}
                        """.
                        replace("${varName}", varName).
                        replace("${genReadField}", genReadField(structRef));
            case FList fList:
                return """
                            ${varName}Size := stream.ReadInt32()
                            v.${varName} = make([]${ElemType}, ${varName}Size)
                            for i := 0; i < int(${varName}Size); i++ {
                                v.${varName}[i] = ${ReadElem}
                            }
                        """.
                        replace("${varName}", varName).
                        replace("${ElemType}", type(fList.item())).
                        replace("${ReadElem}", genReadField(fList.item()));
            case FMap fMap:
                return """
                        	${varName}Size := stream.ReadInt32()
                        	v.${varName} = make(map[${KeyType}]${ValueType}, ${varName}Size)
                        	for i := 0; i < int(${varName}Size); i++ {
                        		var k = ${ReadKey}
                        		v.${varName}[k] = ${ReadValue}
                        	}
                        """.replace("${varName}", varName).
                        replace("${KeyType}", type(fMap.key())).
                        replace("${ValueType}", type(fMap.value())).
                        replace("${ReadValue}", genReadField(fMap.value())).
                        replace("${ReadKey}", genReadField(fMap.key()));
            default:
                return """
                            v.${varName} = ${genReadField}
                        """.replace("${varName}", varName).
                        replace("${genReadField}", genReadField(fieldSchema.type()));

        }
    }

    private String genReadField(FieldType fieldType) {
        return switch (fieldType) {
            case StructRef structRef -> "create${varType}(stream)".
                    replace("${varType}", ClassName(structRef.obj()));
            case FList ignore -> null;
            case FMap ignore -> null;
            default -> "stream.Read${varType}()".replace("${varType}", Generator.upper1(type(fieldType)));
        };
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

    public static String type(FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int32";
            case LONG -> "int64";
            case FLOAT -> "float32";
            case STRING -> "string";
            case TEXT -> "string";
            case StructRef structRef -> {
                Fieldable fieldable = structRef.obj();
                yield switch (fieldable) {
                    case StructSchema ignored -> "*" + ClassName(fieldable);
                    case InterfaceSchema ignored -> ClassName(fieldable);
                };
            }
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

    private void GenCfgMgrFile(CfgValue cfgValue) {
        String mgrFileName = Generator.lower1(pkg) + "mgr";
        String mgrClassName = Generator.upper1(pkg) + "Mgr";
        File csFile = dstDir.toPath().resolve(mgrFileName + ".go").toFile();

        String templateDefine = """
                var ${className}Mgr *${ClassName}Mgr
                
                func Get${ClassName}Mgr() *${ClassName}Mgr {
                    return ${className}Mgr
                }
                
                """;
        String templateCase = """
                        case "${ClassReadName}":
                            ${className}Mgr = &${ClassName}Mgr{}
                            ${className}Mgr.Init(myStream)
                """;
        String template = """
                package ${pkg}
                
                import "io"
                
                ${templateDefines}func Init(reader io.Reader) {
                    myStream := &Stream{reader: reader}
                    for {
                        cfgName := myStream.ReadString()
                        if cfgName == "" {
                            break
                        }
                        switch cfgName {
                ${templateCases}        }
                    }
                }""";

        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
            StringBuilder templateDefines = new StringBuilder();
            StringBuilder templateCases = new StringBuilder();
            for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
                GoName name = new GoName(vTable.schema());
                templateDefines.append(templateDefine.
                        replace("${ClassName}", name.className).
                        replace("${className}", Generator.lower1(name.className))
                );
                templateCases.append(templateCase.
                        replace("${ClassName}", name.className).
                        replace("${className}", Generator.lower1(name.className)).
                        replace("${ClassReadName}", name.pkgName)
                );
            }
            ps.println(template.
                    replace("${pkg}", pkg).
                    replace("${mgrClassName}", mgrClassName).
                    replace("${templateDefines}", templateDefines).
                    replace("${templateCases}", templateCases)
            );
        }
    }

    private static String ClassName(Nameable variable) {
        var varName = new GoName(variable);
        return varName.className;
    }

    private String refType(ForeignKeySchema fk) {
        GoName refTableName = new GoName(fk.refTableSchema());
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "[]*" + ClassName(fk.refTableSchema());
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {
                    case SimpleType ignored2 -> {
                        return "*" + refTableName.className;
                    }
                    case FList ignored2 -> {
                        return "[]*" + ClassName(fk.refTableSchema());
                    }
                    case FMap fMap -> {
                        return String.format("map[%s]*%s", type(fMap.key()), ClassName(fk.refTableSchema()));
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

    public static String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return "Key" + keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining());
        else return type(keySchema.fieldSchemas().getFirst().type());
    }

    public static String mapName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1) {
            return Generator.lower1(keySchema.fields().stream().map(Generator::upper1).collect(Collectors.joining()));
        } else {
            return Generator.lower1(keySchema.fields().getFirst());
        }
    }

}


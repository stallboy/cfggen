package configgen.gengo;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.gengo.model.InterfaceModel;
import configgen.gengo.model.StructModel;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;
import configgen.util.JteEngine;
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
        InterfaceModel model = new InterfaceModel(pkg, name, sInterface);
        File file = dstDir.toPath().resolve(name.filePath).toFile();
        try (CachedIndentPrinter ps = new CachedIndentPrinter(file, encoding)) {
            JteEngine.render("go/GenInterface.jte", model, ps);
        }
    }

    private void generateStruct(Structural structural, CfgValue.VTable vTable) {
        GoName name = new GoName(structural);
        StructModel model = new StructModel(pkg, name, structural,vTable);
        File csFile = dstDir.toPath().resolve(name.filePath).toFile();
        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
//            generateStructClass(structural, vTable, name, ps);
            JteEngine.render("go/GenStruct.jte", model, ps);
        }
    }

    public static String genReadField(FieldType t) {
        return switch (t) {
            case BOOL -> "stream.ReadBool()";
            case INT -> "stream.ReadInt32()";
            case LONG -> "stream.ReadInt64()";
            case FLOAT -> "stream.ReadFloat32()";
            case STRING, TEXT -> "stream.ReadString()";
            case StructRef structRef -> String.format("create%s(stream)",ClassName(structRef.obj()));
            case FList ignored -> null;
            case FMap ignored -> null;
        };
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

    public static String ClassName(Nameable variable) {
        var varName = new GoName(variable);
        return varName.className;
    }

    public static String refType(ForeignKeySchema fk) {
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

    public static String refName(ForeignKeySchema fk) {
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


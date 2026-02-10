package configgen.gengo;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.util.*;
import configgen.util.CachedIndentPrinter.CacheConfig;
import configgen.value.CfgValue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.Primitive.*;
import static configgen.schema.FieldType.Primitive.TEXT;

public class GoCodeGenerator extends GeneratorWithTag {
    private final String dir;
    private final String pkg;
    private final String encoding;
    private Path dstDir;
    private CacheConfig cacheConfig;

    public GoCodeGenerator(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "config");
        pkg = parameter.get("pkg", "config");
        encoding = parameter.get("encoding", "GBK");
        GoName.modName = parameter.get("mod", null);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));
        CfgValue cfgValue = ctx.makeValue(tag);
        CfgSchema cfgSchema = cfgValue.schema();
        cacheConfig = CacheConfig.of();

        FileUtil.copyFileIfNotExist("support/go/stream.go",
                "src/main/resources/support/go/stream.go",
                dstDir.resolve("stream.go"),
                encoding);

        genCfgMgrFile(cfgValue);

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
        CachedFiles.deleteOtherFiles(dstDir.toFile());
    }

    private CachedIndentPrinter createCode(String fn) {
        return cacheConfig.printer(dstDir.resolve(fn), encoding);
    }

    private void generateInterface(InterfaceSchema sInterface) {
        GoName name = new GoName(sInterface);
        InterfaceModel model = new InterfaceModel(pkg, name, sInterface);
        try (var ps = createCode(name.filePath)) {
            JteEngine.render("go/GenInterface.jte", model, ps);
        }
    }

    private void generateStruct(Structural structural, CfgValue.VTable vTable) {
        GoName name = new GoName(structural);
        StructModel model = new StructModel(pkg, name, structural, vTable);
        try (var ps = createCode(name.filePath)) {
            JteEngine.render("go/GenStruct.jte", model, ps);
        }
    }

    private void genCfgMgrFile(CfgValue cfgValue) {
        String mgrFileName = StringUtil.lower1(pkg) + "mgr";
        var model = new CfgMgrModel(pkg, cfgValue);
        try (var ps = createCode(mgrFileName + ".go")) {
            JteEngine.render("go/GenCfgMgr.jte", model, ps);
        }
    }


    public static String keyClassName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1)
            return "Key" + keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining());
        else return type(keySchema.fieldSchemas().getFirst().type());
    }

    public static String mapName(KeySchema keySchema) {
        if (keySchema.fieldSchemas().size() > 1) {
            return StringUtil.lower1(keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()));
        } else {
            return StringUtil.lower1(keySchema.fields().getFirst());
        }
    }

    public static String type(FieldType t) {
        return switch (t) {
            case BOOL -> "bool";
            case INT -> "int32";
            case LONG -> "int64";
            case FLOAT -> "float32";
            case STRING, TEXT -> "string";
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

    public static String ClassName(Nameable variable) {
        var varName = new GoName(variable);
        return varName.className;
    }
}


package configgen.gengo;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.util.JteEngine;
import configgen.value.CfgValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
        copySupportFileIfNotExist("stream.go", dstDir.toPath(), encoding);
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
        CachedFiles.deleteOtherFiles(dstDir);
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
            JteEngine.render("go/GenStruct.jte", model, ps);
        }
    }

    private void GenCfgMgrFile(CfgValue cfgValue) {
        String mgrFileName = Generator.lower1(pkg) + "mgr";
        var model = new CfgMgrModel(pkg, cfgValue);
        File csFile = dstDir.toPath().resolve(mgrFileName + ".go").toFile();
        try (CachedIndentPrinter ps = createCode(csFile, encoding)) {
            JteEngine.render("go/GenCfgMgr.jte", model, ps);
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
    
    public static String ClassName(Nameable variable) {
        var varName = new GoName(variable);
        return varName.className;
    }
}


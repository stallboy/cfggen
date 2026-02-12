package configgen.gengd;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.util.FileUtil;
import configgen.util.JteEngine;
import configgen.util.CachedIndentPrinter.CacheConfig;
import configgen.value.CfgValue;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static configgen.value.CfgValue.VTable;

public class GdCodeGenerator extends GeneratorWithTag {
    private final String dir;
    public final String prefix;

    private Path dstDir;
    private CacheConfig cacheConfig;
    public CfgSchema cfgSchema;
    public boolean isLangSwitch;

    public static final String ENCODING = "UTF-8";
    private static final List<String> COPY_FILES = List.of(
            "ConfigStream.gd",
            "ConfigLoader.gd",
            "ConfigErrors.gd",
            "TextPoolManager.gd"
    );
    private static final String CLIENT_TEXT_FILE = "ConfigText.gd";


    public GdCodeGenerator(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "config");
        prefix = parameter.get("prefix", "Data");
    }


    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        cfgSchema = cfgValue.schema();

        dstDir = Paths.get(dir);
        cacheConfig = CacheConfig.of();

        isLangSwitch = ctx.nullableLangSwitch() != null;

        List<String> needCopyFiles = new ArrayList<>(4);
        needCopyFiles.addAll(COPY_FILES);
        if (isLangSwitch) {
            needCopyFiles.add(CLIENT_TEXT_FILE);
        }
        for (String fn : needCopyFiles) {
            FileUtil.copyFileIfNotExist("/support/gd/" + fn,
                    "src/main/resources/support/gd/" + fn,
                    dstDir.resolve(fn),
                    ENCODING);
        }

        generateProcessor(cfgSchema);

        for (Fieldable fieldable : cfgSchema.sortedFieldables()) {
            switch (fieldable) {
                case StructSchema structSchema -> {
                    generateStruct(structSchema);
                }
                case InterfaceSchema interfaceSchema -> {
                    generateInterface(interfaceSchema);
                    for (StructSchema impl : interfaceSchema.impls()) {
                        generateStruct(impl);
                    }
                }
            }
        }

        for (VTable vTable : cfgValue.sortedTables()) {
            generateTable(vTable);
        }

        CachedFiles.keepMetaAndDeleteOtherFiles(dstDir.toFile(), ".uid");
    }

    private void generateInterface(InterfaceSchema sInterface) {
        InterfaceModel model = new InterfaceModel(this, sInterface);
        try (var ps = createCode(model.name.path)) {
            JteEngine.render("gd/GenInterface.jte", model, ps);
        }
    }

    private void generateStruct(StructSchema structSchema) {
        generateStructOrTable(structSchema, null);
    }

    private void generateTable(CfgValue.VTable vTable) {
        generateStructOrTable(vTable.schema(), vTable);
    }

    private void generateStructOrTable(Structural structural, CfgValue.VTable nullableVTable) {
        StructModel model = new StructModel(this, structural, nullableVTable);
        try (var ps = createCode(model.name.path)) {
            JteEngine.render("gd/GenStruct.jte", model, ps);
        }
    }

    private void generateProcessor(CfgSchema cfgSchema) {
        try (var ps = createCode("ConfigProcessor.gd")) {
            JteEngine.render("gd/Processor.jte", new ProcessorModel(this, cfgSchema.sortedTables()), ps);
        }
    }


    private CachedIndentPrinter createCode(String fn) {
        return cacheConfig.printer(dstDir.resolve(fn), ENCODING);
    }
}

package configgen.gengd;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.i18n.LangSwitchable;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.util.FileUtils;
import configgen.util.JteEngine;
import configgen.value.CfgValue;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.VTable;

public class GenGd extends GeneratorWithTag {
    public final String prefix;
    public final Path dstDir;
    public CfgSchema cfgSchema;
    public boolean isLangSwitch;

    private static final String ENCODING = "UTF-8";
    private static final List<String> COPY_FILES = List.of(
            "ConfigStream.gd",
            "ConfigLoader.gd",
            "ConfigErrors.gd"
    );
    private static final String TEXT_MGR_FILE = "TextMgr.gd";


    public GenGd(Parameter parameter) {
        super(parameter);
        String dir = parameter.get("dir", "config");
        prefix = parameter.get("prefix", "Data");
        dstDir = Paths.get(dir);
    }


    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        cfgSchema = cfgValue.schema();

        isLangSwitch = ctx.nullableLangSwitch() != null;


        List<String> needCopyFiles = new ArrayList<>(4);
        needCopyFiles.addAll(COPY_FILES);
        if (isLangSwitch) {
            needCopyFiles.add(TEXT_MGR_FILE);
        }
        for (String fn : needCopyFiles) {
            FileUtils.copyFileIfNotExist("support/gd/" + fn,
                    "src/main/resources/support/gd/" + fn,
                    dstDir.resolve(fn), ENCODING);
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

        if (isLangSwitch) {
            generateText(ctx.nullableLangSwitch());
        }

        CachedFiles.keepMetaAndDeleteOtherFiles(dstDir.toFile());
    }

    private void generateInterface(InterfaceSchema sInterface) {
        InterfaceModel model = new InterfaceModel(this, sInterface);
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve(model.name.path), ENCODING)) {
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
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve(model.name.path), ENCODING)) {
            JteEngine.render("gd/GenStruct.jte", model, ps);
        }
    }

    private void generateProcessor(CfgSchema cfgSchema) {
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve("ConfigProcessor.gd"), ENCODING)) {
            JteEngine.render("gd/Processor.jte", new ProcessorModel(this, cfgSchema.sortedTables()), ps);
        }
    }

    private void generateText(LangSwitchable langSwitch) {
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve("ConfigText.gd"), ENCODING)) {
            List<String> languages = langSwitch.languages();
            JteEngine.render("gd/Text.jte", Map.of("languages", languages), ps);
        }
    }
}

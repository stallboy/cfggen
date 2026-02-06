package configgen.gengd;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.i18n.LangSwitchable;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.util.JteEngine;
import configgen.value.CfgValue;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.VTable;

public class GenGd extends GeneratorWithTag {
    public final String prefix;
    public final String encoding;
    public final Path dstDir;
    public CfgSchema cfgSchema;
    public boolean isLangSwitch;

    public GenGd(Parameter parameter) {
        super(parameter);
        String dir = parameter.get("dir", "config");
        prefix = parameter.get("prefix", "Data");
        encoding = parameter.get("encoding", "UTF-8");
        dstDir = Paths.get(dir);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        cfgSchema = cfgValue.schema();

        isLangSwitch = ctx.nullableLangSwitch() != null;
        copySupportFileIfNotExist("gd/ConfigStream.gd", "ConfigStream.gd", dstDir, encoding);
        copySupportFileIfNotExist("gd/ConfigLoader.gd", "ConfigLoader.gd", dstDir, encoding);
        copySupportFileIfNotExist("gd/ConfigErrors.gd", "ConfigErrors.gd", dstDir, encoding);
        if (isLangSwitch) {
            copySupportFileIfNotExist("gd/TextMgr.gd", "TextMgr.gd", dstDir, encoding);
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
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve(model.name.path), encoding)) {
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
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve(model.name.path), encoding)) {
            JteEngine.render("gd/GenStruct.jte", model, ps);
        }
    }

    private void generateProcessor(CfgSchema cfgSchema) {
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve("ConfigProcessor.gd"), encoding)) {
            JteEngine.render("gd/Processor.jte", new ProcessorModel(this, cfgSchema.sortedTables()), ps);
        }
    }

    private void generateText(LangSwitchable langSwitch) {
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve("ConfigText.gd"), encoding)) {
            List<String> languages = langSwitch.languages();
            JteEngine.render("gd/Text.jte", Map.of("languages", languages), ps);
        }
    }
}

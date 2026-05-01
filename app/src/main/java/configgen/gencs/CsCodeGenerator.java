package configgen.gencs;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.i18n.LangSwitchable;
import configgen.schema.*;
import configgen.util.*;
import configgen.util.CachedIndentPrinter.CacheConfig;
import configgen.value.CfgValue;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static configgen.value.CfgValue.VTable;

public class CsCodeGenerator extends GeneratorWithTag {
    private final String dir;
    public final String pkg;
    public final String encoding;
    public final String prefix;
    public final boolean serverText;

    private Path dstDir;
    private CacheConfig cacheConfig;
    public boolean isLangSwitch;
    private static final List<String> COPY_FILES = List.of(
            "Loader.cs"
    );

    public CsCodeGenerator(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "Config");
        pkg = parameter.get("pkg", "Config");
        encoding = parameter.get("encoding", "GBK");
        prefix = parameter.get("prefix", "D");
        serverText = parameter.has("serverText");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);  // 这里只需要schema，生成value只用于检验数据
        CfgSchema cfgSchema = cfgValue.schema();

        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));
        cacheConfig = CacheConfig.of();

        isLangSwitch = ctx.nullableLangSwitch() != null;
        for (String fn : COPY_FILES) {
            FileUtil.copyFileIfNotExist("/support/cs/" + fn,
                    "src/main/resources/support/cs/" + fn,
                    dstDir.resolve(fn),
                    encoding);
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

        generateModuleLoaders(cfgSchema, cfgValue);

        if (isLangSwitch) { //生成 Text这个Bean
            generateText(ctx.nullableLangSwitch());
        }

        CachedFiles.keepMetaAndDeleteOtherFiles(dstDir.toFile());
    }

    private void generateInterface(InterfaceSchema sInterface) {
        InterfaceModel model = new InterfaceModel(this, sInterface);
        try (var ps = createCode(model.name.path)) {
            JteEngine.render("cs/GenInterface.jte", model, ps);
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
            JteEngine.render("cs/GenStruct.jte", model, ps);
        }
    }

    private void generateProcessor(CfgSchema cfgSchema) {
        ProcessorModel model = new ProcessorModel(this, cfgSchema.sortedTables());
        try (var ps = createCode("Processor.cs")) {
            JteEngine.render("cs/Processor.jte", model, ps);
        }
    }

    private void generateText(LangSwitchable langSwitch) {
        List<String> languages = langSwitch.languages().stream().map(StringUtil::upper1).toList();
        Map<String, Object> model = Map.of("pkg", pkg, "languages", languages);
        try (var ps = createCode("Text.cs")) {
            String template = serverText ? "cs/ServerText.jte" : "cs/ClientText.jte";
            JteEngine.render(template, model, ps);
        }
    }


    private CachedIndentPrinter createCode(String fn) {
        return cacheConfig.printer(dstDir.resolve(fn), encoding);
    }

    private void generateModuleLoaders(CfgSchema cfgSchema, CfgValue cfgValue) {
        Map<String, ModuleModel> modules = new LinkedHashMap<>();

        // Process fieldables (structs, interfaces, interface impls)
        for (Fieldable fieldable : cfgSchema.sortedFieldables()) {
            switch (fieldable) {
                case StructSchema structSchema -> {
                    StructModel sm = new StructModel(this, structSchema, null);
                    String key = getModuleKey(sm.name);
                    modules.computeIfAbsent(key, k -> new ModuleModel(this, k)).addStruct(sm);
                }
                case InterfaceSchema interfaceSchema -> {
                    InterfaceModel im = new InterfaceModel(this, interfaceSchema);
                    String key = getModuleKey(im.name);
                    ModuleModel mm = modules.computeIfAbsent(key, k -> new ModuleModel(this, k));
                    mm.addInterface(im);
                    for (StructSchema impl : interfaceSchema.impls()) {
                        StructModel sm = new StructModel(this, impl, null);
                        mm.addStruct(sm);
                    }
                }
            }
        }

        // Process tables
        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            StructModel sm = new StructModel(this, vTable.schema(), vTable);
            String key = getModuleKey(sm.name);
            modules.computeIfAbsent(key, k -> new ModuleModel(this, k)).addStruct(sm);
        }

        // Generate module loader files
        for (ModuleModel mm : modules.values()) {
            try (var ps = createCode(mm.outputFilaPath())) {
                JteEngine.render("cs/GenModuleLoader.jte", mm, ps);
            }
        }
    }

    private String getModuleKey(Name name) {
        String path = name.path;
        int slash = path.indexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }
}

package configgen.gencs;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.i18n.LangSwitch;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.util.JteEngine;
import configgen.value.CfgValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.VTable;

public class GenCs extends GeneratorWithTag {
    public final String pkg;
    public final String encoding;
    public final String prefix;
    public final Path dstDir;
    public CfgSchema cfgSchema;
    public boolean isLangSwitch;

    public GenCs(Parameter parameter) {
        super(parameter);
        String dir = parameter.get("dir", "Config");
        pkg = parameter.get("pkg", "Config");
        encoding = parameter.get("encoding", "GBK");
        prefix = parameter.get("prefix", "Data");
        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);  // 这里只需要schema，生成value只用于检验数据
        cfgSchema = cfgValue.schema();

        isLangSwitch = ctx.nullableLangSwitch() != null;
        copyFileIfNotExist("CSV.cs");
        copyFileIfNotExist("Loader.cs");
        copyFileIfNotExist("LoadErrors.cs");
        copyFileIfNotExist("KeyedList.cs");
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

        if (isLangSwitch) { //生成Text这个Bean
            generateText(ctx.nullableLangSwitch());
        }

        CachedFiles.keepMetaAndDeleteOtherFiles(dstDir.toFile());
    }

    private void generateInterface(InterfaceSchema sInterface) {
        InterfaceModel model = new InterfaceModel(this, sInterface);
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve(model.name.path), encoding)) {
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
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve(model.name.path), encoding)) {
            JteEngine.render("cs/GenStruct.jte", model, ps);
        }
    }

    private void generateProcessor(CfgSchema cfgSchema) {
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve("Processor.cs"), encoding)) {
            JteEngine.render("cs/Processor.jte", new ProcessorModel(this, cfgSchema.sortedTables()), ps);
        }
    }

    private void generateText(LangSwitch langSwitch) {
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstDir.resolve("Text.cs"), encoding)) {
            List<String> languages = langSwitch.languages().stream().map(Generator::upper1).toList();
            JteEngine.render("cs/Text.jte", Map.of("pkg", pkg, "languages", languages), ps);
        }
    }

    private void copyFileIfNotExist(String file) throws IOException {
        Path dst = dstDir.resolve(file);
        if (Files.exists(dst)) {
            CachedFiles.keepFile(dst);
            return;
        }

        try (InputStream is = getClass().getResourceAsStream("/support/" + file);
             BufferedReader br = new BufferedReader(new InputStreamReader(is != null ? is : new FileInputStream("src/support/" + file), StandardCharsets.UTF_8));
             CachedIndentPrinter ps = new CachedIndentPrinter(dst, encoding)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ps.println(line);
            }
        }
    }


}

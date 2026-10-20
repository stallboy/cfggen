package configgen.genbytes;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.gen.CliException;
import configgen.genjava.*;
import configgen.i18n.LangSwitchable;
import configgen.i18n.LangSwitchableRuntime;
import configgen.util.CachedFileOutputStream;
import configgen.util.CachedFiles;
import configgen.util.XorCipherOutputStream;
import configgen.value.CfgValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class BytesGenerator extends GeneratorWithTag {
    private final String dir;
    private final String cipher;
    private final boolean isLangSeparated;
    private final String pack;
    private final String requires;
    private final List<Path> dependencies;
    private CachedFiles outputFiles;

    public BytesGenerator(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", ".");
        cipher = parameter.get("cipher", "");
        pack = parameter.get("pack", "base");
        requires = parameter.get("requires", "").replace(';', ',');
        String deps = parameter.get("deps", "");
        dependencies = deps.isEmpty() ? List.of() : java.util.Arrays.stream(deps.split(";", -1)).map(Path::of).toList();
        if (pack.isBlank() || !pack.equals(pack.strip()) || pack.contains(",") || pack.contains(";")
                || pack.chars().anyMatch(c -> c < 32 || "/\\:*?\"<>|".indexOf(c) >= 0))
            throw new CliException("pack requires a nonempty file name without path separators, reserved characters, commas or semicolons");
        if (!deps.isEmpty() && java.util.Arrays.stream(deps.split(";", -1)).anyMatch(String::isBlank))
            throw new CliException("Empty dependency file in deps");
        isLangSeparated = parameter.has("langSeparated");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        outputFiles = ctx.outputFiles();
        PackageBuilder.Result build = ctx.isFixedSchema() || !requires.isEmpty() || !dependencies.isEmpty()
                ? PackageBuilder.build(ctx, tag, dependencies, pack, requires) : null;
        CfgValue cfgValue = build == null ? ctx.makeValue(tag) : build.own();
        LangSwitchable langSwitch = ctx.nullableLangSwitch();

        // 初始化运行时
        LangSwitchableRuntime langSwitchRuntime = null;
        LangTextPool langTextPool;

        if (langSwitch != null) {
            langSwitchRuntime = new LangSwitchableRuntime(langSwitch);
            langTextPool = new LangTextPool(langSwitch.languages());
        } else {
            langTextPool = new LangTextPool(List.of("default"));
        }

        writePackage(cfgValue, build == null ? cfgValue : build.effective(), langSwitch, langSwitchRuntime, langTextPool);
    }
    private void writePackage(CfgValue value, CfgValue schemaValue, LangSwitchable langSwitch,
                              LangSwitchableRuntime runtime, LangTextPool texts) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("pack", pack);
        if (!requires.isEmpty()) metadata.put("requires", requires);
        metadata.put("defaultLanguage", texts.getTextPools()[0].getLangName());
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("schema", encode(out -> new SchemaSerializer(out).serializeInterface(SchemaParser.parsePackage(schemaValue, langSwitch))));
        StringPool strings = new StringPool();
        Map<String, byte[]> tables = new LinkedHashMap<>();
        for (CfgValue.VTable table : value.sortedTables()) {
            tables.put("." + table.name(), CfgValueSerializer.serializeTableBytes(table, strings, texts, runtime));
        }
        entries.put("strings", encode(strings::serialize));
        for (int i = 0; i < texts.getTextPools().length; i++) {
            TextPool text = texts.getTextPools()[i];
            if (!isLangSeparated || i == 0) entries.put("text/" + text.getLangName(), encode(text::serializeBody));
            else {
                try (ConfigOutput out = createConfigOutput(pack + "_" + text.getLangName() + ".bytes")) {
                    PackageSerializer.write(out, Map.of("pack", pack),
                            Map.of("text/" + text.getLangName(), encode(text::serializeBody)));
                }
            }
        }
        entries.putAll(tables);
        try (ConfigOutput out = createConfigOutput(pack + ".bytes")) { PackageSerializer.write(out, metadata, entries); }
    }

    private static byte[] encode(Consumer<ConfigOutput> writer) {
        var stream = new ByteArrayOutputStream();
        try (ConfigOutput out = new ConfigOutput(stream)) { writer.accept(out); }
        return stream.toByteArray();
    }


    private ConfigOutput createConfigOutput(String fileName) {
        CachedFileOutputStream stream = new CachedFileOutputStream(Path.of(dir, fileName), outputFiles);
        if (cipher.isEmpty()) {
            return new ConfigOutput(stream);
        } else {
            return new ConfigOutput(new XorCipherOutputStream(stream, cipher));
        }
    }
}

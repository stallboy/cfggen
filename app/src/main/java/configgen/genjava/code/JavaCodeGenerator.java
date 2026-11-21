package configgen.genjava.code;

import configgen.util.CachedFiles;

import org.jspecify.annotations.Nullable;

import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.gen.CliException;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.genjava.GenJavaUtil;
import configgen.schema.*;
import configgen.util.*;
import configgen.util.CachedIndentPrinter.CacheConfig;
import configgen.value.CfgValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static configgen.value.CfgValue.VTable;

public class JavaCodeGenerator extends GeneratorWithTag {
    private final String dir;
    private final String pkg;
    private final String encoding;
    private final boolean sealed;
    private final boolean jspecify;
    private final @Nullable String buildersFilename;
    private final @Nullable String configgenDir; // 新增：configgen genjava 源文件复制目录
    private final boolean beautifulName; // 美化由 snake_case schema 名派生的标识符（类名/getter 转 PascalCase、enum 常量转 SCREAMING_SNAKE_CASE），默认 false 保持老行为
    private final String prefix; // 生成类名的前缀，同 csharp 的 prefix，默认为空
    private final int schemaNumPerFile;

    // 并发生成：每个工作线程独占一组打印机缓冲区，避免多线程踩踏共享 StringBuilder
    private final ThreadLocal<CacheConfig> mainCc = ThreadLocal.withInitial(CacheConfig::of);

    // 需要复制的源文件列表
    private static final String[] COPY_FILES = {
            "Schema.java",
            "SchemaBean.java",
            "SchemaCompatibleException.java",
            "SchemaEnum.java",
            "SchemaInterface.java",
            "SchemaList.java",
            "SchemaMap.java",
            "SchemaPrimitive.java",
            "SchemaRef.java",
            "SchemaDeserializer.java",

            "ConfigErr.java",
            "ConfigInput.java",
            "ConfigLoader.java",
            "ConfigPackage.java",
            "LoadValueErrs.java",

            "BytesInspector.java",
            "JsonValue.java",
            "CodeDataInspector.java",
            "CodeDataPrinter.java",
            "Repl.java",
    };

    public JavaCodeGenerator(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "config");
        pkg = parameter.get("pkg", "config");
        encoding = parameter.get("encoding", "UTF-8");
        sealed = !parameter.has("noSealed"); // 默认sealed
        jspecify = parameter.has("jspecify");
        buildersFilename = parameter.get("builders", null);
        configgenDir = parameter.get("configgenDir", null);
        schemaNumPerFile = Integer.parseInt(parameter.get("schemaNumPerFile", "100"));
        if (schemaNumPerFile != -1 && schemaNumPerFile <= 0) {
            throw new CliException("schemaNumPerFile must be -1 or a positive integer");
        }
        beautifulName = parameter.has("beautifulName");
        prefix = parameter.get("prefix", "");
        // 非法前缀（如 1a、my-c）会静默产出编不过的代码，这里直接报错，同 ParameterParser 对布尔取值的严格解析
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            boolean ok = i == 0 ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c);
            if (!ok) {
                throw new CliException("invalid value for parameter 'prefix': '" + prefix
                        + "' is not a valid java identifier fragment");
            }
        }
    }


    @Override
    public void generate(Context ctx, CachedFiles outputFiles) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        CfgData cfgData = ctx.cfgData();
        Path dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));
        outputFiles.prepareOutputDirectory(dstDir);
        Set<String> generatedPackagePaths = ConcurrentHashMap.newKeySet();
        Function<String, CachedIndentPrinter> openCode = filename -> {
            if (jspecify) {
                int slash = filename.lastIndexOf('/');
                generatedPackagePaths.add(slash < 0 ? "" : filename.substring(0, slash));
            }
            return mainCc.get().printer(dstDir.resolve(filename), encoding, outputFiles);
        };

        // 一次生成的固定配置：不可变、随调用链显式传递，替代原先散落在 Name/TypeStr/NameableName 的
        // static 字段（并发生成时会互相踩踏）
        var translationBundle = ctx.nullableTranslationBundle();
        GenCfg cfg = new GenCfg(pkg, sealed, beautifulName, translationBundle == null ? 0 : translationBundle.languageCount(),
                prefix, jspecify);

        List<NameableName> tableDataNames = new ArrayList<>();
        List<String> setAllRefsInMgrLoader = new ArrayList<>();

        Set<String> needBuilderTables = buildersFilename == null ? Set.of() : readNeedBuilderTables(buildersFilename);
        // struct/interface 类与 table 类各自生成独立文件，互不依赖；并发渲染。
        // tableDataNames / setAllRefs 顺序敏感——故每个 table 任务用独立 local 列表，渲染后按原序合并，保证字节级一致。
        List<Callable<@Nullable Void>> structTasks = new ArrayList<>();
        for (Nameable nameable : cfgValue.schema().items()) {
            switch (nameable) {
                case StructSchema s -> structTasks.add(() -> {
                    generateStructClass(cfg, s, openCode);
                    return null;
                });
                case InterfaceSchema iface -> {
                    final InterfaceSchema ifaceF = iface;
                    // interface 连同其 impls 放一个任务：二者可能同名同包（如 Effect），
                    // 串行下 impl 后写覆盖 interface；任务内保持先 interface 后 impls 的顺序，避免并发竞态写反。
                    structTasks.add(() -> {
                        generateInterfaceClass(cfg, ifaceF, openCode);
                        for (StructSchema impl : ifaceF.impls()) {
                            generateStructClass(cfg, impl, openCode);
                        }
                        return null;
                    });
                }
                case TableSchema _ -> {
                }
            }
        }

        List<Callable<TableRefs>> tableTasks = new ArrayList<>();
        for (VTable vtable : cfgValue.tables()) {
            final VTable vt = vtable;
            tableTasks.add(() -> {
                List<NameableName> localDataNames = new ArrayList<>();
                List<String> localSetAllRefs = new ArrayList<>();
                generateTableClass(cfg, vt, cfgData, openCode, needBuilderTables.contains(vt.name()), localDataNames, localSetAllRefs);
                return new TableRefs(localDataNames, localSetAllRefs);
            });
        }

        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            // 两阶段并发：struct 先于 table（struct 不再贡献 ConfigMgr 成员，仅为顺序稳定的并发渲染）
            invokeAllAndWait(executor, structTasks);
            for (TableRefs r : invokeAllAndWait(executor, tableTasks)) {
                tableDataNames.addAll(r.dataNames());
                setAllRefsInMgrLoader.addAll(r.setAllRefs());
            }
        }

        if (translationBundle != null) {
            try (var ps = openCode.apply("Text.java")) {
                JteEngine.render("java/Text.jte",
                        new TextModel(pkg, translationBundle.languages()), ps);
            }
        }

        try (var ps = openCode.apply("ConfigMgr.java")) {
            JteEngine.render("java/ConfigMgr.jte",
                    Map.of("cfg", cfg, "tableDataNames", tableDataNames), ps);
        }

        try (var ps = openCode.apply("ConfigExtension.java")) {
            JteEngine.render("java/ConfigExtension.jte", Map.of("cfg", cfg), ps);
        }

        try (var ps = openCode.apply("ConfigMgrLoader.java")) {
            JteEngine.render("java/ConfigMgrLoader.jte",
                    new ConfigMgrLoaderModel(cfg, cfgValue, setAllRefsInMgrLoader,
                            translationBundle == null ? List.of() : translationBundle.languages()), ps);
        }

        GenConfigCodeSchema.generateAll(openCode, cfg, schemaNumPerFile, cfgValue, translationBundle);

        if (jspecify) {
            Charset charset = Charset.forName(encoding);
            for (String packagePath : new TreeSet<>(generatedPackagePaths)) {
                String packageName = packagePath.isEmpty() ? pkg : pkg + "." + packagePath.replace('/', '.');
                outputFiles.writeFile(dstDir.resolve(packagePath).resolve("package-info.java"),
                        packageInfo(packageName).getBytes(charset));
            }
        }

        copyConfigGenSourcesIfNeed(outputFiles);
    }

    // 单个 table 任务的并发产物：本任务收集的 dataName（供 ConfigMgr 渲染成员）与 setAllRefs 类名
    private record TableRefs(List<NameableName> dataNames, List<String> setAllRefs) {
    }

    private static Set<String> readNeedBuilderTables(String filename) {
        Path fn = Path.of(filename).normalize();
        if (Files.exists(fn)) {
            try {
                return new HashSet<>(Files.readAllLines(fn, StandardCharsets.UTF_8));
            } catch (IOException e) {
                Logger.log("读文件异常, 忽略此文件: %s", fn.toAbsolutePath());
            }
        }
        return Set.of();
    }

    /**
     * 复制 configgen genjava 源文件到指定目录
     * 仅更新缺失文件或与另一种注解模式下的模板一致的文件。
     */
    private void copyConfigGenSourcesIfNeed(CachedFiles outputFiles) throws IOException {
        if (configgenDir == null || configgenDir.isEmpty()) {
            return;
        }

        Path configgenPath = Path.of(configgenDir);
        Path targetDir = configgenPath.resolve("configgen/genjava");

        Charset charset = Charset.forName(encoding);
        for (String fn : COPY_FILES) {
            String annotated;
            try (var source = FileUtil.openSupportSource("/support/configgen/genjava/" + fn,
                    "src/main/java/configgen/genjava/" + fn);
                 var reader = new BufferedReader(new UnicodeReader(source, StandardCharsets.UTF_8))) {
                var contents = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    contents.append(line).append('\n');
                }
                annotated = contents.toString();
            }
            String plain = withoutJSpecify(fn, annotated);
            Path target = targetDir.resolve(fn);
            byte[] desired = (jspecify ? annotated : plain).getBytes(charset);
            if (!Files.exists(target) || (!annotated.equals(plain) && Files.isRegularFile(target) && Files.readString(target, charset)
                    .replace("\r\n", "\n").equals(jspecify ? plain : annotated))) {
                outputFiles.writeFile(target, desired);
            } else {
                outputFiles.keepFile(target);
            }
        }
        Path marker = targetDir.resolve("package-info.java");
        String contents = packageInfo("configgen.genjava");
        if (jspecify && !Files.exists(marker)) {
            outputFiles.writeFile(marker, contents.getBytes(charset));
        } else if (!jspecify && Files.isRegularFile(marker) &&
                Files.readString(marker, charset).replace("\r\n", "\n").equals(contents)) {
            Files.deleteIfExists(marker);
        } else if (Files.exists(marker)) {
            outputFiles.keepFile(marker);
        }
    }

    private static String withoutJSpecify(String filename, String source) {
        String plain = source.replace("import org.jspecify.annotations.Nullable;\n\n", "")
                .replace(" @Nullable []", "[]")
                .replace("@Nullable ", "");
        // 保留旧版支持源码中的实现，包括启用空值注解前的检查与局部变量用法。
        return switch (filename) {
            case "BytesInspector.java" -> plain
                    .replace("java.util.Objects.requireNonNull(rootSchema)", "rootSchema")
                    .replace("java.util.Objects.requireNonNull(schema)", "schema")
                    .replace("java.util.Objects.requireNonNull(implSchema)", "implSchema")
                    .replace("java.util.Objects.requireNonNull(deRef)", "deRef");
            case "CodeDataInspector.java" -> plain
                    .replace("switch (java.util.Objects.requireNonNull(target))", "switch (target)");
            case "ConfigInput.java" -> plain
                    .replace("        LangTextPool[] pools = java.util.Objects.requireNonNull(langTextPools);\n", "")
                    .replace("new String[pools.length]", "new String[langTextPools.length]")
                    .replace("i < pools.length; i++", "i < langTextPools.length; i++")
                    .replace("texts[i] = pools[i].getText(index);", "texts[i] = textInPool(i, index);")
                    .replace("java.util.Objects.requireNonNull(langTextPools)[language]", "langTextPools[language]");
            default -> plain;
        };
    }

    private static String packageInfo(String packageName) {
        return "@NullMarked\npackage " + packageName + ";\n\nimport org.jspecify.annotations.NullMarked;\n";
    }

    private void generateStructClass(GenCfg cfg, StructSchema struct, Function<String, CachedIndentPrinter> openCode) {
        NameableName name = new NameableName(cfg, struct);
        try (var ps = openCode.apply(name.path)) {
            StructuralClassModel model = new StructuralClassModel(cfg, struct, name, false,
                    SourceComment.of(struct, null));
            JteEngine.render("java/GenStructuralClass.jte", model, ps);
        }
    }

    private void generateInterfaceClass(GenCfg cfg, InterfaceSchema interfaceSchema,
                                        Function<String, CachedIndentPrinter> openCode) {
        NameableName name = new NameableName(cfg, interfaceSchema);
        try (CachedIndentPrinter ps = openCode.apply(name.path)) {
            InterfaceModel model = new InterfaceModel(cfg, interfaceSchema, name);
            JteEngine.render("java/GenInterface.jte", model, ps);
        }
    }

    private void generateTableClass(GenCfg cfg, VTable vTable, CfgData cfgData,
                                    Function<String, CachedIndentPrinter> openCode,
                                    boolean isTableNeedBuilder, List<NameableName> tableDataNames, List<String> setAllRefsInMgrLoader) {
        boolean isNeedReadData = true;
        String dataPostfix = "";
        TableSchema schema = vTable.schema();
        // 该表数据来源的原始文件路径（xlsx/csv 或其 sheet），写到生成类顶部方便反查源文件
        CfgData.DTable dTable = cfgData.getDTable(vTable.name());
        List<String> rawSheetIds = (dTable == null) ? List.of()
                : dTable.rawSheets().stream().map(CfgData.DRawSheet::id).toList();
        String sourceComment = SourceComment.of(schema, rawSheetIds);
        if (schema.entry() instanceof EntryType.EntryBase entryBase) {
            String entryPostfix = "";
            boolean isEnum = entryBase instanceof EntryType.EEnum;
            if (isEnum) {
                if (!GenJavaUtil.needsTableData(schema)) {
                    isNeedReadData = false;
                } else {
                    dataPostfix = "_Detail";
                }
            } else {
                entryPostfix = "_Entry";
            }

            NameableName name = new NameableName(cfg, schema, entryPostfix);
            if (isNeedReadData) {
                setAllRefsInMgrLoader.add(name.fullName);
            }
            NameableName dataName = new NameableName(cfg, schema, dataPostfix);
            try (var ps = openCode.apply(name.path)) {
                JteEngine.render("java/GenEntryOrEnumClass.jte",
                        new EntryOrEnumModel(cfg, vTable, entryBase, name, isNeedReadData, dataName, sourceComment), ps);
            }
        }

        if (isNeedReadData) {
            NameableName name = new NameableName(cfg, schema, dataPostfix);
            tableDataNames.add(name);
            StructuralClassModel model = new StructuralClassModel(cfg, schema, name, isTableNeedBuilder, sourceComment);
            try (var ps = openCode.apply(name.path)) {
                JteEngine.render("java/GenStructuralClass.jte", model, ps);
            }

            if (isTableNeedBuilder) {
                String builder = name.path.substring(0, name.path.length() - 5) + "Builder.java";
                try (var ps = openCode.apply(builder)) {
                    JteEngine.render("java/GenTableBuilder.jte", model, ps);
                }
            }

        }
    }

}

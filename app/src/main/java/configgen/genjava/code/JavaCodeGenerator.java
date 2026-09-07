package configgen.genjava.code;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static configgen.value.CfgValue.VTable;

public class JavaCodeGenerator extends GeneratorWithTag {
    private final String dir;
    private final String pkg;
    private final String encoding;
    private final boolean sealed;
    private final String buildersFilename;
    private final String configgenDir; // 新增：configgen genjava 源文件复制目录
    private final boolean beautifulName; // 美化由 snake_case schema 名派生的标识符（类名/getter 转 PascalCase、enum 常量转 SCREAMING_SNAKE_CASE），默认 false 保持老行为
    private final String prefix; // 生成类名的前缀，同 csharp 的 prefix，默认为空
    private Set<String> needBuilderTables = null;
    private final int schemaNumPerFile;

    private Path dstDir;
    private CfgData cfgData;
    private CachedFiles outputFiles;
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
        buildersFilename = parameter.get("builders", null);
        configgenDir = parameter.get("configgenDir", null);
        schemaNumPerFile = Integer.parseInt(parameter.get("schemaNumPerFile", "100"));
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
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        cfgData = ctx.cfgData();
        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));
        outputFiles = ctx.outputFiles();

        // 一次生成的固定配置：不可变、随调用链显式传递，替代原先散落在 Name/TypeStr/NameableName 的
        // static 字段（并发生成时会互相踩踏）
        GenCfg cfg = new GenCfg(pkg, sealed, beautifulName, ctx.nullableLangSwitch() != null, prefix);

        List<NameableName> tableDataNames = new ArrayList<>();
        List<String> setAllRefsInMgrLoader = new ArrayList<>();

        if (buildersFilename != null) {
            readNeedBuilderTables();
        }
        // struct/interface 类与 table 类各自生成独立文件，互不依赖；并发渲染。
        // tableDataNames / setAllRefs 顺序敏感——故每个 table 任务用独立 local 列表，渲染后按原序合并，保证字节级一致。
        List<Callable<Void>> structTasks = new ArrayList<>();
        for (Nameable nameable : cfgValue.schema().items()) {
            switch (nameable) {
                case StructSchema s -> structTasks.add(() -> {
                    generateStructClass(cfg, s);
                    return null;
                });
                case InterfaceSchema iface -> {
                    final InterfaceSchema ifaceF = iface;
                    // interface 连同其 impls 放一个任务：二者可能同名同包（如 Effect），
                    // 串行下 impl 后写覆盖 interface；任务内保持先 interface 后 impls 的顺序，避免并发竞态写反。
                    structTasks.add(() -> {
                        generateInterfaceClass(cfg, ifaceF);
                        for (StructSchema impl : ifaceF.impls()) {
                            generateStructClass(cfg, impl);
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
                generateTableClass(cfg, vt, localDataNames, localSetAllRefs);
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

        if (cfg.isLangSwitch()) {
            try (var ps = createCode("Text.java")) {
                JteEngine.render("java/Text.jte",
                        new TextModel(pkg, ctx.nullableLangSwitch().languages()), ps);
            }
        }

        try (var ps = createCode("ConfigMgr.java")) {
            JteEngine.render("java/ConfigMgr.jte",
                    Map.of("pkg", cfg.codeTopPkg(), "tableDataNames", tableDataNames), ps);
        }

        try (var ps = createCode("ConfigLoader.java")) {
            JteEngine.render("java/ConfigLoader.jte",
                    Map.of("pkg", cfg.codeTopPkg()), ps);
        }

        try (var ps = createCode("ConfigMgrLoader.java")) {
            JteEngine.render("java/ConfigMgrLoader.jte",
                    new ConfigMgrLoaderModel(cfg, cfgValue, setAllRefsInMgrLoader), ps);
        }

        GenConfigCodeSchema.generateAll(this, cfg, schemaNumPerFile, cfgValue, ctx.nullableLangSwitch());

        outputFiles.deleteOtherFiles(dstDir.toFile());

        copyConfigGenSourcesIfNeed();
    }

    // 单个 table 任务的并发产物：本任务收集的 dataName（供 ConfigMgr 渲染成员）与 setAllRefs 类名
    private record TableRefs(List<NameableName> dataNames, List<String> setAllRefs) {
    }

    private void readNeedBuilderTables() {
        Path fn = Path.of(buildersFilename).normalize();
        if (Files.exists(fn)) {
            try {
                needBuilderTables = new HashSet<>();
                List<String> lines = Files.readAllLines(fn, StandardCharsets.UTF_8);
                needBuilderTables.addAll(lines);
            } catch (IOException e) {
                Logger.log("读文件异常, 忽略此文件", fn.toAbsolutePath());
            }
        }
    }

    /**
     * 复制 configgen genjava 源文件到指定目录
     * 如果目标目录已存在则跳过，保护用户可能的修改
     */
    private void copyConfigGenSourcesIfNeed() throws IOException {
        if (configgenDir == null || configgenDir.isEmpty()) {
            return;
        }

        Path configgenPath = Path.of(configgenDir);
        Path targetDir = configgenPath.resolve("configgen/genjava");

        // 逐个复制文件
        for (String fn : COPY_FILES) {
            FileUtil.copyFileIfNotExist("/support/configgen/genjava/" + fn,
                    "src/main/java/configgen/genjava/" + fn,
                    targetDir.resolve(fn), encoding, outputFiles);
        }
    }

    CachedIndentPrinter createCode(String fn) {
        return mainCc.get().printer(dstDir.resolve(fn), encoding, outputFiles);
    }

    private void generateStructClass(GenCfg cfg, StructSchema struct) {
        NameableName name = new NameableName(cfg, struct);
        try (var ps = createCode(name.path)) {
            StructuralClassModel model = new StructuralClassModel(cfg, struct, name, false,
                    SourceComment.of(struct, null));
            JteEngine.render("java/GenStructuralClass.jte", model, ps);
        }
    }

    private void generateInterfaceClass(GenCfg cfg, InterfaceSchema interfaceSchema) {
        NameableName name = new NameableName(cfg, interfaceSchema);
        try (CachedIndentPrinter ps = createCode(name.path)) {
            InterfaceModel model = new InterfaceModel(cfg, interfaceSchema, name);
            JteEngine.render("java/GenInterface.jte", model, ps);
        }
    }

    private void generateTableClass(GenCfg cfg, VTable vTable, List<NameableName> tableDataNames, List<String> setAllRefsInMgrLoader) {
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
                if (GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(schema)) {
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
            try (var ps = createCode(name.path)) {
                JteEngine.render("java/GenEntryOrEnumClass.jte",
                        new EntryOrEnumModel(cfg, vTable, entryBase, name, isNeedReadData, dataName, sourceComment), ps);
            }
        }

        if (isNeedReadData) {
            NameableName name = new NameableName(cfg, schema, dataPostfix);
            tableDataNames.add(name);
            boolean isTableNeedBuilder = needBuilderTables != null && needBuilderTables.contains(vTable.name());
            try (var ps = createCode(name.path)) {
                StructuralClassModel model = new StructuralClassModel(cfg, vTable.schema(), name, isTableNeedBuilder,
                        sourceComment);
                JteEngine.render("java/GenStructuralClass.jte", model, ps);
            }

            if (isTableNeedBuilder) {
                String builder = name.path.substring(0, name.path.length() - 5) + "Builder.java";
                try (var ps = createCode(builder)) {
                    JteEngine.render("java/GenTableBuilder.jte",
                            Map.of("table", vTable.schema(), "name", name), ps);
                }
            }

        }
    }

}

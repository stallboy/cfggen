package configgen.genjava.code;

import configgen.ctx.Context;
import configgen.data.CfgData;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.value.CfgValue.VTable;

public class JavaCodeGenerator extends GeneratorWithTag {
    private final String dir;
    private final String pkg;
    private final String encoding;
    private final boolean sealed;
    private final String buildersFilename;
    private final String configgenDir; // 新增：configgen genjava 源文件复制目录
    private final boolean snakeEnumName; // enum/entry 常量字段名用 SCREAMING_SNAKE_CASE，默认 false 保持老行为（toUpperCase）
    private Set<String> needBuilderTables = null;
    private final int schemaNumPerFile;

    private Path dstDir;
    private CfgData cfgData;
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

            "BytesInspector.java",
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
        snakeEnumName = parameter.has("snakeEnumName");
    }


    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        cfgData = ctx.cfgData();
        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));

        Name.codeTopPkg = pkg;
        NameableName.isSealedInterface = sealed;
        Name.snakeEnumName = snakeEnumName;
        boolean isLangSwitch = ctx.nullableLangSwitch() != null;
        TypeStr.isLangSwitch = isLangSwitch; //辅助 Text的类型声明和创建

        List<String> mapsInMgr = new ArrayList<>();
        List<String> setAllRefsInMgrLoader = new ArrayList<>();

        if (buildersFilename != null) {
            readNeedBuilderTables();
        }
        // struct/interface 类与 table 类各自生成独立文件，互不依赖；并发渲染。
        // mapsInMgr / setAllRefs 在模板渲染时被 add，且被后续 ConfigMgr/ConfigMgrLoader 消费、顺序敏感——
        // 故每个任务用独立的 local 列表，渲染后按原序合并，保证字节级一致。
        List<Callable<List<String>>> structTasks = new ArrayList<>();
        for (Nameable nameable : cfgValue.schema().items()) {
            switch (nameable) {
                case StructSchema s -> structTasks.add(() -> {
                    List<String> local = new ArrayList<>();
                    generateStructClass(s, local);
                    return local;
                });
                case InterfaceSchema iface -> {
                    final InterfaceSchema ifaceF = iface;
                    // interface 连同其 impls 放一个任务：二者可能同名同包（如 Effect），
                    // 串行下 impl 后写覆盖 interface；任务内保持先 interface 后 impls 的顺序，避免并发竞态写反。
                    structTasks.add(() -> {
                        generateInterfaceClass(ifaceF);
                        List<String> local = new ArrayList<>();
                        for (StructSchema impl : ifaceF.impls()) {
                            generateStructClass(impl, local);
                        }
                        return local;
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
                List<String> localMaps = new ArrayList<>();
                List<String> localSetAllRefs = new ArrayList<>();
                generateTableClass(vt, localMaps, localSetAllRefs);
                return new TableRefs(localMaps, localSetAllRefs);
            });
        }

        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            // 两个 invokeAll 屏障：struct 阶段先于 table 阶段完成，保持 mapsInMgr 里 struct 项先于 table 项的顺序
            for (Future<List<String>> f : executor.invokeAll(structTasks)) {
                mapsInMgr.addAll(f.get());
            }
            for (Future<TableRefs> f : executor.invokeAll(tableTasks)) {
                TableRefs r = f.get();
                mapsInMgr.addAll(r.maps);
                setAllRefsInMgrLoader.addAll(r.setAllRefs);
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (isLangSwitch) {
            try (var ps = createCode("Text.java")) {
                JteEngine.render("java/Text.jte",
                        new TextModel(pkg, ctx.nullableLangSwitch().languages()), ps);
            }
        }

        try (var ps = createCode("ConfigMgr.java")) {
            JteEngine.render("java/ConfigMgr.jte",
                    Map.of("pkg", Name.codeTopPkg, "mapsInMgr", mapsInMgr), ps);
        }

        try (var ps = createCode("ConfigLoader.java")) {
            JteEngine.render("java/ConfigLoader.jte",
                    Map.of("pkg", Name.codeTopPkg), ps);
        }

        try (var ps = createCode("ConfigMgrLoader.java")) {
            JteEngine.render("java/ConfigMgrLoader.jte",
                    new ConfigMgrLoaderModel(cfgValue, setAllRefsInMgrLoader), ps);
        }

        GenConfigCodeSchema.generateAll(this, schemaNumPerFile, cfgValue, ctx.nullableLangSwitch());

        CachedFiles.deleteOtherFiles(dstDir.toFile());

        copyConfigGenSourcesIfNeed();
    }

    // 单个 table 任务的并发产物：本任务往 mapsInMgr / setAllRefsInMgrLoader 累加的项
    private record TableRefs(List<String> maps, List<String> setAllRefs) {
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
                    targetDir.resolve(fn), encoding);
        }
    }

    CachedIndentPrinter createCode(String fn) {
        return mainCc.get().printer(dstDir.resolve(fn), encoding);
    }

    private void generateStructClass(StructSchema struct, List<String> mapsInMgr) {
        NameableName name = new NameableName(struct);
        try (var ps = createCode(name.path)) {
            StructuralClassModel model = new StructuralClassModel(struct, name, false, mapsInMgr,
                    SourceComment.of(struct, null));
            JteEngine.render("java/GenStructuralClass.jte", model, ps);
        }
    }

    private void generateInterfaceClass(InterfaceSchema interfaceSchema) {
        NameableName name = new NameableName(interfaceSchema);
        try (CachedIndentPrinter ps = createCode(name.path)) {
            InterfaceModel model = new InterfaceModel(interfaceSchema, name);
            JteEngine.render("java/GenInterface.jte", model, ps);
        }
    }

    private void generateTableClass(VTable vTable, List<String> mapsInMgr, List<String> setAllRefsInMgrLoader) {
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

            NameableName name = new NameableName(schema, entryPostfix);
            if (isNeedReadData) {
                setAllRefsInMgrLoader.add(name.fullName);
            }
            NameableName dataName = new NameableName(schema, dataPostfix);
            try (var ps = createCode(name.path)) {
                JteEngine.render("java/GenEntryOrEnumClass.jte",
                        new EntryOrEnumModel(vTable, entryBase, name, isNeedReadData, dataName, sourceComment), ps);
            }
        }

        if (isNeedReadData) {
            NameableName name = new NameableName(schema, dataPostfix);
            boolean isTableNeedBuilder = needBuilderTables != null && needBuilderTables.contains(vTable.name());
            try (var ps = createCode(name.path)) {
                StructuralClassModel model = new StructuralClassModel(vTable.schema(), name, isTableNeedBuilder,
                        mapsInMgr, sourceComment);
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

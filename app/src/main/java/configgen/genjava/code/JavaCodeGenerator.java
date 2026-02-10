package configgen.genjava.code;

import configgen.ctx.Context;
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

import static configgen.value.CfgValue.VTable;

public class JavaCodeGenerator extends GeneratorWithTag {
    private final String dir;
    private final String pkg;
    private final String encoding;
    private final boolean sealed;
    private final String buildersFilename;
    private final String configgenDir; // 新增：configgen genjava 源文件复制目录
    private Set<String> needBuilderTables = null;
    private final int schemaNumPerFile;

    private Path dstDir;
    private CacheConfig cacheConfig;

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
        sealed = parameter.has("sealed");
        buildersFilename = parameter.get("builders", null);
        configgenDir = parameter.get("configgenDir", null);
        schemaNumPerFile = Integer.parseInt(parameter.get("schemaNumPerFile", "100"));
    }


    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));
        cacheConfig = CacheConfig.of();

        Name.codeTopPkg = pkg;
        NameableName.isSealedInterface = sealed;
        boolean isLangSwitch = ctx.nullableLangSwitch() != null;
        TypeStr.isLangSwitch = isLangSwitch; //辅助 Text的类型声明和创建

        List<String> mapsInMgr = new ArrayList<>();
        List<String> setAllRefsInMgrLoader = new ArrayList<>();

        if (buildersFilename != null) {
            readNeedBuilderTables();
        }

        for (Nameable nameable : cfgValue.schema().items()) {
            switch (nameable) {
                case StructSchema structSchema -> {
                    generateStructClass(structSchema, mapsInMgr);
                }
                case InterfaceSchema interfaceSchema -> {
                    generateInterfaceClass(interfaceSchema);
                    for (StructSchema impl : interfaceSchema.impls()) {
                        generateStructClass(impl, mapsInMgr);
                    }
                }
                case TableSchema ignored -> {
                }
            }
        }
        for (VTable vtable : cfgValue.tables()) {
            generateTableClass(vtable, mapsInMgr, setAllRefsInMgrLoader);
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
            FileUtil.copyFileIfNotExist("support/configgen/genjava/" + fn,
                    "src/main/java/configgen/genjava/" + fn,
                    targetDir.resolve(fn), encoding);
        }
    }

    CachedIndentPrinter createCode(String fn) {
        return cacheConfig.printer(dstDir.resolve(fn), encoding);
    }

    private void generateStructClass(StructSchema struct, List<String> mapsInMgr) {
        NameableName name = new NameableName(struct);
        try (var ps = createCode(name.path)) {
            StructuralClassModel model = new StructuralClassModel(struct, name, false, mapsInMgr);
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
                        new EntryOrEnumModel(vTable, entryBase, name, isNeedReadData, dataName), ps);
            }
        }

        if (isNeedReadData) {
            NameableName name = new NameableName(schema, dataPostfix);
            boolean isTableNeedBuilder = needBuilderTables != null && needBuilderTables.contains(vTable.name());
            try (var ps = createCode(name.path)) {
                StructuralClassModel model = new StructuralClassModel(vTable.schema(), name, isTableNeedBuilder, mapsInMgr);
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

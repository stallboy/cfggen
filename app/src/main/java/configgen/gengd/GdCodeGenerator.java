package configgen.gengd;

import configgen.ctx.Context;
import configgen.gen.CliException;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static configgen.value.CfgValue.VTable;

public class GdCodeGenerator extends GeneratorWithTag {
    private final String dir;
    public final String prefix;

    private Path dstDir;
    private CachedFiles outputFiles;
    // 并发生成：每个工作线程独占一组打印机缓冲区，避免多线程踩踏共享 StringBuilder
    private final ThreadLocal<CacheConfig> mainCc = ThreadLocal.withInitial(CacheConfig::of);
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
        // 非法前缀会静默产出编不过的代码，这里直接报错；GDScript 标识符片段：字母或_开头，后续字母/数字/_
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            boolean ok = i == 0 ? (Character.isLetter(c) || c == '_')
                    : (Character.isLetterOrDigit(c) || c == '_');
            if (!ok) {
                throw new CliException("invalid value for parameter 'prefix': '" + prefix
                        + "' is not a valid gdscript identifier fragment");
            }
        }
    }


    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        cfgSchema = cfgValue.schema();

        // gd 不支持复合键：提前报清涉及的表并给出处理办法，而不是渲染中途抛裸异常栈
        List<String> compositeKeyTables = new ArrayList<>();
        for (VTable vTable : cfgValue.sortedTables()) {
            TableSchema s = vTable.schema();
            if (s.primaryKey().fieldSchemas().size() > 1
                    || s.uniqueKeys().stream().anyMatch(k -> k.fieldSchemas().size() > 1)) {
                compositeKeyTables.add(s.name());
            }
        }
        if (!compositeKeyTables.isEmpty()) {
            throw new CliException("gd not support composite key: " + String.join(", ", compositeKeyTables)
                    + "; add (nogd) tag to these tables or filter with own:-nogd");
        }

        dstDir = Paths.get(dir);
        outputFiles = ctx.outputFiles();

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
                    ENCODING, outputFiles);
        }

        generateProcessor(cfgSchema);

        // struct/interface/table 类各自生成独立文件，互不依赖；单阶段并发渲染。
        // 唯一共享可变状态是打印机缓冲区，已用 ThreadLocal（mainCc）按线程隔离。
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Fieldable fieldable : cfgSchema.sortedFieldables()) {
            switch (fieldable) {
                case StructSchema structSchema -> tasks.add(() -> {
                    generateStruct(structSchema);
                    return null;
                });
                case InterfaceSchema interfaceSchema -> {
                    final InterfaceSchema iface = interfaceSchema;
                    // interface 连同其 impls 放一个任务：任务内保持先 interface 后 impls 的顺序。
                    tasks.add(() -> {
                        generateInterface(iface);
                        for (StructSchema impl : iface.impls()) {
                            generateStruct(impl);
                        }
                        return null;
                    });
                }
            }
        }
        for (VTable vTable : cfgValue.sortedTables()) {
            final VTable vt = vTable;
            tasks.add(() -> {
                generateTable(vt);
                return null;
            });
        }

        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            invokeAllAndWait(executor, tasks);
        }

        outputFiles.keepMetaAndDeleteOtherFiles(dstDir.toFile());
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
        return mainCc.get().printer(dstDir.resolve(fn), ENCODING, outputFiles);
    }
}

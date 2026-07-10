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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.value.CfgValue.VTable;

public class CsCodeGenerator extends GeneratorWithTag {
    private final String dir;
    public final String pkg;
    public final String encoding;
    public final String prefix;
    public final boolean serverText;
    public final boolean unity;

    private Path dstDir;
    // 并发生成：每个工作线程独占一组打印机缓冲区，避免多线程踩踏共享 StringBuilder
    private final ThreadLocal<CacheConfig> mainCc = ThreadLocal.withInitial(CacheConfig::of);
    public boolean isLangSwitch;

    public CsCodeGenerator(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "Config");
        pkg = parameter.get("pkg", "Config");
        encoding = parameter.get("encoding", "UTF-8");
        prefix = parameter.get("prefix", "D");
        serverText = parameter.has("serverText");
        unity = parameter.has("unity");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);  // 这里只需要schema，生成value只用于检验数据
        CfgSchema cfgSchema = cfgValue.schema();

        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));

        isLangSwitch = ctx.nullableLangSwitch() != null;
        // unity 模式用 C#9 兼容的 Loader（含自定义 OrderedDictionary 等）；否则用最新特性版
        String loaderSrc = unity ? "Loader.unity.cs" : "Loader.cs";
        FileUtil.copyFileIfNotExist("/support/cs/" + loaderSrc,
                "src/main/resources/support/cs/" + loaderSrc,
                dstDir.resolve("Loader.cs"),
                encoding);

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
                    // interface 连同其 impls 放一个任务：二者可能落到同一路径，
                    // 任务内保持先 interface 后 impls 的顺序，避免并发竞态写反。
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
            for (Future<Void> f : executor.invokeAll(tasks)) {
                f.get();
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
        Map<String, Object> model = Map.of("pkg", pkg, "languages", languages, "unity", unity);
        try (var ps = createCode("Text.cs")) {
            String template = serverText ? "cs/ServerText.jte" : "cs/ClientText.jte";
            JteEngine.render(template, model, ps);
        }
    }


    private CachedIndentPrinter createCode(String fn) {
        return mainCc.get().printer(dstDir.resolve(fn), encoding);
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

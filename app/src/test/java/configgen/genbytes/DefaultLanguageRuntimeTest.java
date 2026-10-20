package configgen.genbytes;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.gengd.GdCodeGenerator;
import configgen.gengo.GoCodeGenerator;
import configgen.genjava.*;
import configgen.genjava.code.JavaCodeGenerator;
import configgen.gents.TsCodeGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLanguageRuntimeTest {
    @TempDir Path root;

    private Context fixture() throws Exception {
        var context = TestCtx.newContext(Files.createDirectories(root.resolve("data")),
                "interface Choice { struct Known { } struct Other { } } interface Solo { struct Only { } } table key_test[id] { id:int; choice:Solo (pack); } table variant[id] { id:int; choice:Choice (pack); } table containers[id] { id:int; values:list<int> (pack); pairs:map<int,int> (pack); } table item[id] { id:int; name:str; desc:text; }",
                Map.of("item", "id,name,desc\nid,name,desc\n1,Alice,你好\n", "key_test", "id,choice\nid,choice\n1,Only()\n", "variant", "id,choice\nid,choice\n1,Known()\n", "containers", "id,values,pairs\nid,values,pairs\n1,,\n"));
        new BytesGenerator(new ParameterParser("bytes,dir:" + root)).generate(context);
        var pack = ConfigPackage.read(new ConfigInput(root.resolve("config.bytes")));
        var metadata = new LinkedHashMap<>(pack.metadata());
        metadata.put("defaultLanguage", "zh_cn");
        var blocks = new LinkedHashMap<String, byte[]>();
        for (String block : pack.blockNames()) {
            var input = pack.block(block);
            blocks.put(block.equals("text/default") ? "text/zh_cn" : block, input.readRawBytes((int) input.remaining()));
        }
        try (var out = new ConfigOutput(Files.newOutputStream(root.resolve("localized.bytes")))) {
            PackageSerializer.write(out, metadata, blocks);
        }
        byte[] collections = blocks.get(".containers");
        assertEquals(16, collections.length);
        for (var entry : Map.of("list", 8, "map", 12).entrySet()) {
            byte[] invalid = collections.clone();
            java.nio.ByteBuffer.wrap(invalid).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(entry.getValue(), -1);
            blocks.put(".containers", invalid);
            try (var out = new ConfigOutput(Files.newOutputStream(root.resolve("bad-" + entry.getKey() + ".bytes")))) {
                PackageSerializer.write(out, metadata, blocks);
            }
        }
        return context;
    }

    @Test
    void javaLoaderAndModBuilderUseDependencyDefaultLanguage() throws Exception {
        Context context = fixture();
        Path project = Files.createDirectories(root.resolve("java"));
        new JavaCodeGenerator(new ParameterParser("java,dir:" + project + ",configgenDir:" + project)).generate(context);
        Path modData = Files.createDirectories(root.resolve("mod-data"));
        Files.writeString(modData.resolve("item.csv"), "id,name,desc\nid,name,desc\n2,Bob,new\n");
        new BytesGenerator(new ParameterParser("bytes,pack:mod,requires:base,deps:" + root.resolve("localized.bytes")
                + ",dir:" + root.resolve("mod"))).generate(new Context(Context.ContextCfg.of(modData), context.cfgSchema()));
        Files.writeString(project.resolve("Check.java"), """
                import config.*;
                import configgen.genjava.*;
                public class Check {
                    public static void main(String[] args) throws Exception {
                        var base = ConfigPackage.read(new ConfigInput(java.nio.file.Path.of(args[0])));
                        var mod = ConfigPackage.read(new ConfigInput(java.nio.file.Path.of(args[1])));
                        var original = ConfigMgrLoader.loadPackages(base);
                        var combined = ConfigMgrLoader.loadPackages(base, mod);
                        if (!original.getItem(1).getDesc().equals("你好") || !combined.getItem(1).getDesc().equals("你好")
                                || !combined.getItem(2).getDesc().equals("new")) throw new AssertionError("default language");
                    }
                }
                """);
        Path classes = Files.createDirectories(root.resolve("classes"));
        var arguments = new ArrayList<>(List.of("-encoding", "UTF-8", "-d", classes.toString()));
        try (var files = Files.walk(project)) { files.filter(p -> p.toString().endsWith(".java")).map(Path::toString).forEach(arguments::add); }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)));
        try (var loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            loader.loadClass("Check").getMethod("main", String[].class).invoke(null,
                    (Object) new String[]{root.resolve("localized.bytes").toString(), root.resolve("mod/config.bytes").toString()});
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ts", "go"})
    @Tag("otherRuntimes")
    void standaloneReadersUsePackageDefaultLanguage(String language) throws Exception {
        Context context = fixture();
        Path project = Files.createDirectories(root.resolve(language));
        if (language.equals("ts")) {
            new TsCodeGenerator(new ParameterParser("ts,dir:" + project)).generate(context);
            Path compiler = Path.of(System.getProperty("cfggen.tsc", "../cfgeditor/node_modules/typescript/bin/tsc")).toAbsolutePath();
            assertTrue(Files.isRegularFile(compiler), "Install cfgeditor dependencies or set -Dcfggen.tsc");
            run(project, List.of("node", compiler.toString(), "--target", "es2022", "--module", "commonjs", "--strict", "--skipLibCheck", "--outDir", "out", "Config.ts", "ConfigUtil.ts"));
            Files.writeString(project.resolve("test.cjs"), """
                    const fs = require('fs');
                    const {Config} = require('./out/Config.js');
                    const {ConfigPackage, LoadErrors, Loader} = require('./out/ConfigUtil.js');
                    const data = fs.readFileSync(process.env.CFGGEN_FIXTURE + '/localized.bytes');
                    const pack = ConfigPackage.Read(data.buffer.slice(data.byteOffset, data.byteOffset + data.length));
                    for (let i = 0; i < 2; i++) {
                        Config.Processor.ProcessPackage(pack, new LoadErrors());
                        if (Config.Item.Get(1).Desc !== '你好' || Config.Item.Get(1).Name !== 'Alice') throw Error('default language');
                    }
                    if (pack.OpenData([]).GetLangNames()[0] !== 'zh_cn') throw Error('empty language selection');
                    Loader.LoadBytes(data.buffer.slice(data.byteOffset, data.byteOffset + data.length), Config.Processor.Process, new LoadErrors());
                    if (Config.Item.Get(1).Desc !== '你好') throw Error('LoadBytes default language');
                    for (const name of ['bad-list.bytes', 'bad-map.bytes']) {
                        const raw = fs.readFileSync(process.env.CFGGEN_FIXTURE + '/' + name);
                        try {
                            Config.Processor.ProcessPackage(ConfigPackage.Read(raw.buffer.slice(raw.byteOffset, raw.byteOffset + raw.length)), new LoadErrors());
                            throw Error('accepted negative count');
                        } catch (error) { if (!error.message.includes('Invalid count')) throw error; }
                    }
                    console.log('runtime-ok');
                    """);
            run(project, List.of("node", "test.cjs"));
        } else {
            new GoCodeGenerator(new ParameterParser("go,dir:" + project)).generate(context);
            Files.writeString(project.resolve("go.mod"), "module fixture\n\ngo 1.22\n");
            Files.writeString(project.resolve("config/default_test.go"), """
                    package config
                    import ("testing"; "os"; "path/filepath"; "bytes"; "strings"; "fmt")
                    func TestDefaultLanguage(t *testing.T) {
                        data, err := os.ReadFile(filepath.Join(os.Getenv("CFGGEN_FIXTURE"), "localized.bytes")); if err != nil { t.Fatal(err) }
                        pack := ReadPackage(data)
                        for i := 0; i < 2; i++ {
                            InitPackage(pack)
                            if GetItemMgr().Get(1).Desc() != "你好" || GetItemMgr().Get(1).Name() != "Alice" { t.Fatal("default language") }
                        }
                    }
                    func TestSkipBytesDoesNotAllocate(t *testing.T) {
                        data := make([]byte, 1024)
                        reader := bytes.NewReader(data)
                        stream := &Stream{reader: reader}
                        if count := testing.AllocsPerRun(100, func() { reader.Reset(data); stream.SkipBytes(len(data)) }); count != 0 {
                            t.Fatalf("skip allocated %v objects", count)
                        }
                        reader.Reset([]byte{1,0,0,0,2,0,0,0})
                        stream.SkipBytes(4)
                        if stream.ReadInt32() != 2 { t.Fatal("skip position") }
                        reader.Reset([]byte{2,0,0,0})
                        if stream.ReadCount(0) != 2 { t.Fatal("zero-byte items") }
                    }
                    func TestNegativeCounts(t *testing.T) {
                        for _, name := range []string{"bad-list.bytes", "bad-map.bytes"} {
                            func() {
                                defer func() { failure := recover(); if failure == nil || !strings.Contains(fmt.Sprint(failure), "negative count") { t.Fatalf("invalid count was not rejected: %v", failure) } }()
                                data, err := os.ReadFile(filepath.Join(os.Getenv("CFGGEN_FIXTURE"), name)); if err != nil { t.Fatal(err) }
                                InitPackage(ReadPackage(data))
                            }()
                        }
                    }
                    """);
            run(project, List.of("go", "test", "./..."));
        }
    }

    @Test
    @Tag("godot")
    void godotUsesPackageDefaultLanguage() throws Exception {
        Context context = fixture();
        Path project = Files.createDirectories(root.resolve("gd"));
        new GdCodeGenerator(new ParameterParser("gd,dir:" + project.resolve("config"))).generate(context);
        Files.writeString(project.resolve("project.godot"), "config_version=5\n[application]\nconfig/name=\"Default Language Test\"\n");
        Files.writeString(project.resolve("test.gd"), """
                extends SceneTree
                func _initialize():
                    call_deferred("run_test")
                func run_test():
                    var count_stream = ConfigStream.new(PackedByteArray([2, 0, 0, 0]))
                    if count_stream.read_count(0) != 2:
                        quit(1)
                        return
                    var package = ConfigPackage.read_file(OS.get_environment("CFGGEN_FIXTURE").path_join("localized.bytes"))
                    for i in range(2):
                        var stream = ConfigProcessor.new().load_from_package(package, ConfigErrors.new())
                        if stream == null or DataItem.find(1).desc != "你好" or DataItem.find(1).name != "Alice":
                            push_error("default language")
                            quit(1)
                            return
                    for filename in ["bad-list.bytes", "bad-map.bytes"]:
                        var invalid = ConfigPackage.read_file(OS.get_environment("CFGGEN_FIXTURE").path_join(filename))
                        if ConfigProcessor.new().load_from_package(invalid, ConfigErrors.new()) != null:
                            push_error("accepted negative count")
                            quit(1)
                            return
                    print("runtime-ok")
                    quit(0)
                """);
        Path godot = Path.of(System.getProperty("cfggen.godot", "build/godot-runtime/Godot_v4.6-stable_win64_console.exe")).toAbsolutePath();
        assertTrue(Files.isRegularFile(godot), "Set -Dcfggen.godot to a Godot executable");
        run(project, List.of(godot.toString(), "--headless", "--path", project.toString(), "--editor", "--import", "--quit"));
        run(project, List.of(godot.toString(), "--headless", "--path", project.toString(), "--script", "test.gd"));
        assertTrue(Files.readString(project.resolve("runtime.log")).contains("runtime-ok"));
    }

    private void run(Path project, List<String> command) throws Exception {
        Path log = project.resolve("runtime.log");
        var builder = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("CFGGEN_FIXTURE", root.toString());
        Process process = builder.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Runtime timed out: " + command); }
        String output = Files.readString(log);
        assertEquals(0, process.exitValue(), output);
        assertFalse(output.contains("SCRIPT ERROR"), output);
    }
}

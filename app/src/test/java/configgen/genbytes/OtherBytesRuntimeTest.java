package configgen.genbytes;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.gengo.GoCodeGenerator;
import configgen.gents.TsCodeGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Real TS/Go compilers and runtimes; run with otherRuntimeTest after installing cfgeditor's dev dependencies. */
@Tag("otherRuntimes")
class OtherBytesRuntimeTest {
    @TempDir Path root;

    @ParameterizedTest
    @CsvSource({"ts,false", "ts,true", "go,false", "go,true"})
    void readsPackagesWithSeparatedLanguages(String language, boolean server) throws Exception {
        Path source = Files.createDirectories(root.resolve("source"));
        Path texts = Files.createDirectories(root.resolve("languages"));
        Files.writeString(texts.resolve("en.csv"), "item,你好,hello\n");
        Context context = TestCtx.newLangSwitchContext(source, "table item[id] { id:int; name:str; desc:text; }",
                Map.of("item", "id,name,desc\nid,name,desc\n1,Alice,你好\n2,Bob,你好\n"), texts, "zh_cn");
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("initial"))).generate(context);
        Files.writeString(source.resolve("item.csv"), "id,name,desc\nid,name,desc\n1,Alice,你好\n");
        new BytesGenerator(new ParameterParser("bytes,pack:base,langSeparated,dir:" + root.resolve("pack"))).generate(new Context(context.contextCfg()));
        writeMalformedVariants();
        Path project = Files.createDirectories(root.resolve("project"));
        if (language.equals("ts")) {
            new TsCodeGenerator(new ParameterParser("ts,dir:" + project + (server ? ",serverText" : ""))).generate(context);
            if (server) assertFalse(Files.readString(project.resolve("Config.ts")).contains("ReadTextsInPool()"));
            Path compiler = Path.of(System.getProperty("cfggen.tsc", "../cfgeditor/node_modules/typescript/bin/tsc")).toAbsolutePath();
            assertTrue(Files.isRegularFile(compiler), "Install cfgeditor dependencies or set -Dcfggen.tsc");
            run(project, List.of("node", compiler.toString(), "--target", "es2022", "--module", "commonjs", "--strict", "--skipLibCheck", "--outDir", "out", "Config.ts", "ConfigUtil.ts"));
            String script = """
                    const fs = require('fs'); const path = require('path');
                    const {Config} = require('./out/Config.js'); const {Loader, ConfigPackage, LoadErrors, TextPoolManager} = require('./out/ConfigUtil.js');
                    const read = name => { const b = fs.readFileSync(path.join(process.env.CFGGEN_FIXTURE, name)); return b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength); };
                    const check = value => { if (!value) throw Error('runtime assertion failed'); };
                    let stream = Loader.LoadBytes(read('initial/config.bytes'), Config.Processor.Process, new LoadErrors(), ['zh_cn', 'en']);
                    TextPoolManager.SetGlobalTexts(stream.GetLangTextPools()[0]);
                    check(Config.Item.Get(1).Name === 'Alice'); check(CHINESE === '你好');
                    TextPoolManager.SetGlobalTexts(stream.GetLangTextPools()[1]); check(ENGLISH === 'hello');
                    const pack = ConfigPackage.Read(read('pack/config.bytes')).Attach(ConfigPackage.Read(read('pack/en.bytes')));
                    stream = Config.Processor.ProcessPackage(pack, new LoadErrors());
                    check(Config.Item.Get(2) === undefined);
                    TextPoolManager.SetGlobalTexts(stream.GetLangTextPools()[0]); check(CHINESE === '你好');
                    TextPoolManager.SetGlobalTexts(stream.GetLangTextPools()[1]); check(ENGLISH === 'hello');
                    const reject = (action, message) => { try { action(); throw Error('accepted invalid data'); } catch (e) { check(e.message.includes(message)); } };
                    const attach = file => ConfigPackage.Read(read(file)).Attach(ConfigPackage.Read(read('pack/en.bytes')));
                    reject(() => Config.Processor.ProcessPackage(attach('pack/incomplete.bytes'), new LoadErrors()), 'Missing table');
                    check(Config.Item.Get(1).Name === 'Alice');
                    reject(() => Config.Processor.ProcessPackage(attach('pack/bad-text.bytes'), new LoadErrors()), 'text');
                    const bytes = read('pack/config.bytes'); new DataView(bytes).setInt32(0, 99, true);
                    try { ConfigPackage.Read(bytes); throw Error('accepted version'); } catch (e) { check(e.message.includes('version 99')); }
                    try { ConfigPackage.Read(read('pack/config.bytes').slice(0, -1)); throw Error('accepted truncation'); } catch (e) { check(!e.message.includes('accepted truncation')); }
                    console.log('runtime-ok');
                    """;
            script = script.replace("CHINESE", "Config.Item.Get(1).Desc." + (server ? "zh_cn" : "T"))
                    .replace("ENGLISH", "Config.Item.Get(1).Desc." + (server ? "en" : "T"));
            Files.writeString(project.resolve("test.cjs"), script);
            run(project, List.of("node", "test.cjs"));
        } else {
            new GoCodeGenerator(new ParameterParser("go,dir:" + project + (server ? ",serverText" : ""))).generate(context);
            if (server) assertFalse(Files.readString(project.resolve("config/Text.go")).contains("ReadTextsInPool()"));
            Files.writeString(project.resolve("go.mod"), "module fixture\n\ngo 1.22\n");
            String script = """
                    package config
                    import ("testing"; "os"; "path/filepath"; "bytes"; "encoding/binary"; "strings")
                    func TestPackages(t *testing.T) {
                        read := func(name string) []byte { data, err := os.ReadFile(filepath.Join(os.Getenv("CFGGEN_FIXTURE"), name)); if err != nil { t.Fatal(err) }; return data }
                        stream := Init(bytes.NewReader(read("initial/config.bytes")))
                        CLIENT_SET_ZERO
                        if GetItemMgr().Get(1).Name() != "Alice" || CHINESE != "你好" { t.Fatal("initial decode") }
                        pack := ReadPackage(read("pack/config.bytes")).Attach(ReadPackage(read("pack/en.bytes")))
                        stream = InitPackage(pack)
                        if GetItemMgr().Get(2) != nil { t.Fatal("previous records survived standalone replacement") }
                        CLIENT_SET_ZERO
                        if CHINESE != "你好" { t.Fatal("package native text") }
                        CLIENT_SET_ONE
                        if ENGLISH != "hello" { t.Fatal("package translated text") }
                        rejectOperation := func(action func(), message string) { defer func() { failure := recover(); if failure == nil || !strings.Contains(fmt.Sprint(failure), message) { t.Fatalf("unexpected failure: %v", failure) } }(); action() }
                        attach := func(name string) *ConfigPackage { return ReadPackage(read(name)).Attach(ReadPackage(read("pack/en.bytes"))) }
                        rejectOperation(func() { InitPackage(attach("pack/incomplete.bytes")) }, "missing table")
                        if GetItemMgr().Get(1).Name() != "Alice" { t.Fatal("incomplete package changed old data") }
                        rejectOperation(func() { InitPackage(attach("pack/bad-text.bytes")) }, "text")
                        _ = stream
                        rejected := func(data []byte, message string) { defer func() { failure := recover(); if failure == nil || (message != "" && !strings.Contains(failure.(string), message)) { t.Fatalf("invalid failure: %v", failure) } }(); ReadPackage(data) }
                        data := read("pack/config.bytes"); binary.LittleEndian.PutUint32(data, 99)
                        rejected(data, "version 99")
                        data = read("pack/config.bytes"); rejected(data[:len(data)-1], "")
                    }
                    """;
            // Version errors use formatted strings; all malformed reads panic, preserving the existing Go API convention.
            script = script.replace("failure.(string)", "fmt.Sprint(failure)").replace("\"strings\")", "\"strings\"; \"fmt\")")
                    .replace("CHINESE", "GetItemMgr().Get(1).Desc()." + (server ? "Zh_cn" : "T()"))
                    .replace("ENGLISH", "GetItemMgr().Get(1).Desc()." + (server ? "En" : "T()"))
                    .replace("CLIENT_SET_ZERO", server ? "" : "TextPoolManagerInstance().SetGlobalTexts(stream.GetLangTextPools()[0])")
                    .replace("CLIENT_SET_ONE", server ? "" : "TextPoolManagerInstance().SetGlobalTexts(stream.GetLangTextPools()[1])");
            Files.writeString(project.resolve("config/package_test.go"), script);
            run(project, List.of("go", "test", "./..."));
        }
    }

    private void writeMalformedVariants() throws Exception {
        var pack = configgen.genjava.ConfigPackage.read(new configgen.genjava.ConfigInput(root.resolve("pack/config.bytes")));
        var blocks = new java.util.LinkedHashMap<String, byte[]>();
        for (String name : pack.blockNames()) {
            var input = pack.block(name);
            blocks.put(name, input.readRawBytes((int) input.remaining()));
        }
        byte[] table = blocks.remove(".item");
        try (var out = new configgen.genjava.ConfigOutput(Files.newOutputStream(root.resolve("pack/incomplete.bytes")))) {
            PackageSerializer.write(out, pack.metadata(), blocks);
        }
        java.nio.ByteBuffer.wrap(table).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(table.length - 4, 999);
        blocks.put(".item", table);
        try (var out = new configgen.genjava.ConfigOutput(Files.newOutputStream(root.resolve("pack/bad-text.bytes")))) {
            PackageSerializer.write(out, pack.metadata(), blocks);
        }
    }

    private void run(Path project, List<String> command) throws Exception {
        Path log = project.resolve("runtime.log");
        ProcessBuilder builder = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("CFGGEN_FIXTURE", root.toString());
        Process process = builder.start();
        if (!process.waitFor(90, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Runtime timed out: " + command); }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }
}

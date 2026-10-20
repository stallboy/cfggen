package configgen.gengd;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("godot")
class GdRuntimeTest {
    @TempDir Path root;

    @Test
    void loadsPackagesAndSeparatedTextResourcesInGodot() throws Exception {
        Path source = Files.createDirectories(root.resolve("source"));
        Path texts = Files.createDirectories(root.resolve("languages"));
        Files.writeString(texts.resolve("en.csv"), "item,你好,hello\n");
        Context context = TestCtx.newLangSwitchContext(source, "interface Choice { struct Known { } } table variant[id] { id:int; choice:Choice (pack); } table item[id] { id:int; name:str; desc:text; target:int ->target; } table target[id] { id:int; power:int; }",
                Map.of("item", "id,name,desc,target\nid,name,desc,target\n1,Alice,你好,1\n2,Bob,你好,1\n", "target", "id,power\nid,power\n1,10\n", "variant", "id,choice\nid,choice\n1,Known()\n"), texts, "zh_cn");
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("initial"))).generate(context);
        Files.writeString(source.resolve("item.csv"), "id,name,desc,target\nid,name,desc,target\n1,Alice,你好,1\n");
        new BytesGenerator(new ParameterParser("bytes,pack:base,langSeparated,dir:" + root.resolve("pack"))).generate(new Context(context.contextCfg()));
        var initial = configgen.genjava.ConfigPackage.read(new configgen.genjava.ConfigInput(root.resolve("initial/config.bytes")));
        var blocks = new java.util.LinkedHashMap<String, byte[]>();
        for (String block : initial.blockNames()) {
            if (block.equals(".target")) continue;
            var input = initial.block(block);
            blocks.put(block, input.readRawBytes((int) input.remaining()));
        }
        try (var output = new configgen.genjava.ConfigOutput(Files.newOutputStream(root.resolve("incomplete.bytes")))) {
            configgen.genbytes.PackageSerializer.write(output, initial.metadata(), blocks);
        }
        var full = configgen.genjava.ConfigPackage.read(new configgen.genjava.ConfigInput(root.resolve("pack/config.bytes")))
                .attach(configgen.genjava.ConfigPackage.read(new configgen.genjava.ConfigInput(root.resolve("pack/en.bytes"))));
        blocks.clear();
        for (String block : full.blockNames()) {
            var input = full.block(block);
            blocks.put(block, input.readRawBytes((int) input.remaining()));
        }
        var order = java.nio.ByteOrder.LITTLE_ENDIAN;
        java.nio.ByteBuffer.wrap(blocks.get(".variant")).order(order).putInt(8, java.nio.ByteBuffer.wrap(blocks.get(".item")).order(order).getInt(8));
        try (var output = new configgen.genjava.ConfigOutput(Files.newOutputStream(root.resolve("bad-variant.bytes")))) {
            configgen.genbytes.PackageSerializer.write(output, full.metadata(), blocks);
        }
        Path project = Files.createDirectories(root.resolve("project"));
        new GdCodeGenerator(new ParameterParser("gd,dir:" + project.resolve("config"))).generate(context);
        Files.writeString(project.resolve("project.godot"), "config_version=5\n[application]\nconfig/name=\"Config Runtime Test\"\n");
        Files.writeString(project.resolve("test.gd"), """
                extends SceneTree
                var failed: bool = false
                func _initialize():
                    call_deferred("run_test")
                func check(value: bool):
                    if not value:
                        failed = true
                        push_error("runtime assertion failed")
                func run_test():
                    var root = OS.get_environment("CFGGEN_FIXTURE")
                    var errors = ConfigErrors.new()
                    var processor = ConfigProcessor.new()
                    var stream = processor.load_from_package(ConfigPackage.read_file(root.path_join("initial/config.bytes")), errors)
                    check(stream != null)
                    TextPoolManager.set_global_texts(stream.get_lang_text_pools()[0])
                    check(DataItem.find(1).name == "Alice" and DataItem.find(1).desc.get_text() == "你好")
                    check(DataItem.find(2) != null)
                    var package = ConfigPackage.read_file(root.path_join("pack/config.bytes"))
                    package = package.attach(ConfigPackage.read_file(root.path_join("pack/en.bytes")))
                    stream = processor.load_from_package(package, errors)
                    check(stream != null)
                    check(DataItem.find(2) == null)
                    check(DataItem.find(1).RefTarget.power == 10)
                    TextPoolManager.set_global_texts(stream.get_lang_text_pools()[1])
                    check(DataItem.find(1).desc.get_text() == "hello")
                    var previous = DataItem.find(1)
                    var incomplete = ConfigPackage.read_file(root.path_join("incomplete.bytes"))
                    check(processor.load_from_package(incomplete, errors) == null)
                    check(DataItem.find(1) == previous and DataItem.find(2) == null)
                    check(processor.load_from_package(ConfigPackage.read_file(root.path_join("bad-variant.bytes")), errors) == null)
                    var bad = FileAccess.get_file_as_bytes(root.path_join("pack/config.bytes"))
                    bad.encode_s32(0, 99)
                    check(ConfigPackage.read(bad) == null)
                    check(errors.get_error_count() == 0)
                    print("runtime-ok")
                    quit(1 if failed else 0)
                """);
        String executable = System.getProperty("cfggen.godot", "build/godot-runtime/Godot_v4.6-stable_win64_console.exe");
        Path godot = Path.of(executable).toAbsolutePath();
        assertTrue(Files.isRegularFile(godot), "Set -Dcfggen.godot to a Godot 4.6 executable");
        run(project, List.of(godot.toString(), "--headless", "--path", project.toString(), "--editor", "--import", "--quit"), false);
        run(project, List.of(godot.toString(), "--headless", "--path", project.toString(), "--script", "test.gd"), true);
    }

    private void run(Path project, List<String> command, boolean expectOutput) throws Exception {
        Path log = project.resolve("runtime.log");
        var builder = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("CFGGEN_FIXTURE", root.toString());
        var process = builder.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Godot timed out"); }
        String output = Files.readString(log);
        assertEquals(0, process.exitValue(), output);
        assertFalse(output.contains("SCRIPT ERROR"), output);
        if (expectOutput) assertTrue(output.contains("runtime-ok"), output);
    }
}

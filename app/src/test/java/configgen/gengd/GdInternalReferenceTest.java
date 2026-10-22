package configgen.gengd;

import configgen.InternalReferenceFixture;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("godot")
class GdInternalReferenceTest {
    @TempDir Path root;
    @Test
    void avoidsSchemaNamesForItsPrivateResolver() throws Exception {
        Path data = Files.createDirectories(root.resolve("data"));
        Files.writeString(data.resolve("config.cfg"), """
                struct _ObjectRelations { key:str; }
                table t[id] (json) {
                    id:int; items:list<_ObjectRelations> (index='key'); key:str ->::items;
                }
                """);
        InternalReferenceFixture.write(data, "t", "{\"id\":1,\"items\":[{\"key\":\"A\"}],\"key\":\"A\"}");
        var context = new configgen.ctx.Context(data);
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("base"))).generate(context);
        Path project = Files.createDirectories(root.resolve("project"));
        new GdCodeGenerator(new ParameterParser("gd,prefix:,dir:" + project.resolve("config"))).generate(context);
        Files.writeString(project.resolve("project.godot"), "config_version=5\n[application]\nconfig/name=\"Names\"\n");
        Files.writeString(project.resolve("test.gd"), """
                extends SceneTree
                func _initialize():
                    var errors = ConfigErrors.new()
                    var package = ConfigPackage.read_file(OS.get_environment("CFGGEN_FIXTURE").path_join("base/base.bytes"))
                    var snapshot = ConfigProcessor.new().load_from_package(package, errors)
                    if snapshot == null or errors.get_error_count() != 0:
                        quit(1)
                        return
                    if T.find(1).RefKey != T.find(1).items[0]:
                        quit(1)
                        return
                    print("runtime-ok")
                    quit()
                """);
        runGodot(project);
    }
    @Test
    void keepsTargetsAliveWithSnapshotsAndCollectsOldCycles() throws Exception {
        var context = InternalReferenceFixture.create(root.resolve("data"));
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("base"))).generate(context);
        InternalReferenceFixture.writeInvalidPackages(root.resolve("base/base.bytes"));
        Path project = Files.createDirectories(root.resolve("project"));
        new GdCodeGenerator(new ParameterParser("gd,dir:" + project.resolve("config"))).generate(context);
        Files.writeString(project.resolve("project.godot"), "config_version=5\n[application]\nconfig/name=\"Object References\"\n");
        Files.writeString(project.resolve("test.gd"), """
                extends SceneTree
                var failed: bool = false
                func _initialize():
                    call_deferred("run_test")
                func check(ok: bool):
                    if not ok:
                        failed = true
                        push_error("reference assertion")
                func capture(processor: ConfigProcessor, package: ConfigPackage) -> Array:
                    var errors = ConfigErrors.new()
                    var snapshot = processor.load_from_package(package, errors)
                    check(snapshot != null and errors.get_error_count() == 0)
                    var combat = DataCombat_Ability.find(1001)
                    var cast = DataCombat_Cast.find(1)
                    var book = DataCombat_Phasebook.find("Fire", 0, false)
                    check(combat.machines[0].RefInitial == combat.machines[0].phases[0])
                    check(combat.machines[0].RefFrame.tick == 9007199254740993)
                    check(cast.RefBook == book.entry and cast.RefBookRecord == book)
                    var outer = DataCombat_Encounter.find(1).command
                    var inner = outer.phases[1].action
                    var missing = inner.phases[0].action
                    var local = inner.phases[1].action
                    check(outer.phases[0].key == missing.key)
                    check(missing.NullableRefKey == null)
                    check(local.NullableRefKey == inner.phases[0])
                    var dispatched = DataDispatched.find(1)
                    var link = dispatched.groups[0].links[0]
                    check(link.RefIndexed == dispatched.items[0])
                    check(link.RefExternal == DataAddressed.find(1).items[0])
                    check(link.RefFixed == dispatched.anchor)
                    var mapping = DataMapping.find(1)
                    check(mapping.RefMapped == mapping.actions.values()[0])
                    var branches = DataAbility.find(1).branches
                    for branch in branches:
                        check(branch.phases[0] == branch.uses[0].RefKey)
                    check(branches[0].phases[0] == branches[0].phases[0].NullableRefNext.NullableRefNext)
                    check(branches[1].phases[0].NullableRefNext == null)
                    check(branches[0] == DataCaller.find(1).RefTarget)
                    return [snapshot, weakref(branches[0].phases[0]), weakref(DataCaller.find(1))]
                func verify_old(first: Array):
                    check(first[1].get_ref() != DataAbility.find(1).branches[0].phases[0])
                    check(first[2].get_ref().RefTarget.phases[0] == first[1].get_ref())
                func run_test():
                    var root = OS.get_environment("CFGGEN_FIXTURE")
                    var processor = ConfigProcessor.new()
                    var package = ConfigPackage.read_file(root.path_join("base/base.bytes"))
                    for filename in ["bad-map.bytes", "bad-index.bytes"]:
                        var bad = ConfigPackage.read_file(root.path_join("base/" + filename))
                        check(processor.load_from_package(bad, ConfigErrors.new()) == null)
                    var first = capture(processor, package)
                    var second = capture(processor, package)
                    verify_old(first)
                    first[0] = null
                    check(first[1].get_ref() == null)
                    check(first[2].get_ref() == null)
                    var current = DataAbility.find(1)
                    check(processor.load_from_package(ConfigPackage.read_file(root.path_join("base/bad-map.bytes")), ConfigErrors.new()) == null)
                    check(current == DataAbility.find(1))
                    second[0] = null
                    print("runtime-ok")
                    quit(1 if failed else 0)
                """);
        runGodot(project);
    }
    private void runGodot(Path project) throws Exception {
        String godot = Path.of(System.getProperty("cfggen.godot", "build/godot-runtime/Godot_v4.6-stable_win64_console.exe")).toAbsolutePath().toString();
        run(project, List.of(godot, "--headless", "--path", project.toString(), "--editor", "--import", "--quit"), false);
        run(project, List.of(godot, "--headless", "--path", project.toString(), "--script", "test.gd"), true);
    }
    private void run(Path project, List<String> command, boolean expectOutput) throws Exception {
        Path log = project.resolve("runtime.log");
        var builder = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("CFGGEN_FIXTURE", root.toString());
        var process = builder.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Godot timed out\n" + Files.readString(log));
        }
        String output = Files.readString(log);
        assertEquals(0, process.exitValue(), output);
        assertFalse(output.contains("SCRIPT ERROR"), output);
        assertFalse(output.contains("does not have any 'meta' values"), output);
        if (expectOutput) assertTrue(output.contains("runtime-ok"), output);
    }
}

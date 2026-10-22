package configgen.gencs;

import configgen.InternalReferenceFixture;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dotnet")
class CsInternalReferenceTest {
    @TempDir Path root;
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void resolvesTypedCyclesInSeparateSnapshotsAndFinalModData(boolean unity) throws Exception {
        Path data = root.resolve("data");
        InternalReferenceFixture.create(data);
        Path schemaFile = data.resolve("config.cfg");
        Files.writeString(schemaFile, Files.readString(schemaFile).replace("struct Use { key:str",
                "struct Use { _BindRefKey:int; key:str")
                + "struct RelationResolver.Phase { key:int; }\ntable named[id] (json) { id:int; point:RelationResolver.Phase; }\n");
        Path abilityFile = data.resolve("_ability/1.json");
        Files.writeString(abilityFile, Files.readString(abilityFile).replace("\"uses\":[{", "\"uses\":[{\"_BindRefKey\":7,"));
        InternalReferenceFixture.write(data, "named", "{\"id\":1,\"point\":{\"key\":9}}");
        Context context = new Context(data);
        Path project = Files.createDirectories(root.resolve("project"));
        new CsCodeGenerator(new ParameterParser("cs,dir:" + project + (unity ? ",unity" : ""))).generate(context);
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("base"))).generate(context);
        InternalReferenceFixture.writeInvalidPackages(root.resolve("base/base.bytes"));
        Path mod = Files.createDirectories(root.resolve("mod"));
        InternalReferenceFixture.write(mod, "ability", "{\"id\":1,\"branches\":[{\"code\":\"left\",\"phases\":[{\"key\":\"A\",\"next\":\"A\"}],\"uses\":[{\"key\":\"A\"}]}]}");
        new BytesGenerator(new ParameterParser("bytes,pack:mod,requires:base,deps:" + root.resolve("base/base.bytes") + ",dir:" + root.resolve("modpack")))
                .generate(new Context(Context.ContextCfg.of(mod), context.cfgSchema()));
        writeProject(project, unity);
        Files.writeString(project.resolve("Program.cs"), """
                using System;
                using Config;
                using Config.Combat.Command;
                class Program {
                    static void Check(bool ok) { if (!ok) throw new Exception("reference assertion"); }
                    static DBranch CheckConfig(ConfigMgr config) {
                        var combat = Config.Combat.DAbility.Get(config, 1001)!;
                        Check(ReferenceEquals(combat.Machines[0].Phases[0], combat.Machines[0].RefInitial));
                        var cast = Config.Combat.DCast.Get(config, 1)!;
                        var book = Config.Combat.DPhasebook.Get(config, "Fire", 0, false)!;
                        Check(ReferenceEquals(book.Entry, cast.RefBook));
                        Check(ReferenceEquals(book, cast.RefBookRecord));
                        var outer = (DStateMachine)Config.Combat.DEncounter.Get(config, 1)!.Command;
                        var inner = (DStateMachine)outer.Phases[1].Action;
                        var missing = (DJump)inner.Phases[0].Action;
                        var local = (DJump)inner.Phases[1].Action;
                        Check(outer.Phases[0].Key == missing.Key);
                        Check(missing.NullableRefKey == null);
                        Check(ReferenceEquals(local.NullableRefKey, inner.Phases[0]));
                        Check(DNamed.Get(config, 1)!.Point.Key == 9);
                        var dispatched = DDispatched.Get(config, 1)!;
                        var link = dispatched.Groups[0].Links[0];
                        Check(ReferenceEquals(dispatched.Items[0], link.RefIndexed));
                        Check(ReferenceEquals(DAddressed.Get(config, 1)!.Items[0], link.RefExternal));
                        Check(ReferenceEquals(dispatched.Anchor, link.RefFixed));
                        var mapping = DMapping.Get(config, 1)!;
                        foreach (var action in mapping.Actions.Values) Check(ReferenceEquals(action, mapping.RefMapped));
                        var row = DAbility.Get(config, 1)!;
                        Check(row.Branches[0].Uses[0]._BindRefKey == 7);
                        foreach (var branch in row.Branches) Check(ReferenceEquals(branch.Phases[0], branch.Uses[0].RefKey));
                        Check(ReferenceEquals(row.Branches[0].Phases[0], row.Branches[0].Phases[0].NullableRefNext!.NullableRefNext));
                        Check(ReferenceEquals(row.Branches[0], DCaller.Get(config, 1)!.RefTarget));
                        Check(row.Branches[1].Phases[0].NullableRefNext == null);
                        return row.Branches[0];
                    }
                    static void Main(string[] args) {
                        var pack = ConfigPackage.ReadFile(args[0]);
                        foreach (var name in new[] { "bad-map.bytes", "bad-index.bytes" }) {
                            bool rejected = false;
                            try { Processor.LoadPackages(ConfigPackage.ReadFile(System.IO.Path.Combine(System.IO.Path.GetDirectoryName(args[0])!, name))); }
                            catch (ConfigException error) { rejected = error.Message.ToLowerInvariant().Contains("duplicate"); }
                            Check(rejected);
                        }
                        var a = Processor.LoadPackages(pack); var b = Processor.LoadPackages(pack);
                        Check(!ReferenceEquals(CheckConfig(a), CheckConfig(b)));
                        var merged = Processor.LoadPackages(pack, ConfigPackage.ReadFile(args[1]));
                        var target = DCaller.Get(merged, 1)!.RefTarget;
                        Check(ReferenceEquals(target, DAbility.Get(merged, 1)!.Branches[0]));
                        Check(ReferenceEquals(target.Phases[0], target.Phases[0].NullableRefNext));
                        CheckConfig(a);
                        Console.WriteLine("runtime-ok");
                    }
                }
                """);
        run(project, List.of(root.resolve("base/base.bytes"), root.resolve("modpack/mod.bytes")));
    }

    @Test
    void supportsADataClassNamedLikeTheResolverWithAnEmptyPrefix() throws Exception {
        var context = configgen.TestCtx.newContext(Files.createDirectories(root.resolve("data")), """
                struct RelationResolver { key:int; }
                table t[id] { id:int; items:list<RelationResolver> (pack,index='key'); selected:int ->::items; }
                """, Map.of("t", "id,items,selected\nid,items,selected\n1,(2),2\n"));
        Path project = Files.createDirectories(root.resolve("project"));
        new CsCodeGenerator(new ParameterParser("cs,prefix:,dir:" + project)).generate(context);
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("base"))).generate(context);
        writeProject(project, false);
        Files.writeString(project.resolve("Program.cs"), """
                class Program {
                    static void Main(string[] args) {
                        var config = Config.Processor.LoadPackages(Config.ConfigPackage.ReadFile(args[0]));
                        var row = Config.T.Get(config, 1)!;
                        if (!object.ReferenceEquals(row.Items[0], row.RefSelected)) throw new System.Exception("reference assertion");
                        System.Console.WriteLine("runtime-ok");
                    }
                }
                """);
        run(project, List.of(root.resolve("base/base.bytes")));
    }

    private void writeProject(Path project, boolean unity) throws Exception {
        Files.writeString(project.resolve("runtime.csproj"), """
                <Project Sdk="Microsoft.NET.Sdk"><PropertyGroup>
                <OutputType>Exe</OutputType><TargetFramework>net10.0</TargetFramework>
                <Nullable>enable</Nullable><ImplicitUsings>disable</ImplicitUsings><LangVersion>%s</LangVersion>
                <TreatWarningsAsErrors>true</TreatWarningsAsErrors>
                </PropertyGroup></Project>
                """.formatted(unity ? "9.0" : "latest"));
    }

    private void run(Path project, List<Path> packages) throws Exception {
        Path log = project.resolve("runtime.log");
        List<String> command = new ArrayList<>(List.of("dotnet", "run", "--project", project.resolve("runtime.csproj").toString(), "--configuration", "Release", "--verbosity", "quiet", "--"));
        packages.forEach(path -> command.add(path.toString()));
        var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("dotnet timed out"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
        assertTrue(Files.readString(log).contains("runtime-ok"));
    }
}

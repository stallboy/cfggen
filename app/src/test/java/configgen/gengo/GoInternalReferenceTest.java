package configgen.gengo;

import configgen.InternalReferenceFixture;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("otherRuntimes")
class GoInternalReferenceTest {
    @TempDir Path root;
    @Test
    void bindsContainmentAndKeepsOldObjectsStableAfterReload() throws Exception {
        InternalReferenceFixture.create(root.resolve("data"));
        Files.writeString(root.resolve("data/config.cfg"), """
                struct Point { x:int; }
                table symbolic[id] (json,entry='name') { id:int; name:str; fixed:Point; ->point:[] ->::fixed; }
                table unusedmap[id] { id:int; values:map<AddressKey,int> (pack); }
                """, StandardOpenOption.APPEND);
        InternalReferenceFixture.write(root.resolve("data"), "symbolic", "{\"id\":1,\"name\":\"Start\",\"fixed\":{\"x\":1}}");
        Files.writeString(root.resolve("data/unusedmap.csv"), "id,values\nid,values\n1,\"(fire,2),10\"\n");
        var context = new configgen.ctx.Context(root.resolve("data"));
        Path project = Files.createDirectories(root.resolve("project"));
        new GoCodeGenerator(new ParameterParser("go,dir:" + project)).generate(context);
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("base"))).generate(context);
        InternalReferenceFixture.writeInvalidPackages(root.resolve("base/base.bytes"));
        Files.writeString(project.resolve("go.mod"), "module fixture\n\ngo 1.22\n");
        Files.writeString(project.resolve("config/internal_reference_test.go"), """
                package config
                import ("testing"; "os"; "path/filepath"; "fmt"; "strings")
                func TestObjectReferences(t *testing.T) {
                    data, err := os.ReadFile(os.Getenv("CFGGEN_FIXTURE")); if err != nil { t.Fatal(err) }
                    check := func(ok bool) { if !ok { t.Fatal("reference assertion") } }
                    for _, name := range []string{"bad-map.bytes", "bad-index.bytes"} {
                        invalid, err := os.ReadFile(filepath.Join(filepath.Dir(os.Getenv("CFGGEN_FIXTURE")), name)); if err != nil { t.Fatal(err) }
                        func() { defer func() { check(strings.Contains(strings.ToLower(fmt.Sprint(recover())), "duplicate")) }(); InitPackage(ReadPackage(invalid)) }()
                    }
                    load := func() *Branch {
                        InitPackage(ReadPackage(data))
                        combat := GetCombatAbilityMgr().Get(1001); cast := GetCombatCastMgr().Get(1)
                        check(combat.Machines()[0].RefInitial() == combat.Machines()[0].Phases()[0])
                        check(combat.Machines()[0].RefFrame().Tick() == 9007199254740993)
                        book := GetCombatPhasebookMgr().Get("Fire", 0, false)
                        check(cast.RefBook() == book.Entry() && cast.RefBookRecord() == book)
                        outer := GetCombatEncounterMgr().Get(1).Command().(*CombatCommandStateMachine)
                        inner := outer.Phases()[1].Action().(*CombatCommandStateMachine)
                        missing := inner.Phases()[0].Action().(*CombatCommandJump)
                        check(outer.Phases()[0].Key() == missing.Key())
                        check(missing.NullableRefKey() == nil)
                        local := inner.Phases()[1].Action().(*CombatCommandJump)
                        check(local.NullableRefKey() == inner.Phases()[0])
                        dispatched := GetDispatchedMgr().Get(1); link := dispatched.Groups()[0].Links()[0]
                        check(link.RefIndexed() == dispatched.Items()[0])
                        check(link.RefExternal() == GetAddressedMgr().Get(1).Items()[0])
                        check(link.RefFixed() == dispatched.Anchor())
                        mapping := GetMappingMgr().Get(1)
                        for _, action := range mapping.Actions() { check(mapping.RefMapped() == action) }
                        branches := GetAbilityMgr().Get(1).Branches()
                        for _, branch := range branches { check(branch.Phases()[0] == branch.Uses()[0].RefKey()) }
                        check(branches[0].Phases()[0].NullableRefNext().NullableRefNext() == branches[0].Phases()[0])
                        check(branches[1].Phases()[0].NullableRefNext() == nil)
                        check(GetCallerMgr().Get(1).RefTarget() == branches[0])
                        return branches[0]
                    }
                    old := load(); oldSymbol := GetSymbolicMgr().GetStart()
                    check(oldSymbol == GetSymbolicMgr().Get(1)); check(oldSymbol.RefPoint() == oldSymbol.Fixed())
                    check(old != load()); check(oldSymbol != GetSymbolicMgr().GetStart())
                    check(oldSymbol.RefPoint() == oldSymbol.Fixed())
                    check(old.Phases()[0].NullableRefNext().NullableRefNext() == old.Phases()[0])
                    values := GetUnusedmapMgr().Get(1).values
                    check(len(values) == 1)
                    extra := &AddressKey{label: "fire", variant: 3}
                    values[extra] = 20
                    (&Stream{}).bindObjectReferences()
                    check(len(values) == 2)
                    values[&AddressKey{label: extra.label, variant: extra.variant}] = 30
                    check(len(values) == 3)
                    func() {
                        defer func() { check(strings.Contains(fmt.Sprint(recover()), "duplicate map key:")) }()
                        (&Stream{}).bindObjectReferences()
                    }()
                }
                """);
        Path log = project.resolve("runtime.log");
        var builder = new ProcessBuilder(List.of("go", "test", "./...")).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("CFGGEN_FIXTURE", root.resolve("base/base.bytes").toString());
        var process = builder.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Go timed out"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }
}

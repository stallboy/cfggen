package configgen.gengo;

import configgen.InternalReferenceFixture;
import configgen.TestCtx;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("otherRuntimes")
class GoReverseReferenceTest {
    @TempDir Path root;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void indexesReferencedNonPrimaryFieldsAndComparesStructFields(boolean relations) throws Exception {
        Path data = root.resolve("data");
        String fields = "\"key\":{\"code\":\"Hit\",\"variant\":0,\"rank\":1},"
                + "\"category\":\"Fire\",\"ordinal\":0,\"airborne\":false,\"tick\":9007199254740993";
        InternalReferenceFixture.write(data, "rank", "{\"id\":1}");
        InternalReferenceFixture.write(data, "row", "{\"id\":1," + fields + "}");
        Files.writeString(data.resolve("_row/2.json"), "{\"id\":2," + fields + "}");
        InternalReferenceFixture.write(data, "source", "{\"id\":1," + fields + "}");
        var context = TestCtx.newContext(data, """
                struct Key { code:str; variant:int; rank:int ->rank; }
                table rank[id] (json) { id:int; }
                table row[id] (json) { id:int; key:Key; category:str; ordinal:int; airborne:bool; tick:long; }
                table source[id] (json) {
                    id:int;
                    key:Key =>row[key];
                    category:str =>row[category];
                    ordinal:int =>row[ordinal];
                    airborne:bool =>row[airborne];
                    tick:long =>row[tick];
                    %s
                }
                """.formatted(relations ? "->path:[::id] ->row;" : ""), Map.of());
        Path project = Files.createDirectories(root.resolve("project"));
        new GoCodeGenerator(new ParameterParser("go,dir:" + project)).generate(context);
        new BytesGenerator(new ParameterParser("bytes,dir:" + project)).generate(context);
        Files.writeString(project.resolve("go.mod"), "module fixture\n\ngo 1.23\n");
        Files.writeString(project.resolve("config/reverse_test.go"), """
                package config
                import ("os"; "testing")
                func TestReverseReferences(t *testing.T) {
                    file, err := os.Open("../base.bytes"); if err != nil { t.Fatal(err) }; defer file.Close()
                    Init(file)
                    check := func(name string, rows []*Row) {
                        if len(rows) != 2 || rows[0] != GetRowMgr().Get(1) || rows[1] != GetRowMgr().Get(2) {
                            t.Fatalf("%s reverse reference must keep both matching rows", name)
                        }
                    }
                    source := GetSourceMgr().Get(1)
                    key := *source.Key()
                    key.RefRank() // A derived cache cannot change grouping by the persisted key fields.
                    check("copied struct", GetRowMgr().GetAllByKey(&key))
                    check("struct", source.ListRefKey())
                    check("string", source.ListRefCategory())
                    check("integer", source.ListRefOrdinal())
                    check("boolean", source.ListRefAirborne())
                    check("64-bit", source.ListRefTick())
                    key.code = "Missing"
                    if len(GetRowMgr().GetAllByKey(&key)) != 0 || len(GetRowMgr().GetAllByKey(nil)) != 0 {
                        t.Fatal("missing and nil keys must not match")
                    }
                }
                """);
        Path log = project.resolve("runtime.log");
        var process = new ProcessBuilder("go", "test", "./...").directory(project.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Go reverse-reference test timed out");
        }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }
}

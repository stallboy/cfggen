package configgen.gengo;

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
class GoStructKeyTest {
    @TempDir Path root;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void comparesPrimaryAndUniqueStructKeysByPersistedFields(boolean relations) throws Exception {
        String key = "{\"name\":\"\",\"count\":0,\"flag\":false,\"tick\":9007199254740993,\"rank\":1}";
        String schema = """
                struct Key { name:str; count:int; flag:bool; tick:long; rank:int ->rank; }
                table rank[id] (json) { id:int; }
                table keyed[key] (json) { key:Key; }
                table aliases[id] (json) { [key]; id:int; key:Key; }
                table uses[id] (json) { id:int; key:Key ->keyed; ->unique:[key] ->aliases[key]; %s }
                """.formatted(relations ? "->self:[] ->::key;" : "");
        configgen.InternalReferenceFixture.write(root.resolve("data"), "rank", "{\"id\":1}");
        configgen.InternalReferenceFixture.write(root.resolve("data"), "keyed", "{\"key\":" + key + "}");
        configgen.InternalReferenceFixture.write(root.resolve("data"), "aliases", "{\"id\":1,\"key\":" + key + "}");
        configgen.InternalReferenceFixture.write(root.resolve("data"), "uses", "{\"id\":1,\"key\":" + key + "}");
        var context = TestCtx.newContext(root.resolve("data"), schema, Map.of());
        Path project = Files.createDirectories(root.resolve("project"));
        new GoCodeGenerator(new ParameterParser("go,dir:" + project)).generate(context);
        new BytesGenerator(new ParameterParser("bytes,dir:" + project)).generate(context);
        Files.writeString(project.resolve("go.mod"), "module fixture\n\ngo 1.23\n");
        Files.writeString(project.resolve("config/keys_test.go"), """
                package config
                import ("os"; "testing")
                func TestStructKeys(t *testing.T) {
                    file, err := os.Open("../base.bytes"); if err != nil { t.Fatal(err) }; defer file.Close()
                    Init(file)
                    source := GetUsesMgr().Get(1)
                    if source.String() != "Uses{id=1, key=Key{name=, count=0, flag=false, tick=9007199254740993, rank=1}}" {
                        t.Fatal("String must preserve nested struct formatting")
                    }
                    key := *source.Key()
                    key.RefRank() // Derived reference caches must not participate in key equality.
                    primary := GetKeyedMgr().Get(&key)
                    unique := GetAliasesMgr().GetBykey(&key)
                    if primary == nil || primary != source.RefKey() || unique == nil || unique != source.RefUnique() {
                        t.Fatal("equal struct fields must find the same records")
                    }
                    if primary.Key() == source.Key() { t.Fatal("fixture must use distinct key objects") }
                    key.name = "missing"
                    if GetKeyedMgr().Get(&key) != nil || GetAliasesMgr().GetBykey(&key) != nil { t.Fatal("missing key matched") }
                    if GetKeyedMgr().Get(nil) != nil || GetAliasesMgr().GetBykey(nil) != nil { t.Fatal("nil key matched") }
                }
                """);
        Path log = project.resolve("runtime.log");
        var process = new ProcessBuilder("go", "test", "./...").directory(project.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Go struct-key test timed out");
        }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }
}

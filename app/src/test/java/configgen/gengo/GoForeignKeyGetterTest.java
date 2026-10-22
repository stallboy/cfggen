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
class GoForeignKeyGetterTest {
    @TempDir Path root;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void generatesScalarAndContainerGettersForPrimaryAndUniqueKeys(boolean relations) throws Exception {
        Path data = root.resolve("data");
        InternalReferenceFixture.write(data, "row", "{\"id\":1,\"code\":\"Hit\",\"kind\":\"Fire\",\"tier\":0}");
        String schema = """
                table row[id] (json) { [code]; [kind,tier]; id:int; code:str; kind:str; tier:int; }
                table child[parent,sequence] { parent:int; sequence:int; }
                table refs[id] {
                    id:int;
                    selected:str ->row[code];
                    next:str ->row[code] (nullable);
                    codes:list<str> ->row[code] (pack);
                    rows:map<str,str> ->row[code] (pack);
                    booleanRows:map<bool,str> ->row[code] (pack);
                    primaryRows:map<long,int> ->row (pack);
                    ->aliasedRows:[rows] ->row[code];
                    kind:str; tier:int;
                    ->combo:[kind,tier] ->row[kind,tier];
                    parentId:int =>child[parent];
                    %s
                }
                """.formatted(relations ? "->path:[::selected] ->row[code];" : "");
        var context = TestCtx.newContext(data, schema, Map.of(
                "child", "parent,sequence\nparent,sequence\n1,1\n1,2\n",
                "refs", """
                        id,selected,next,codes,rows,booleanRows,primaryRows,kind,tier,parentId
                        id,selected,next,codes,rows,booleanRows,primaryRows,kind,tier,parentId
                        1,Hit,,"Hit,Hit","left,Hit","false,Hit,true,Hit","9007199254740993,1",Fire,0,1
                        """));
        Path project = Files.createDirectories(root.resolve("project"));
        new GoCodeGenerator(new ParameterParser("go,dir:" + project)).generate(context);
        new BytesGenerator(new ParameterParser("bytes,dir:" + project)).generate(context);
        Files.writeString(project.resolve("go.mod"), "module fixture\n\ngo 1.23\n");
        Files.writeString(project.resolve("config/references_test.go"), """
                package config
                import ("os"; "testing")
                func TestForeignKeyGetters(t *testing.T) {
                    file, err := os.Open("../base.bytes"); if err != nil { t.Fatal(err) }; defer file.Close()
                    Init(file)
                    source := GetRefsMgr().Get(1); target := GetRowMgr().Get(1)
                    if source.RefSelected() != target || source.NullableRefNext() != nil || source.RefCombo() != target {
                        t.Fatal("scalar unique-key getters")
                    }
                    if len(source.RefCodes()) != 2 || source.RefCodes()[0] != target || source.RefCodes()[1] != target {
                        t.Fatal("list unique-key getter")
                    }
                    if source.RefRows()["left"] != target { t.Fatal("string-keyed map unique-key getter") }
                    if source.RefBooleanRows()[false] != target || source.RefBooleanRows()[true] != target {
                        t.Fatal("boolean-keyed map getter")
                    }
                    if source.RefAliasedRows()["left"] != target { t.Fatal("named map getter") }
                    if source.RefPrimaryRows()[9007199254740993] != target { t.Fatal("64-bit map primary-key getter") }
                    children := source.ListRefParentId()
                    if len(children) != 2 || children[0] != GetChildMgr().Get(1, 1) || children[1] != GetChildMgr().Get(1, 2) {
                        t.Fatal("reverse reference uses the remote field name")
                    }
                    source.codes = nil; source.rows = nil; source.booleanRows = nil; source.primaryRows = nil
                    if source.String() != "Refs{id=1, selected=Hit, next=, codes=[], rows=map[], booleanRows=map[], primaryRows=map[], kind=Fire, tier=0, parentId=1}" {
                        t.Fatal("String must preserve empty list and map formatting")
                    }
                }
                """);
        Path log = project.resolve("runtime.log");
        var process = new ProcessBuilder("go", "test", "./...").directory(project.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Go foreign-key getter test timed out");
        }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }
}

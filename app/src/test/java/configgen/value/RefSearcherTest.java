package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RefSearcherTest {

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void searchRef() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    asset:str ->assets(lowercase);
                }
                table assets[path] {
                    path:str;
                    type:str;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,
                id,asset
                1,npc/A.prefab
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);
        String assetStr = """
                ,
                path,type
                pic.png,all
                npc/a.prefab,npc
                """;
        Resources.addTempFileFromText("assets.csv", tempDir, assetStr);
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        RefSearcher.RefSearchResult res = RefSearcher.search(cfgValue, "assets", null, Set.of());
        assertEquals(RefSearcher.RefSearchErr.Ok, res.err());
        assertEquals(1, res.value2tables().size());
        CfgValue.Value v = res.value2tables().keySet().iterator().next();
        assertEquals("npc/a.prefab", ((CfgValue.VString)v).value());
        Set<String> tables = res.value2tables().values().iterator().next();
        assertEquals(Set.of("t"), tables);
    }

}

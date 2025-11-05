package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.schema.TableSchemaRefGraph;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ValueRefInCollectorTest {

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void collectRefIns() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    asset:str ->assets;
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
                1,npc/a.prefab
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
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable assets = cfgValue.getTable("assets");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);
        var refIns = refInCollector.collect(assets, Values.ofStr("npc/a.prefab"));

        assertEquals(1, refIns.size());
        ValueRefCollector.RefId refId = refIns.keySet().iterator().next();
        assertEquals("t", refId.table());
        assertEquals("1", refId.id());
        CfgValue.VStruct value = refIns.values().iterator().next().recordValue();
        assertEquals(Values.ofInt(1), value.values().get(0));
        assertEquals(Values.ofStr("npc/a.prefab"), value.values().get(1));

    }
}

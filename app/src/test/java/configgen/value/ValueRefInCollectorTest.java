package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.data.CfgDataReader;
import configgen.data.ReadByFastExcel;
import configgen.data.ReadCsv;
import configgen.schema.TableSchemaRefGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValueRefInCollectorTest {

    private @TempDir Path tempDir;

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
        CfgDataReader dataReader = new CfgDataReader(2, new ReadCsv("GBK"), ReadByFastExcel.INSTANCE);
        Context ctx = new Context(tempDir, dataReader, null, null);
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        CfgValue.VTable assets = cfgValue.getTable("assets");

        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);
        Map<ValueRefCollector.RefId, CfgValue.VStruct> refIns = refInCollector.collect(assets, Values.ofStr("npc/a.prefab"));

        assertEquals(1, refIns.size());
        ValueRefCollector.RefId refId = refIns.keySet().iterator().next();
        assertEquals("t", refId.table());
        assertEquals("1", refId.id());
        CfgValue.VStruct value = refIns.values().iterator().next();
        assertEquals(Values.ofInt(1), value.values().get(0));
        assertEquals(Values.ofStr("npc/a.prefab"), value.values().get(1));

    }
}
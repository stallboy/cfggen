package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.data.CfgDataReader;
import configgen.data.ReadByFastExcel;
import configgen.data.ReadCsv;
import configgen.schema.TableSchema;
import configgen.value.CfgValue.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CfgValueParserTest {

    private @TempDir Path tempDir;

    @Test
    void parseCfgValue() {
        String cfgStr = """
                table rank[RankID] (enum='RankName'){
                    [RankName];
                    RankID:int; // 稀有度
                    RankName:str; // 程序用名字
                    RankShowName:text; // 显示名称
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromResourceFile("rank.csv", tempDir);

        CfgDataReader dataReader = new CfgDataReader(2, new ReadCsv("GBK"), ReadByFastExcel.INSTANCE);
        Context ctx = new Context(tempDir, dataReader, null, null);
        CfgValue cfgValue = ctx.makeValue();
        VTable rank = cfgValue.getTable("rank");
        TableSchema rankSchema = ctx.cfgSchema().findTable("rank");

        assertEquals(rankSchema, rank.schema());
        assertEquals(5, rank.valueList().size());
        {
            VStruct v = rank.valueList().get(0);
            assertEquals(rankSchema, v.schema());
            assertEquals(1, ((VInt) v.values().get(0)).value());
            assertEquals("white", ((VString) v.values().get(1)).value());
            assertEquals("下品", ((VText) v.values().get(2)).value());
        }
        {
            VStruct v = rank.valueList().get(4);
            assertEquals(rankSchema, v.schema());
            assertEquals(5, ((VInt) v.values().get(0)).value());
            assertEquals("yellow", ((VString) v.values().get(1)).value());
            assertEquals("准神", ((VText) v.values().get(2)).value());
        }
    }


}
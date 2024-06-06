package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.data.CfgDataReader;
import configgen.data.ReadByFastExcel;
import configgen.data.ReadCsv;
import configgen.schema.CfgSchema;
import configgen.schema.ForeignKeySchema;
import configgen.schema.Structural;
import configgen.schema.TableSchema;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;
import static configgen.value.Values.*;
import static org.junit.jupiter.api.Assertions.*;

class ValueUtilTest {

    @Test
    void extractKeyValue() {
        String str = """
                struct st{
                    i:int;
                    s:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve();
        VStruct vStruct = VStruct.of((Structural) cfg.findItem("st"), List.of(ofInt(1), ofStr("abc")));
        {
            Value keyValue = ValueUtil.extractKeyValue(vStruct, new int[]{0});
            assertTrue(keyValue instanceof VInt vInt && vInt.value() == 1);
        }
        {
            Value keyValue = ValueUtil.extractKeyValue(vStruct, new int[]{0, 1});
            assertTrue(keyValue instanceof VList vList && vList.valueList().size() == 2 &&
                       ((VString) vList.valueList().get(1)).value().equals("abc"));
        }
        {
            assertThrows(Exception.class, () -> ValueUtil.extractKeyValue(vStruct, new int[]{2}));
        }
    }


    @Test
    void extractPrimaryKeyValue() {
        String str = """
                table t[id]{
                    id:int;
                    s:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve();
        TableSchema t = cfg.findTable("t");
        VStruct vStruct = VStruct.of(t, List.of(ofInt(1), ofStr("abc")));

        Value keyValue = ValueUtil.extractPrimaryKeyValue(vStruct, t);
        assertTrue(keyValue instanceof VInt vInt && vInt.value() == 1);
    }

    @Test
    void extractFieldValue() {
        String str = """
                table t[id]{
                    id:int;
                    s:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        TableSchema t = cfg.findTable("t");
        VStruct vStruct = VStruct.of(t, List.of(ofInt(1), ofStr("abc")));


        {
            Value idValue = ValueUtil.extractFieldValue(vStruct, "id");
            assertTrue(idValue instanceof VInt vInt && vInt.value() == 1);
        }

        {
            Value sValue = ValueUtil.extractFieldValue(vStruct, "s");
            assertTrue(sValue instanceof VString vString && vString.value().equals("abc"));
        }

        {
            Value v = ValueUtil.extractFieldValue(vStruct, "notExist");
            assertNull(v);
        }
    }

    @Test
    void getForeignKeyValueMap(@TempDir Path tempDir) {
        String cfgStr = """
                table rank[RankID] (enum='RankName'){
                    [RankName];
                    RankID:int; // 稀有度
                    RankName:str; // 程序用名字
                    RankShowName:text; // 显示名称
                }
                table test[id] {
                    id:int;
                    rank:int ->rank;
                    rankName:str ->rank[RankName];
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromResourceFile("rank.csv", tempDir);

        String testStr = """
                ,,
                id,rank,rankName""";
        Resources.addTempFileFromText("test.csv", tempDir, testStr);

        CfgDataReader dataReader = new CfgDataReader(2, new ReadCsv("GBK"), ReadByFastExcel.INSTANCE);
        Context ctx = new Context(tempDir, dataReader, null, null);
        List<ForeignKeySchema> fks = ctx.cfgSchema().findTable("test").foreignKeys();
        CfgValue cfgValue = ctx.makeValue();
        VTable rank = cfgValue.getTable("rank");

        {
            Map<Value, VStruct> foreignKeyValueMap = ValueUtil.getForeignKeyValueMap(cfgValue, fks.get(0));
            assertEquals(rank.primaryKeyMap(), foreignKeyValueMap);
        }
        {
            Map<Value, VStruct> foreignKeyValueMap = ValueUtil.getForeignKeyValueMap(cfgValue, fks.get(1));
            assertEquals(rank.uniqueKeyMaps().values().iterator().next(), foreignKeyValueMap);
        }
    }
}
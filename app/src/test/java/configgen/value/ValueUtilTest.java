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
        VStruct vStruct = ofStruct((Structural) cfg.findItem("st"), List.of(ofInt(1), ofStr("abc")));
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
        VStruct vStruct = ofStruct(t, List.of(ofInt(1), ofStr("abc")));

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
        VStruct vStruct = ofStruct(t, List.of(ofInt(1), ofStr("abc")));


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

        Context ctx = new Context(tempDir);
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

    @Test
    void should_extractComplexKeyValue_when_multipleFieldIndexesProvided() {
        String str = """
                struct st{
                    i:int;
                    s:str;
                    b:bool;
                    f:float;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve();
        VStruct vStruct = ofStruct((Structural) cfg.findItem("st"),
            List.of(ofInt(1), ofStr("abc"), ofBool(true), ofFloat(3.14f)));

        {
            Value keyValue = ValueUtil.extractKeyValue(vStruct, new int[]{0, 1});
            assertTrue(keyValue instanceof VList);
            VList keyList = (VList) keyValue;
            assertEquals(2, keyList.valueList().size());
            assertEquals(1, ((VInt) keyList.valueList().get(0)).value());
            assertEquals("abc", ((VString) keyList.valueList().get(1)).value());
        }

        {
            Value keyValue = ValueUtil.extractKeyValue(vStruct, new int[]{1, 2, 3});
            assertTrue(keyValue instanceof VList);
            VList keyList = (VList) keyValue;
            assertEquals(3, keyList.valueList().size());
            assertEquals("abc", ((VString) keyList.valueList().get(0)).value());
            assertTrue(((VBool) keyList.valueList().get(1)).value());
            assertEquals(3.14f, ((VFloat) keyList.valueList().get(2)).value(), 0.001);
        }
    }

    @Test
    void should_extractFieldValueFromNestedStructure_when_complexPathProvided() {
        String str = """
                struct Inner {
                    value:int;
                    text:str;
                }
                struct Outer {
                    id:int;
                    inner:Inner;
                    flag:bool;
                }
                table t[id] {
                    id:int;
                    outer:Outer;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();

        VStruct inner = ofStruct((Structural) cfg.findItem("Inner"),
            List.of(ofInt(100), ofStr("inner text")));
        VStruct outer = ofStruct((Structural) cfg.findItem("Outer"),
            List.of(ofInt(1), inner, ofBool(true)));
        VStruct vStruct = ofStruct(cfg.findTable("t"),
            List.of(ofInt(999), outer));

        {
            Value idValue = ValueUtil.extractFieldValue(vStruct, "id");
            assertEquals(999, ((VInt) idValue).value());
        }

        {
            Value outerValue = ValueUtil.extractFieldValue(vStruct, "outer");
            assertTrue(outerValue instanceof VStruct);
            assertEquals(1, ((VInt) ((VStruct) outerValue).values().getFirst()).value());

            Value innerValue = ValueUtil.extractFieldValue((VStruct) outerValue, "inner");
            assertTrue(innerValue instanceof VStruct);
            assertEquals(100, ((VInt) ((VStruct) innerValue).values().getFirst()).value());

            Value innerTextValue = ValueUtil.extractFieldValue((VStruct) innerValue, "text");
            assertEquals("inner text", ((VString) innerTextValue).value());
        }
    }

    @Test
    void should_handleEdgeCases_when_extractingFieldValues() {
        String str = """
                table t[id] {
                    id:int;
                    emptyStr:str;
                    nullStr:str (nullable);
                    zeroInt:int;
                    falseBool:bool;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        TableSchema t = cfg.findTable("t");

        VStruct vStruct = ofStruct(t, List.of(
            ofInt(1),
            ofStr(""),  // 空字符串
            ofStr(""),  // 可为空的空字符串
            ofInt(0),   // 零值
            ofBool(false) // false布尔值
        ));

        {
            Value emptyStrValue = ValueUtil.extractFieldValue(vStruct, "emptyStr");
            assertEquals("", ((VString) emptyStrValue).value());
        }

        {
            Value nullStrValue = ValueUtil.extractFieldValue(vStruct, "nullStr");
            assertEquals("", ((VString) nullStrValue).value());
        }

        {
            Value zeroIntValue = ValueUtil.extractFieldValue(vStruct, "zeroInt");
            assertEquals(0, ((VInt) zeroIntValue).value());
        }

        {
            Value falseBoolValue = ValueUtil.extractFieldValue(vStruct, "falseBool");
            assertFalse(((VBool) falseBoolValue).value());
        }
    }

}

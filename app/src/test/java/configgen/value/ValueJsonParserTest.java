package configgen.value;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;
import static configgen.value.Values.*;
import static org.junit.jupiter.api.Assertions.*;

class ValueJsonParserTest {

    private CfgSchema cfg;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @BeforeEach
    void beforeAll() {
        String str = """
                interface condition {
                    struct checkItem {
                        id:int;
                    }
                    struct and {
                        c1:condition;
                        c2:condition;
                    }
                }
                table cond[id] {
                    id:int;
                    c:map<int,condition> (pack);
                }
                struct attr {
                	Attr:int; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                table test[id] (json) {
                    id:int;
                    bool1:bool;
                	long1:long;
                	float1:float;
                	str1:str;
                }
                table ts[id] (json) {
                    id:int;
                	attr:attr;
                }
                table tl[id] (json) {
                    id:int;
                    listInt1:list<int>;
                }
                table tm[id] {
                    id:int;
                    mapIntStr:map<int,str> (pack);
                }
                table tls[id] (json) {
                    id:int;
                    listAttr:list<attr>;
                }
                """;
        cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
    }

    @Test
    void fromJson_PrimitiveValue() {
        TableSchema test = cfg.findTable("test");
        VStruct vStruct = ofStruct(test, List.of(ofInt(123), ofBool(true), ofLong(1234567890L), ofFloat(3.14f), ofStr("abc")));

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr = """
                {"$type":"test","id":123,"bool1":true,"long1":1234567890,"float1":3.14,"str1":"abc"}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(test, jsonStr);

        assertEquals(vStruct, vStruct2);
    }

    private static VStruct fromJson(TableSchema tableSchema,
                                    String jsonStr) {
        CfgValueErrs errs = CfgValueErrs.of();
        VStruct vStruct = new ValueJsonParser(tableSchema, errs).fromJson(jsonStr);
        assertEquals(0, errs.errs().size());
        assertEquals(0, errs.warns().size());
        return vStruct;
    }

    private static VStruct fromJson(TableSchema tableSchema,
                                    CfgValueErrs errs,
                                    String jsonStr) {
        return new ValueJsonParser(tableSchema, errs).fromJson(jsonStr);
    }


    @Test
    void fromJson_Primitive_DefaultValue() {
        TableSchema test = cfg.findTable("test");
        VStruct vStruct = fromJson(test, """
                {"$type":"test"}""");
        JSONObject json = new ValueToJson().toJson(vStruct);

        String jsonStr = """
                {"$type":"test","id":0,"bool1":false,"long1":0,"float1":0.0,"str1":""}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(test, "{}");
        JSONObject json2 = new ValueToJson().toJson(vStruct2);
        assertEquals(jsonStr, json2.toString());
    }

    @Test
    void fromJson_PrimitiveValue_TypeNotMatch() {
        TableSchema test = cfg.findTable("test");
        {
            String jsonStr = """
                    {"$type":"test","id":123,"bool1":"should be bool,but str","long1":1234567890,"float1":3.14,"str1":"abc"}""";
            CfgValueErrs errs = CfgValueErrs.of();
            VStruct vStruct2 = fromJson(test, errs, jsonStr);
            assertEquals(1, errs.errs().size());
            errs.checkErrors("", true);

        }

        {
            String jsonStr = """
                    {"$type":"test","id":123,"bool1":true,"long1":1234567890,"float1":3.14,"str1":123}""";
            CfgValueErrs errs = CfgValueErrs.of();
            VStruct vStruct2 = fromJson(test, errs, jsonStr);
            assertEquals(1, errs.errs().size());
            errs.checkErrors("", true);
        }
    }


    @Test
    void fromJson_VStruct() {
        TableSchema ts = cfg.findTable("ts");
        StructSchema attr = (StructSchema) cfg.findFieldable("attr");
        VStruct vAttr = ofStruct(attr, List.of(ofInt(111), ofInt(222), ofInt(333)));
        VStruct vTs = ofStruct(ts, List.of(ofInt(1), vAttr));

        JSONObject json = new ValueToJson().toJson(vTs);
        String jsonStr = """
                {"$type":"ts","id":1,"attr":{"$type":"attr","Attr":111,"Min":222,"Max":333}}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(ts, jsonStr);
        assertEquals(vTs, vStruct2);

        String jsonStr_simplified = """
                {"id":1,"attr":{"Attr":111,"Min":222,"Max":333}}""";
        VStruct vStruct_simplified = fromJson(ts, jsonStr_simplified);
        assertEquals(vTs, vStruct_simplified);
    }

    @Test
    void fromJson_VStruct_DefaultValue() {
        TableSchema ts = cfg.findTable("ts");
        String jsonStr = """
                {"id":1,"$type":"ts"}""";
        VStruct vStruct = fromJson(ts, jsonStr);

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr2 = """
                {"$type":"ts","id":1,"attr":{"$type":"attr","Attr":0,"Min":0,"Max":0}}""";
        assertEquals(jsonStr2, json.toString());
    }

    @Test
    void fromJson_VStruct_TypeNotMatch() {
        TableSchema ts = cfg.findTable("ts");
        String jsonStr = """
                {"id":1,"attr":123,"$type":"ts"}""";
        CfgValueErrs errs = CfgValueErrs.of();
        VStruct vStruct2 = fromJson(ts, errs, jsonStr);
        assertEquals(1, errs.errs().size());
    }

    @Test
    void fromJson_VListPrimitive() {
        TableSchema ts = cfg.findTable("tl");
        VList vList = ofList(List.of(ofInt(111), ofInt(222), ofInt(333)));
        VStruct vStruct = ofStruct(ts, List.of(ofInt(1), vList));

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr = """
                {"$type":"tl","id":1,"listInt1":[111,222,333]}""";
        assertEquals(jsonStr, json.toString());

        CfgValueErrs errs = CfgValueErrs.of();
        VStruct vStruct2 = fromJson(ts, errs, jsonStr);
        assertEquals(vStruct, vStruct2);
    }

    @Test
    void fromJson_VListPrimitive_DefaultValue() {
        TableSchema ts = cfg.findTable("tl");
        String jsonStr = """
                {"id":1,"$type":"tl"}""";
        VStruct vStruct = fromJson(ts, jsonStr);

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr2 = """
                {"$type":"tl","id":1,"listInt1":[]}""";
        assertEquals(jsonStr2, json.toString());
    }

    @Test
    void fromJson_ExtraFieldsAsWarning() {
        TableSchema ts = cfg.findTable("tl");
        String jsonStr = """
                {"id":1,"extra":333,"$type":"tl"}""";
        CfgValueErrs errs = CfgValueErrs.of();
        VStruct vStruct = fromJson(ts, errs, jsonStr);
        assertEquals(1, errs.warns().size());

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr2 = """
                {"$type":"tl","id":1,"listInt1":[]}""";
        assertEquals(jsonStr2, json.toString());
        errs.checkErrors("", true);
    }

    @Test
    void fromJson_VMapPrimitive() {
        TableSchema ts = cfg.findTable("tm");
        VMap vMap = ofMap(Map.of(ofInt(111), ofStr("aaa"), ofInt(222), ofStr("bbb")));
        VStruct vStruct = ofStruct(ts, List.of(ofInt(1), vMap));

        JSONObject json = new ValueToJson().toJson(vStruct); // 顺序不定
        //{"id":1,"mapIntStr":[{"key":111,"value":"aaa","$type":"$entry"},{"key":222,"value":"bbb","$type":"$entry"}],"$type":"tm"}
        VStruct vStruct2 = fromJson(ts, json.toString());
        assertEquals(vStruct, vStruct2);
    }

    @Test
    void fromJson_VMapPrimitive_DefaultValue() {
        TableSchema ts = cfg.findTable("tm");
        String jsonStr = """
                {"id":1,"$type":"tm"}""";
        VStruct vStruct = fromJson(ts, jsonStr);

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr2 = """
                {"$type":"tm","id":1,"mapIntStr":[]}""";
        assertEquals(jsonStr2, json.toString());
    }

    @Test
    void fromJson_VListStruct() {
        TableSchema ts = cfg.findTable("tls");
        StructSchema attr = (StructSchema) cfg.findFieldable("attr");
        VStruct vAttr = ofStruct(attr, List.of(ofInt(111), ofInt(222), ofInt(333)));
        VList vList = ofList(List.of(vAttr));
        VStruct vStruct = ofStruct(ts, List.of(ofInt(1), vList));

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr = """
                {"$type":"tls","id":1,"listAttr":[{"$type":"attr","Attr":111,"Min":222,"Max":333}]}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(ts, json.toString());
        assertEquals(vStruct, vStruct2);

        String jsonStr_simplified = """
                {"id":1,"listAttr":[{"Attr":111,"Min":222,"Max":333}]}""";
        VStruct vStruct_simplified = fromJson(ts, jsonStr_simplified);
        assertEquals(vStruct, vStruct_simplified);
    }


    @Test
    void fromJson_VMapInterface() {
        InterfaceSchema condition = (InterfaceSchema) cfg.findItem("condition");
        StructSchema checkItem = condition.findImpl("checkItem");
        StructSchema and = condition.findImpl("and");
        TableSchema cond = cfg.findTable("cond");

        VStruct c1 = ofStruct(checkItem, List.of(ofInt(123)));
        VStruct c2 = ofStruct(checkItem, List.of(ofInt(456)));
        VStruct vAnd = ofStruct(and, List.of(c1, c2));
        VStruct vStruct = ofStruct(cond,
                List.of(ofInt(666), ofMap(Map.of(ofInt(123456), vAnd))));

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr = """
                {"$type":"cond","id":666,"c":[{"$type":"$entry","key":123456,"value":{"$type":"condition.and","c1":{"$type":"condition.checkItem","id":123},"c2":{"$type":"condition.checkItem","id":456}}}]}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(cond, jsonStr);
        assertNotEquals(vStruct, vStruct2); // 因为vStruct里的对象 没有用VInterface包装

        String jsonStr_simplified = """
                {"id":666,"c":[{"key":123456,"value":{"$type":"and","c1":{"$type":"checkItem","id":123},"c2":{"$type":"checkItem","id":456}}}]}""";
        VStruct vStruct2_simplified = fromJson(cond, jsonStr_simplified);
        assertEquals(vStruct2, vStruct2_simplified);

        JSONObject json2 = new ValueToJson().toJson(vStruct2);
        assertEquals(jsonStr, json2.toString());
    }

    @Test
    void fromJson_VMapInterface_Equal_ToJsonStr() {
        TableSchema cond = cfg.findTable("cond");
        String jsonStr = """
                {"$type":"cond","id":666,"c":[{"$type":"$entry","key":123456,"value":{"$type":"condition.and","c1":{"$type":"condition.checkItem","id":123},"c2":{"$type":"condition.checkItem","id":456}}}]}""";

        VStruct v = fromJson(cond, jsonStr);
        assertEquals(jsonStr, new ValueToJson().toJson(v).toString());
    }

    @Test
    void fromJson_JsonParseError_When$TypeForInterfaceNotImpl() {
        TableSchema cond = cfg.findTable("cond");
        String jsonStr = """
                {"id":666,"c":[{"key":123456,"value":{"c1":{"id":123},"c2":{"id":456,"$type":"condition.checkItem"},"$type":"condition.and"},"$type":"$entry"}],"$type":"cond"}""";

        CfgValueErrs errs = CfgValueErrs.of();
        fromJson(cond, errs, jsonStr);
        errs.checkErrors("", true);
        assertEquals(1, errs.errs().size());
    }

    @Test
    void fromJson_EmbedFields_RoundTrip() {
        TableSchema test = cfg.findTable("test");
        String jsonStr = """
                {"$type":"test","id":123,"bool1":true,"long1":1234567890,"float1":3.14,"str1":"abc","$embed_bool1":true,"$embed_long1":false}""";
        CfgValueErrs errs = CfgValueErrs.of();
        VStruct vStruct = fromJson(test, errs, jsonStr);
        assertEquals(0, errs.errs().size());
        assertEquals(0, errs.warns().size()); // $embed_* 不再产生JsonHasExtraFields警告

        JSONObject json = new ValueToJson().toJson(vStruct);
        assertEquals(Boolean.TRUE, json.getBoolean("$embed_bool1"));
        assertEquals(Boolean.FALSE, json.getBoolean("$embed_long1"));

        // 再parse一次，embedFields保持一致
        VStruct vStruct2 = fromJson(test, json.toString());
        assertEquals(vStruct.embedFields(), vStruct2.embedFields());
    }

    @Test
    void fromJson_EmbedFields_NonBooleanStillWarn() {
        TableSchema test = cfg.findTable("test");
        String jsonStr = """
                {"$type":"test","id":123,"$embed_bool1":"yes"}""";
        CfgValueErrs errs = CfgValueErrs.of();
        VStruct vStruct = fromJson(test, errs, jsonStr);
        assertEquals(1, errs.warns().size()); // 非boolean的$embed_* 照旧警告
        assertNull(vStruct.embedFields());
    }

    @Test
    void fromJson_EmbedFields_NestedStruct_RoundTrip() {
        TableSchema ts = cfg.findTable("ts");
        String jsonStr = """
                {"$type":"ts","id":1,"$embed_attr":true,"attr":{"$type":"attr","Attr":111,"Min":222,"Max":333,"$embed_Attr":true,"$embed_Min":false}}""";
        VStruct vStruct = fromJson(ts, jsonStr);

        JSONObject json = new ValueToJson().toJson(vStruct);
        assertEquals(Boolean.TRUE, json.getBoolean("$embed_attr"));
        JSONObject attrJson = json.getJSONObject("attr");
        assertEquals(Boolean.TRUE, attrJson.getBoolean("$embed_Attr"));
        assertEquals(Boolean.FALSE, attrJson.getBoolean("$embed_Min"));
    }

    @Test
    void fromJson_MapEntryEmbed_RoundTrip() {
        TableSchema tm = cfg.findTable("tm");
        String jsonStr = """
                {"$type":"tm","id":1,"mapIntStr":[{"$type":"$entry","key":111,"value":"aaa","$embed_value":true},{"$type":"$entry","key":222,"value":"bbb","$embed_value":false}]}""";
        VStruct vStruct = fromJson(tm, jsonStr);

        JSONObject json = new ValueToJson().toJson(vStruct);
        JSONArray entries = json.getJSONArray("mapIntStr");
        assertEquals(2, entries.size());
        JSONObject e0 = entries.getJSONObject(0);
        assertEquals(111, e0.getIntValue("key"));
        assertEquals(Boolean.TRUE, e0.getBoolean("$embed_value"));
        JSONObject e1 = entries.getJSONObject(1);
        assertEquals(222, e1.getIntValue("key"));
        assertEquals(Boolean.FALSE, e1.getBoolean("$embed_value"));

        // 完整字符串round-trip
        VStruct vStruct2 = fromJson(tm, json.toString());
        assertEquals(json.toString(), new ValueToJson().toJson(vStruct2).toString());
    }

    @Test
    void fromJson_MapEntryNodeFold_RoundTrip() {
        TableSchema tm = cfg.findTable("tm");
        String jsonStr = """
                {"$type":"tm","id":1,"mapIntStr":[{"$type":"$entry","key":111,"value":"aaa","$fold":true,"$embed_value":false,"$note":"精锐"},{"$type":"$entry","key":222,"value":"bbb"}]}""";
        VStruct vStruct = fromJson(tm, jsonStr);

        JSONObject json = new ValueToJson().toJson(vStruct);
        JSONArray entries = json.getJSONArray("mapIntStr");
        assertEquals(2, entries.size());
        JSONObject e0 = entries.getJSONObject(0);
        assertEquals(Boolean.TRUE, e0.getBoolean("$fold"));          // 节点级折叠透传
        assertEquals(Boolean.FALSE, e0.getBoolean("$embed_value"));  // 与 $embed_value 并存
        assertEquals("精锐", e0.getString("$note"));                  // entry 备注透传
        JSONObject e1 = entries.getJSONObject(1);
        assertFalse(e1.containsKey("$fold"));                        // 无折叠的 entry 不写键
        assertFalse(e1.containsKey("$embed_value"));
        assertFalse(e1.containsKey("$note"));

        // 完整字符串round-trip
        VStruct vStruct2 = fromJson(tm, json.toString());
        assertEquals(json.toString(), new ValueToJson().toJson(vStruct2).toString());
    }

    @Test
    void fromJson_MapEntryFoldFalse_Dropped() {
        TableSchema tm = cfg.findTable("tm");
        String jsonStr = """
                {"$type":"tm","id":1,"mapIntStr":[{"$type":"$entry","key":111,"value":"aaa","$fold":false}]}""";
        VStruct vStruct = fromJson(tm, jsonStr);
        VMap vMap = (VMap) vStruct.values().get(1);
        assertNull(vMap.foldedEntries()); // $fold=false 无意义（展开即无键），与 VStruct 的 fold 处理同约定

        JSONObject json = new ValueToJson().toJson(vStruct);
        assertFalse(json.getJSONArray("mapIntStr").getJSONObject(0).containsKey("$fold"));
    }

    @Test
    void fromJson_NoEmbedFields_NoEntryEmbed() {
        TableSchema tm = cfg.findTable("tm");
        String jsonStr = """
                {"$type":"tm","id":1,"mapIntStr":[{"$type":"$entry","key":111,"value":"aaa"}]}""";
        VStruct vStruct = fromJson(tm, jsonStr);
        assertNull(vStruct.embedFields());
        VMap vMap = (VMap) vStruct.values().get(1);
        assertNull(vMap.entryEmbeds());
        assertNull(vMap.foldedEntries());
        assertNull(vMap.entryNotes());

        JSONObject json = new ValueToJson().toJson(vStruct);
        assertFalse(json.containsKey("$embed_mapIntStr"));
        JSONObject e0 = json.getJSONArray("mapIntStr").getJSONObject(0);
        assertFalse(e0.containsKey("$embed_value"));
        assertFalse(e0.containsKey("$fold"));
        assertFalse(e0.containsKey("$note"));
    }

}

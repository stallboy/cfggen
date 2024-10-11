package configgen.value;

import com.alibaba.fastjson2.JSONObject;
import configgen.data.Source;
import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;
import static configgen.value.Values.*;
import static org.junit.jupiter.api.Assertions.*;

class ValueJsonParserTest {

    private CfgSchema cfg;

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
                {"id":123,"bool1":true,"long1":1234567890,"float1":3.14,"str1":"abc","$type":"test"}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(test, jsonStr);

        assertEquals(vStruct, vStruct2);
    }

    private static VStruct fromJson(TableSchema tableSchema,
                                    String jsonStr) {
        ValueErrs errs = ValueErrs.of();
        VStruct vStruct = new ValueJsonParser(tableSchema, errs).fromJson(jsonStr, Source.DFile.of("<file>", tableSchema.name()));
        assertEquals(0, errs.errs().size());
        assertEquals(0, errs.warns().size());
        return vStruct;
    }

    private static VStruct fromJson(TableSchema tableSchema,
                                    ValueErrs errs,
                                    String jsonStr) {
        return new ValueJsonParser(tableSchema, errs).fromJson(jsonStr, Source.DFile.of("<file>", tableSchema.name()));
    }


    @Test
    void fromJson_Primitive_DefaultValue() {
        TableSchema test = cfg.findTable("test");
        VStruct vStruct = fromJson(test, """
                {"$type":"test"}""");
        JSONObject json = new ValueToJson().toJson(vStruct);

        String jsonStr = """
                {"id":0,"bool1":false,"long1":0,"float1":0.0,"str1":"","$type":"test"}""";
        assertEquals(jsonStr, json.toString());
    }

    @Test
    void fromJson_PrimitiveValue_TypeNotMatch() {
        TableSchema test = cfg.findTable("test");
        {
            String jsonStr = """
                    {"id":123,"bool1":"should be bool,but str","long1":1234567890,"float1":3.14,"str1":"abc","$type":"test"}""";
            ValueErrs errs = ValueErrs.of();
            VStruct vStruct2 = fromJson(test, errs, jsonStr);
            assertEquals(1, errs.errs().size());
            errs.checkErrors("", true);

        }

        {
            String jsonStr = """
                    {"id":123,"bool1":true,"long1":1234567890,"float1":3.14,"str1":123,"$type":"test"}""";
            ValueErrs errs = ValueErrs.of();
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
                {"id":1,"attr":{"Attr":111,"Min":222,"Max":333,"$type":"attr"},"$type":"ts"}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(ts, jsonStr);
        assertEquals(vTs, vStruct2);
    }

    @Test
    void fromJson_VStruct_DefaultValue() {
        TableSchema ts = cfg.findTable("ts");
        String jsonStr = """
                {"id":1,"$type":"ts"}""";
        VStruct vStruct = fromJson(ts, jsonStr);

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr2 = """
                {"id":1,"attr":{"Attr":0,"Min":0,"Max":0,"$type":"attr"},"$type":"ts"}""";
        assertEquals(jsonStr2, json.toString());
    }

    @Test
    void fromJson_VStruct_TypeNotMatch() {
        TableSchema ts = cfg.findTable("ts");
        String jsonStr = """
                {"id":1,"attr":123,"$type":"ts"}""";
        ValueErrs errs = ValueErrs.of();
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
                {"id":1,"listInt1":[111,222,333],"$type":"tl"}""";
        assertEquals(jsonStr, json.toString());

        ValueErrs errs = ValueErrs.of();
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
                {"id":1,"listInt1":[],"$type":"tl"}""";
        assertEquals(jsonStr2, json.toString());
    }

    @Test
    void fromJson_ExtraFieldsAsWarning() {
        TableSchema ts = cfg.findTable("tl");
        String jsonStr = """
                {"id":1,"extra":333,"$type":"tl"}""";
        ValueErrs errs = ValueErrs.of();
        VStruct vStruct = fromJson(ts, errs, jsonStr);
        assertEquals(1, errs.warns().size());

        JSONObject json = new ValueToJson().toJson(vStruct);
        String jsonStr2 = """
                {"id":1,"listInt1":[],"$type":"tl"}""";
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
                {"id":1,"mapIntStr":[],"$type":"tm"}""";
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
                {"id":1,"listAttr":[{"Attr":111,"Min":222,"Max":333,"$type":"attr"}],"$type":"tls"}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(ts, json.toString());
        assertEquals(vStruct, vStruct2);
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
                {"id":666,"c":[{"key":123456,"value":{"c1":{"id":123,"$type":"condition.checkItem"},"c2":{"id":456,"$type":"condition.checkItem"},"$type":"condition.and"},"$type":"$entry"}],"$type":"cond"}""";
        assertEquals(jsonStr, json.toString());

        VStruct vStruct2 = fromJson(cond, jsonStr);
        assertNotEquals(vStruct, vStruct2); // 因为vStruct里的对象 没有用VInterface包装

        JSONObject json2 = new ValueToJson().toJson(vStruct2);
        assertEquals(jsonStr, json2.toString());

    }

    @Test
    void fromJson_VMapInterface_Equal_ToJsonStr() {
        TableSchema cond = cfg.findTable("cond");
        String jsonStr = """
                {"id":666,"c":[{"key":123456,"value":{"c1":{"id":123,"$type":"condition.checkItem"},"c2":{"id":456,"$type":"condition.checkItem"},"$type":"condition.and"},"$type":"$entry"}],"$type":"cond"}""";

        VStruct v = fromJson(cond, jsonStr);
        assertEquals(jsonStr, new ValueToJson().toJson(v).toString());
    }

    @Test
    void fromJson_JsonParseError_When$TypeForInterfaceNotImpl() {
        TableSchema cond = cfg.findTable("cond");
        String jsonStr = """
                {"id":666,"c":[{"key":123456,"value":{"c1":{"id":123},"c2":{"id":456,"$type":"condition.checkItem"},"$type":"condition.and"},"$type":"$entry"}],"$type":"cond"}""";

        ValueErrs errs = ValueErrs.of();
        fromJson(cond, errs, jsonStr);
        errs.checkErrors("", true);
        assertEquals(1, errs.errs().size());
    }

}

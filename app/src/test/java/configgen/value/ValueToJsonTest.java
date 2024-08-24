package configgen.value;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.value.CfgValue.*;
import static configgen.value.Values.*;
import static org.junit.jupiter.api.Assertions.*;

class ValueToJsonTest {


    private final ValueToJson valueToJson = new ValueToJson();

    @Test
    void toJson_PrimitiveValue() {
        {
            VInt vInt = ofInt(123);
            int v = (int) valueToJson.toJson(vInt);
            assertEquals(123, v);
        }
        {
            String str = "test str";
            VString vString = ofStr(str);
            String v = (String) valueToJson.toJson(vString);
            assertEquals(str, v);
        }
    }

    @Test
    void toJson_ListPrimitiveValue() {
        {
            VList vList = VList.of(List.of(ofInt(123), ofInt(222), ofInt(333)));
            JSONArray ja = valueToJson.toJson(vList);
            assertEquals("[123,222,333]", ja.toString());
            assertEquals(123, ja.get(0));
            assertEquals(222, ja.get(1));
            assertEquals(333, ja.get(2));
        }
        {
            String jsonStr = """
                    ["aaa","bbb"]""";
            VList vList = VList.of(List.of(ofStr("aaa"), ofStr("bbb")));
            JSONArray ja = valueToJson.toJson(vList);
            assertEquals(jsonStr, ja.toString());
        }
    }

    @Test
    void toJson_MapPrimitiveValue() {
        VMap vMap = ofMap(Map.of(ofInt(111), ofStr("aaa"), ofInt(222), ofStr("bbb")));
        JSONArray ja = valueToJson.toJson(vMap);
        // [{"key":111,"value":"aaa","$type":"$entry"},{"key":222,"value":"bbb","$type":"$entry"}]
        assertEquals(2, ja.size());
        assertEquals(Set.of(111, 222), Set.of(ja.getJSONObject(0).getIntValue("key"),
                ja.getJSONObject(1).getIntValue("key")));
        assertEquals(Set.of("aaa", "bbb"), Set.of(ja.getJSONObject(0).getString("value"),
                ja.getJSONObject(1).getString("value")));
    }


    @Test
    void toJson_VStruct() {
        StructSchema ss = new StructSchema("structName", AUTO, Metadata.of(),
                List.of(new FieldSchema("fieldIntA", FieldType.Primitive.INT, AUTO, Metadata.of()),
                        new FieldSchema("fieldStrB", FieldType.Primitive.STRING, AUTO, Metadata.of())),
                List.of());
        VStruct vStruct = ofStruct(ss, List.of(ofInt(123), ofStr("test str")));
        JSONObject json = valueToJson.toJson(vStruct);
        assertEquals(3, json.values().size());

        assertEquals(123, json.getIntValue("fieldIntA"));
        assertEquals("test str", json.getString("fieldStrB"));
        assertEquals("structName", json.getString("$type"));
    }

    @Test
    void toJson_ListVStruct() {
        StructSchema ss = new StructSchema("structName", AUTO, Metadata.of(),
                List.of(new FieldSchema("fieldIntA", FieldType.Primitive.INT, AUTO, Metadata.of()),
                        new FieldSchema("fieldStrB", FieldType.Primitive.STRING, AUTO, Metadata.of())),
                List.of());
        VStruct v1 = ofStruct(ss, List.of(ofInt(123), ofStr("test str")));
        VStruct v2 = ofStruct(ss, List.of(ofInt(456), ofStr("bbb")));
        VList vList = VList.of(List.of(v1, v2));
        JSONArray ja = valueToJson.toJson(vList);

        String jsonStr = """
                [{"fieldIntA":123,"fieldStrB":"test str","$type":"structName"},{"fieldIntA":456,"fieldStrB":"bbb","$type":"structName"}]""";
        assertEquals(jsonStr, ja.toString());
    }

    @Test
    void toJson_VInterface() {
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
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve();

        InterfaceSchema condition = (InterfaceSchema) cfg.findItem("condition");
        StructSchema checkItem = condition.findImpl("checkItem");
        StructSchema and = condition.findImpl("and");

        VStruct c1 = ofStruct(checkItem, List.of(ofInt(123)));
        VStruct c2 = ofStruct(checkItem, List.of(ofInt(456)));
        VStruct vAnd = ofStruct(and, List.of(c1, c2));
        VInterface vCond = ofStruct(condition, vAnd, ofCell("and"));

        JSONObject json = valueToJson.toJson(vCond);
        String jsonStr = """
                {"c1":{"id":123,"$type":"condition.checkItem"},"c2":{"id":456,"$type":"condition.checkItem"},"$type":"condition.and"}""";
        assertEquals(jsonStr, json.toString());
        assertEquals("condition.and", json.getString("$type"));
        assertEquals(123, json.getJSONObject("c1").getIntValue("id"));
        assertEquals(456, json.getJSONObject("c2").getIntValue("id"));
    }


    @Test
    void toJson_MapVInterface() {
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
                table cond[id] (json){
                    id:int;
                    c:map<int,condition>;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve();

        InterfaceSchema condition = (InterfaceSchema) cfg.findItem("condition");
        StructSchema checkItem = condition.findImpl("checkItem");
        StructSchema and = condition.findImpl("and");
        TableSchema cond = cfg.findTable("cond");

        VStruct c1 = ofStruct(checkItem, List.of(ofInt(123)));
        VStruct c2 = ofStruct(checkItem, List.of(ofInt(456)));
        VStruct vAnd = ofStruct(and, List.of(c1, c2));
//        VInterface vCond = ofStruct(condition, vAnd, ofCell("and")); // 可以不要这层抽象

        VStruct vStruct = ofStruct(cond,
                List.of(ofInt(666), ofMap(Map.of(ofInt(123456), vAnd))));


        JSONObject json = valueToJson.toJson(vStruct);
        String jsonStr = """
                {"id":666,"c":[{"key":123456,"value":{"c1":{"id":123,"$type":"condition.checkItem"},"c2":{"id":456,"$type":"condition.checkItem"},"$type":"condition.and"},"$type":"$entry"}],"$type":"cond"}""";
        assertEquals(jsonStr, json.toString());
    }
}

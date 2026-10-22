package configgen.editorserver;

import com.alibaba.fastjson2.JSONObject;
import configgen.schema.CfgSchema;
import configgen.schema.TableSchema;
import configgen.schema.cfg.CfgReader;
import configgen.value.CfgValue;
import configgen.value.CfgValueErrs;
import configgen.value.ValueJsonParser;
import configgen.value.ValueToJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EditorValueJsonTest {
    private static TableSchema table() {
        CfgSchema schema = CfgReader.parse("""
                struct Key { id:long; }
                interface Variant { struct Item { id:long; } }
                table t[id] {
                    id:long;
                    ids:list<long> (pack);
                    keys:map<long,long> (pack);
                    objects:map<Key,Variant> (pack);
                }
                """);
        schema.resolve().checkErrors();
        return schema.findTable("t");
    }

    @Test
    void editorLongRoundTripIncludesEveryContainerAndLeavesDefaultSerializerNumeric() {
        TableSchema table = table();
        String raw = """
                {"id":"9007199254740993","ids":["-9223372036854775808","9223372036854775807","-1","0"],
                 "keys":[{"key":"9007199254740993","value":"-9007199254740993"}],
                 "objects":[{"key":{"id":"9007199254740993"},"value":{"$type":"Variant.Item","id":"9223372036854775807"}}]}
                """;
        CfgValueErrs errors = CfgValueErrs.of();
        CfgValue.VStruct value = new ValueJsonParser(table, errors).fromJson(EditorValueJson.decode(table, raw));
        assertTrue(errors.errs().isEmpty(), errors.toString());
        JSONObject encoded = ValueToJson.forEditor(null, null, null).toJson(value);
        assertEquals("9007199254740993", encoded.get("id"));
        assertEquals("-9223372036854775808", encoded.getJSONArray("ids").get(0));
        assertEquals("9007199254740993", encoded.getJSONArray("keys").getJSONObject(0).get("key"));
        assertEquals("9223372036854775807", encoded.getJSONArray("objects").getJSONObject(0).getJSONObject("value").get("id"));
        assertEquals(new ValueToJson().toJson(value).toJSONString(), EditorValueJson.decode(table, encoded.toJSONString()));
        assertInstanceOf(Long.class, new ValueToJson().toJson(value).get("id"));
    }

    @Test
    void acceptsOnlySafeLegacyNumbersOrValidDecimalStrings() {
        TableSchema table = table();
        for (String input : new String[]{"9007199254740991", "-9007199254740991", "0", "\"9223372036854775807\"", "\"-9223372036854775808\""}) {
            assertDoesNotThrow(() -> EditorValueJson.decode(table, "{\"id\":" + input + "}"));
        }
        for (String input : new String[]{"9007199254740992", "9007199254740993", "-9007199254740992", "1.5", "\"1.5\"", "\"9223372036854775808\"", "\"-9223372036854775809\"", "\"1e3\""}) {
            assertThrows(IllegalArgumentException.class, () -> EditorValueJson.decode(table, "{\"id\":" + input + "}"), input);
        }
        assertThrows(IllegalArgumentException.class, () -> EditorValueJson.decode(table, "{\"objects\":[{\"key\":{\"id\":9007199254740993}}]}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"42.00000000000000000001", "9007199254740991.1", "1e-400", "4.200000000000000000001e1"})
    void rejectsFractionalLongNumbersBeforeAnyRounding(String number) {
        assertThrows(IllegalArgumentException.class, () -> EditorValueJson.decode(table(), "{\"id\":" + number + "}"));
    }

    @Test
    void acceptsSafeIntegralScientificNotation() {
        assertEquals(1000L, JSONObject.parseObject(EditorValueJson.decode(table(), "{\"id\":1e3}")).getLongValue("id"));
    }
}

package configgen.editorserver;

import com.alibaba.fastjson2.JSONObject;
import configgen.schema.CfgSchema;
import configgen.schema.TableSchema;
import configgen.schema.cfg.CfgReader;
import configgen.value.CfgValue;
import configgen.value.CfgValueErrs;
import configgen.value.VTableCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static configgen.editorserver.CheckJsonService.CheckJsonResultCode.ParseJsonError;
import static configgen.editorserver.CheckJsonService.CheckJsonResultCode.ok;
import static org.junit.jupiter.api.Assertions.*;

class CheckJsonServiceTest {
    private static CfgValue cfgValue() {
        CfgSchema schema = CfgReader.parse("table t[id] { id:long; note:str; }");
        schema.resolve().checkErrors();
        TableSchema table = schema.findTable("t");
        CfgValue cfgValue = CfgValue.of(schema);
        cfgValue.vTableMap().put("t", new VTableCreator(table, CfgValueErrs.of()).create(List.of()));
        return cfgValue;
    }

    @ParameterizedTest
    @ValueSource(strings = {"42.00000000000000000001", "9007199254740991.1", "1e-400", "4.200000000000000000001e1"})
    void rejectsFractionalLongNumbersFromOriginalJsonText(String number) {
        String json = "{\"id\":" + number + "}";
        for (String raw : List.of(json, "```json\n" + json + "\n```")) {
            var result = CheckJsonService.checkJson(cfgValue(), "t", raw);
            assertEquals(ParseJsonError, result.resultCode(), raw);
            assertTrue(result.jsonResult().contains("t.id"), result.jsonResult());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"id\":1e3}", " \n{\"id\":1e3}", "AI response\n```json\n{\"id\":1e3}\n```\nDone"})
    void normalizesSafeIntegralScientificNotationForEditorTransport(String raw) {
        var result = CheckJsonService.checkJson(cfgValue(), "t", raw);
        assertEquals(ok, result.resultCode(), result.jsonResult());
        assertEquals("1000", JSONObject.parseObject(result.jsonResult()).get("id"));
    }

    @Test
    void leavesFenceTextInsideRawJsonStringsUntouched() {
        String note = "literal ```json [1,2] ``` tail";
        String raw = "{\"id\":\"9007199254740993\",\"note\":\"" + note + "\"}";
        var result = CheckJsonService.checkJson(cfgValue(), "t", raw);
        assertEquals(ok, result.resultCode(), result.jsonResult());
        JSONObject normalized = JSONObject.parseObject(result.jsonResult());
        assertEquals("9007199254740993", normalized.get("id"));
        assertEquals(note, normalized.get("note"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "No JSON here", "```text\n{}\n```"})
    void retainsJsonNotFoundForNonJsonInput(String raw) {
        var result = CheckJsonService.checkJson(cfgValue(), "t", raw);
        assertEquals(CheckJsonService.CheckJsonResultCode.JsonNotFound, result.resultCode());
    }
}

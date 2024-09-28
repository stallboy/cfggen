package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ValueRefCollectorTest {

    private @TempDir Path tempDir;

    @Test
    void collectRef() {
        String cfgStr = """
                table t[id] {
                    id:int;
                    next:int ->t;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,next
                1,2
                2,2
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VStruct firstRecord = cfgValue.getTable("t").valueList().get(0);
        CfgValue.VStruct secondRecord = cfgValue.getTable("t").valueList().get(1);

        List<ValueRefCollector.FieldRef> fieldRefs = new ArrayList<>();
        Map<ValueRefCollector.RefId, CfgValue.VStruct> refIdToRecordMap = new LinkedHashMap<>();
        ValueRefCollector collector = new ValueRefCollector(cfgValue, refIdToRecordMap, fieldRefs);
        collector.collect(firstRecord, List.of());

        assertEquals(1, refIdToRecordMap.size());
        assertEquals(1, fieldRefs.size());
        Map.Entry<ValueRefCollector.RefId, CfgValue.VStruct> ref2record = refIdToRecordMap.entrySet().iterator().next();
        assertEquals("t", ref2record.getKey().table());
        assertEquals("2", ref2record.getKey().id());
        assertEquals(secondRecord, ref2record.getValue());

        ValueRefCollector.FieldRef fieldRef = fieldRefs.getFirst();
        assertEquals("next", fieldRef.firstField());
        assertEquals("refNext", fieldRef.label());
        assertEquals("t", fieldRef.toTable());
        assertEquals("2", fieldRef.toId());
    }


    @Test
    void collectRef_inSubStruct() {
        String cfgStr = """
                struct s {
                    note:str;
                    next:int ->t (nullable);
                }
                table t[id] {
                    id:int;
                    s:s;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        String csvStr = """
                ,,,,
                id,s.note,s.next
                1,note1,2
                2,note2,
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VStruct firstRecord = cfgValue.getTable("t").valueList().get(0);
        CfgValue.VStruct secondRecord = cfgValue.getTable("t").valueList().get(1);

        List<ValueRefCollector.FieldRef> fieldRefs = new ArrayList<>();
        Map<ValueRefCollector.RefId, CfgValue.VStruct> refIdToRecordMap = new LinkedHashMap<>();
        ValueRefCollector collector = new ValueRefCollector(cfgValue, refIdToRecordMap, fieldRefs);
        collector.collect(firstRecord, List.of());

        assertEquals(1, refIdToRecordMap.size());
        assertEquals(1, fieldRefs.size());
        Map.Entry<ValueRefCollector.RefId, CfgValue.VStruct> ref2record = refIdToRecordMap.entrySet().iterator().next();
        assertEquals("t", ref2record.getKey().table());
        assertEquals("2", ref2record.getKey().id());
        assertEquals(secondRecord, ref2record.getValue());

        ValueRefCollector.FieldRef fieldRef = fieldRefs.getFirst();
        assertEquals("next", fieldRef.firstField());  // 第一个字段会被记录
        assertEquals("s.nullableRefNext", fieldRef.label());
        assertEquals("t", fieldRef.toTable());
        assertEquals("2", fieldRef.toId());
    }

}

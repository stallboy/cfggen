package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;
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

        Context ctx = Context.load(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        var records = requireNonNull(cfgValue.getTable("t")).valueList();
        CfgValue.VStruct firstRecord = records.get(0);
        CfgValue.VStruct secondRecord = records.get(1);

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
    void uniqueKeyReferenceUsesTargetPrimaryKeyAsRecordId() {
        Resources.addTempFileFromText("config.cfg", tempDir, """
                table item[id] {
                    [code];
                    id:int;
                    code:str;
                }
                table source[id] {
                    id:int;
                    itemCode:str ->item[code];
                }
                """);
        Resources.addTempFileFromText("item.csv", tempDir, "ID,Code\nid,code\n7,sword\n");
        Resources.addTempFileFromText("source.csv", tempDir, "ID,Code\nid,itemCode\n1,sword\n");

        CfgValue data = Context.load(tempDir).makeValue();
        var source = requireNonNull(data.getTable("source")).valueList().getFirst();
        var item = requireNonNull(data.getTable("item")).valueList().getFirst();
        List<ValueRefCollector.FieldRef> fieldRefs = new ArrayList<>();
        Map<ValueRefCollector.RefId, CfgValue.VStruct> targets = new LinkedHashMap<>();
        new ValueRefCollector(data, targets, fieldRefs).collect(source, List.of());

        assertEquals(Map.of(new ValueRefCollector.RefId("item", "7"), item), targets);
        assertEquals("7", fieldRefs.getFirst().toId());
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

        Context ctx = Context.load(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        var records = requireNonNull(cfgValue.getTable("t")).valueList();
        CfgValue.VStruct firstRecord = records.get(0);
        CfgValue.VStruct secondRecord = records.get(1);

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

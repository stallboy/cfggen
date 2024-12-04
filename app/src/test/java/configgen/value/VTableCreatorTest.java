package configgen.value;

import configgen.schema.CfgSchema;
import configgen.schema.TableSchema;
import configgen.schema.cfg.CfgReader;
import configgen.value.CfgValue.VStruct;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

import static configgen.value.CfgValue.*;
import static configgen.value.Values.*;
import static org.junit.jupiter.api.Assertions.*;

class VTableCreatorTest {
    @Test
    void create_primaryKey() {
        String str = """
                table t[id]{
                    id:int;
                    s:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        TableSchema t = cfg.findTable("t");

        VStruct vStruct = ofStruct(t, List.of(ofInt(123), ofStr("abc")));
        VStruct vStruct2 = ofStruct(t, List.of(ofInt(456), ofStr("efg")));
        CfgValueErrs errs = CfgValueErrs.of();
        List<VStruct> vStructList = List.of(vStruct, vStruct2);
        VTable vTable = new VTableCreator(t, errs).create(vStructList);

        assertEquals(t, vTable.schema());
        assertEquals("t", vTable.name());
        assertEquals(vStructList, vTable.valueList());

        assertEquals(2, vTable.primaryKeyMap().size());
        assertEquals(vStruct, vTable.primaryKeyMap().get(ofInt(123)));
        assertEquals(vStruct2, vTable.primaryKeyMap().get(ofInt(456)));
        assertEquals(0, vTable.uniqueKeyMaps().size());
        assertNull(vTable.enumNames());
        assertNull(vTable.enumNameToIntegerValueMap());
    }

    @Test
    void create_enum() {
        String str = """
                table t[id] (enum='s'){
                    id:int;
                    s:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        TableSchema t = cfg.findTable("t");

        VStruct vStruct = ofStruct(t, List.of(ofInt(123), ofStr("abc")));
        VStruct vStruct2 = ofStruct(t, List.of(ofInt(456), ofStr("efg")));
        CfgValueErrs errs = CfgValueErrs.of();
        List<VStruct> vStructList = List.of(vStruct, vStruct2);
        VTable vTable = new VTableCreator(t, errs).create(vStructList);

        assertEquals(Set.of("abc", "efg"), vTable.enumNames());
        assertEquals(Map.of("abc", 123, "efg", 456), vTable.enumNameToIntegerValueMap());
    }


    @Test
    void create_uniqKeys() {
        String str = """
                table t[id] {
                    [s];
                    id:int;
                    s:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        TableSchema t = cfg.findTable("t");

        VStruct vStruct = ofStruct(t, List.of(ofInt(123), ofStr("abc")));
        VStruct vStruct2 = ofStruct(t, List.of(ofInt(456), ofStr("efg")));
        CfgValueErrs errs = CfgValueErrs.of();
        List<VStruct> vStructList = List.of(vStruct, vStruct2);
        VTable vTable = new VTableCreator(t, errs).create(vStructList);

        assertEquals(1, vTable.uniqueKeyMaps().size());
        SequencedMap<Value, VStruct> uk = vTable.uniqueKeyMaps().get(List.of("s"));
        assertEquals(2, uk.size());
        assertEquals(vStruct, uk.get(ofStr("abc")));
        assertEquals(vStruct2, uk.get(ofStr("efg")));
    }


    @Test
    void create_multiFieldsAsKey() {
        String str = """
                table t[k1, k2] {
                    k1:int;
                    k2:str;
                    v:long;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        TableSchema t = cfg.findTable("t");

        VStruct vStruct = ofStruct(t, List.of(ofInt(123), ofStr("abc"), ofLong(123456)));
        VStruct vStruct2 = ofStruct(t, List.of(ofInt(456), ofStr("efg"), ofLong(456789)));
        CfgValueErrs errs = CfgValueErrs.of();
        List<VStruct> vStructList = List.of(vStruct, vStruct2);
        VTable vTable = new VTableCreator(t, errs).create(vStructList);

        assertEquals(2, vTable.primaryKeyMap().size());
        {
            VList k1 = ofList(List.of(ofInt(123), ofStr("abc")));
            assertEquals(vStruct, vTable.primaryKeyMap().get(k1));
        }
        {
            VList k2 = ofList(List.of(ofInt(456), ofStr("efg")));
            assertEquals(vStruct2, vTable.primaryKeyMap().get(k2));
        }
    }

}

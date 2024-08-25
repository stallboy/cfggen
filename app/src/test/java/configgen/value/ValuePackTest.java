package configgen.value;

import configgen.data.CfgData;
import configgen.schema.*;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldType.*;
import static configgen.value.CfgValue.*;
import static configgen.value.Values.*;
import static org.junit.jupiter.api.Assertions.*;

class ValuePackTest {
    @Test
    void packList() {
        {
            Value value = ofList(List.of(ofInt(1), ofInt(2), ofInt(3)));
            String str = ValuePack.pack(value);
            assertEquals("1,2,3", str);
            FList flist = new FList(Primitive.INT);

            ValueErrs errs = ValueErrs.of();
            Value unpackValue = ValuePack.unpack(str, flist, errs);
            assertEquals(0, errs.errs().size());
            assertEquals(value, unpackValue);
        }

        {
            Value value = ofList(List.of(ofInt(123)));
            String str = ValuePack.pack(value);
            assertEquals("123", str);
            FList flist = new FList(Primitive.INT);

            ValueErrs errs = ValueErrs.of();
            Value unpackValue = ValuePack.unpack(str, flist, errs);
            assertEquals(0, errs.errs().size());
            assertEquals(value, unpackValue);
        }
    }

    @Test
    void packList_Error_NotMatchFieldType_ReturnList() {
        String str = "1,2,1.2";
        FList flist = new FList(Primitive.INT);

        ValueErrs errs = ValueErrs.of();
        VList unpackValue = (VList) ValuePack.unpack(str, flist, errs);
        assertEquals(1, errs.errs().size());
        assertEquals(3, unpackValue.valueList().size());  // 1.2 导致err，但默认用0填充
        assertEquals(0, ((VInt) (unpackValue.valueList().getLast())).value());

        ValueErrs.VErr err = errs.errs().getFirst();
        assertInstanceOf(ValueErrs.NotMatchFieldType.class, err);
        assertTrue(((ValueErrs.NotMatchFieldType) err).source() instanceof CfgData.DCell cell &&
                cell.value().equals("1.2"));

    }


    @Test
    void packList_Error_NotMatchFieldType2_ReturnList() {
        String str = "(1,2)";
        FList flist = new FList(Primitive.INT);

        ValueErrs errs = ValueErrs.of();
        VList unpackValue = (VList) ValuePack.unpack(str, flist, errs);
        assertEquals(1, errs.errs().size());
        assertEquals(1, unpackValue.valueList().size());  // 1,2 导致err，但默认用0填充
        assertEquals(0, ((VInt) (unpackValue.valueList().getFirst())).value());

        ValueErrs.VErr err = errs.errs().getFirst();
        assertInstanceOf(ValueErrs.NotMatchFieldType.class, err);
        assertTrue(((ValueErrs.NotMatchFieldType) err).source() instanceof CfgData.DCell cell &&
                cell.value().equals("1,2"));
    }

    @Test
    void packList_Error_ParsePackErr_ReturnNull() {
        String str = "(1,2)3";
        FList flist = new FList(Primitive.INT);

        ValueErrs errs = ValueErrs.of();
        Value unpackValue = ValuePack.unpack(str, flist, errs);
        assertEquals(1, errs.errs().size());
        assertNull(unpackValue);
        ValueErrs.VErr err = errs.errs().getFirst();
        assertInstanceOf(ValueErrs.ParsePackErr.class, err);
        assertTrue(((ValueErrs.ParsePackErr) err).source() instanceof CfgData.DCell cell &&
                cell.value().equals(str));
    }

    @Test
    void packMap() {
        {
            FMap fmap = new FMap(Primitive.INT, Primitive.LONG);
            ValueErrs errs = ValueErrs.of();
            Value value = ValuePack.unpack("1,111,2, 222", fmap, errs);
            assertEquals(0, errs.errs().size());
            assertEquals("1,111,2,222", value.packStr());
            assertTrue(value instanceof VMap v && v.valueMap().size() == 2);
        }
        {
            FMap fmap = new FMap(Primitive.INT, Primitive.STRING);
            ValueErrs errs = ValueErrs.of();
            Value value = ValuePack.unpack("1,abc,2, def", fmap, errs);
            assertEquals(0, errs.errs().size());
            assertEquals("1,abc,2,def", value.packStr());
            assertTrue(value instanceof VMap v && v.valueMap().size() == 2);
        }
        {
            FMap fmap = new FMap(Primitive.INT, Primitive.BOOL);
            ValueErrs errs = ValueErrs.of();
            Value value = ValuePack.unpack(" 1, 1, 2, 0", fmap, errs);
            assertEquals(0, errs.errs().size());
            assertEquals("1,true,2,false", value.packStr());
            assertTrue(value instanceof VMap v && v.valueMap().size() == 2);
        }
    }

    @Test
    void packStruct() {

        StructSchema ss = new StructSchema("structName", AUTO, Metadata.of(),
                List.of(new FieldSchema("fieldIntA", Primitive.INT, AUTO, Metadata.of()),
                        new FieldSchema("fieldStrB", Primitive.STRING, AUTO, Metadata.of())),
                List.of());
        StructRef struct = new StructRef("ss");
        struct.setObj(ss);

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpack(" 11, abc", struct, errs);
        assertEquals(0, errs.errs().size());
        assertEquals("11,abc", value.packStr());
        assertTrue(value instanceof VStruct v && v.values().size() == 2);
    }

    @Test
    void packStruct_hasSubStruct() {
        StructSchema ss = new StructSchema("structA", AUTO, Metadata.of(),
                List.of(new FieldSchema("fieldIntA", Primitive.INT, AUTO, Metadata.of()),
                        new FieldSchema("fieldStrB", Primitive.STRING, AUTO, Metadata.of())),
                List.of());
        StructRef structA = new StructRef("structA");
        structA.setObj(ss);

        StructSchema sb = new StructSchema("structB", AUTO, Metadata.of(),
                List.of(new FieldSchema("fieldInt", Primitive.INT, AUTO, Metadata.of()),
                        new FieldSchema("fieldStructA", structA, AUTO, Metadata.of())),
                List.of());
        StructRef structB = new StructRef("structB");
        structB.setObj(sb);

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpack(" 11, (22, abc)", structB, errs);
        assertEquals(0, errs.errs().size());
        assertEquals("11,(22,abc)", value.packStr());
        assertTrue(value instanceof VStruct v && v.values().size() == 2);
    }


    @Test
    void packList_Struct() {
        StructSchema ss = new StructSchema("structA", AUTO, Metadata.of(),
                List.of(new FieldSchema("fieldIntA", Primitive.INT, AUTO, Metadata.of()),
                        new FieldSchema("fieldStrB", Primitive.STRING, AUTO, Metadata.of())),
                List.of());
        StructRef structA = new StructRef("structA");
        structA.setObj(ss);
        FList flist = new FList(structA);

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpack(" (11,aaa), (22, abc)", flist, errs);
        assertEquals(0, errs.errs().size());
        assertEquals("(11,aaa),(22,abc)", value.packStr());
        assertTrue(value instanceof VList v && v.valueList().size() == 2);
    }

    @Test
    void packStruct_hasListIn() {
        FList flist = new FList(Primitive.INT);
        StructSchema ss = new StructSchema("structA", AUTO, Metadata.of(),
                List.of(new FieldSchema("fieldIntA", Primitive.INT, AUTO, Metadata.of()),
                        new FieldSchema("fieldListB", flist, AUTO, Metadata.of())),
                List.of());
        StructRef structA = new StructRef("structA");
        structA.setObj(ss);

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpack(" 11, (22, 33,44)", structA, errs);
        assertEquals(0, errs.errs().size());
        assertEquals("11,(22,33,44)", value.packStr());
        assertTrue(value instanceof VStruct v && v.values().size() == 2);
        VStruct v = (VStruct) value;
        VList vl = (VList) v.values().get(1);
        assertEquals(3, vl.valueList().size());
    }

    @Test
    void packInterface_simple() {
        String str = """
                interface condition {
                    struct checkItem {
                        id:int;
                    }
                    struct and {
                        c1:condition ;
                        c2:condition;
                    }
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve();
        StructRef struct = new StructRef("ss");
        struct.setObj(cfg.findFieldable("condition"));

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpack("checkItem(123)", struct, errs);
        assertEquals(0, errs.errs().size());
        assertEquals("checkItem(123)", value.packStr());
        assertInstanceOf(VInterface.class, value);
    }

    @Test
    void packInterface_nested() {
        String str = """
                interface condition {
                    struct checkItem {
                        id:int;
                    }
                    struct and {
                        c1:condition ;
                        c2:condition;
                    }
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        SchemaErrs errs1 = cfg.resolve();
        assertEquals(0, errs1.errs().size());

        StructRef struct = new StructRef("ss");
        struct.setObj(cfg.findFieldable("condition"));

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpack("and(checkItem(123), checkItem(456))", struct, errs);
        assertEquals(0, errs.errs().size());
        assertEquals("and(checkItem(123),checkItem(456))", value.packStr());
        assertInstanceOf(VInterface.class, value);
    }

    @Test
    void packInterface_nestedImplRef() {
        String str = """
                interface condition {
                    struct checkItem {
                        id:int;
                    }
                    struct and {
                        c1:checkItem ;
                        c2:condition;
                    }
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        SchemaErrs errs1 = cfg.resolve();
        assertEquals(0, errs1.errs().size());

        StructRef struct = new StructRef("ss");
        struct.setObj(cfg.findFieldable("condition"));

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpack("and((123), checkItem(456))", struct, errs);
        assertEquals(0, errs.errs().size());
        assertEquals("and((123),checkItem(456))", value.packStr());
        assertInstanceOf(VInterface.class, value);
    }


    @Test
    void packInterface_hasList() {
        String str = """
                interface condition {
                    struct checkItem {
                        id:int;
                    }
                    struct and {
                        conds:list<condition>;
                    }
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        SchemaErrs errs1 = cfg.resolve();
        assertEquals(0, errs1.errs().size());

        StructRef struct = new StructRef("ss");
        struct.setObj(cfg.findFieldable("condition"));

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpack("and( ( checkItem(123), checkItem(456 )) )", struct, errs);
        assertEquals(0, errs.errs().size());
        assertEquals("and((checkItem(123),checkItem(456)))", value.packStr());
        assertInstanceOf(VInterface.class, value);
    }


    @Test
    void unpackPrimaryKey_oneKey() {
        String str = """
                table t[k1] {
                    k1:int;
                    v:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        TableSchema t = cfg.findTable("t");

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpackTablePrimaryKey("11", t, errs);
        assertEquals(0, errs.errs().size());
        assertInstanceOf(VInt.class, value);
        assertEquals("11", value.packStr());
        VInt vInt = (VInt) value;
        assertEquals(11, vInt.value());
    }

    @Test
    void unpackPrimaryKey_multiKey() {
        String str = """
                table t[k1,k2] {
                    k1:int;
                    k2:int;
                    v:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        cfg.resolve().checkErrors();
        TableSchema t = cfg.findTable("t");

        ValueErrs errs = ValueErrs.of();
        Value value = ValuePack.unpackTablePrimaryKey("11,22", t, errs);
        assertEquals(0, errs.errs().size());
        assertInstanceOf(VList.class, value);
        assertEquals("11,22", value.packStr());
        VList v = (VList) value;
        assertTrue(v.valueList().get(0) instanceof VInt vInt && vInt.value() == 11);
        assertTrue(v.valueList().get(1) instanceof VInt vInt && vInt.value() == 22);
    }

}

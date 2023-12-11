package configgen.value;

import configgen.data.CfgData;
import configgen.schema.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldType.*;
import static configgen.value.CfgValue.*;
import static org.junit.jupiter.api.Assertions.*;

class ValuePackTest {

    private static CfgData.DCell ofCell(String str) {
        return new CfgData.DCell(str, new CfgData.DRowId("fileName", "sheetName", 0), 0, (byte) 0);
    }

    private static VInt ofInt(int v) {
        return new VInt(v, ofCell(String.valueOf(v)));
    }

    @Test
    void packList() {
        {
            Value value = VList.of(List.of(ofInt(1), ofInt(2), ofInt(3)));
            String str = ValuePack.pack(value);
            assertEquals(str, "1,2,3");

            FList flist = new FList(Primitive.INT);
            ValueErrs errs = ValueErrs.of();
            Value unpackValue = ValuePack.unpack(str, flist, errs);
            assertEquals(errs.errs().size(), 0);
            assertEquals(unpackValue, value);
        }

        {
            Value value = VList.of(List.of(ofInt(123)));
            String str = ValuePack.pack(value);
            assertEquals(str, "123");

            FList flist = new FList(Primitive.INT);
            ValueErrs errs = ValueErrs.of();
            Value unpackValue = ValuePack.unpack(str, flist, errs);
            assertEquals(errs.errs().size(), 0);

            assertEquals(unpackValue, value);
        }
    }

    @Test
    void packMap() {
        {
            FMap fmap = new FMap(Primitive.INT, Primitive.LONG);
            ValueErrs errs = ValueErrs.of();
            Value value = ValuePack.unpack("1,111,2, 222", fmap, errs);
            assertEquals(errs.errs().size(), 0);
            assertEquals(value.packStr(), "1,111,2,222");
            assertTrue(value instanceof VMap v && v.valueMap().size() == 2);
        }
        {
            FMap fmap = new FMap(Primitive.INT, Primitive.STRING);
            ValueErrs errs = ValueErrs.of();
            Value value = ValuePack.unpack("1,abc,2, def", fmap, errs);
            assertEquals(errs.errs().size(), 0);
            assertEquals(value.packStr(), "1,abc,2,def");
            assertTrue(value instanceof VMap v && v.valueMap().size() == 2);
        }

        {
            FMap fmap = new FMap(Primitive.INT, Primitive.BOOL);
            ValueErrs errs = ValueErrs.of();
            Value value = ValuePack.unpack(" 1, 1, 2, 0", fmap, errs);
            assertEquals(errs.errs().size(), 0);
            assertEquals(value.packStr(), "1,true,2,false");
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
        assertEquals(errs.errs().size(), 0);
        assertEquals(value.packStr(), "11,abc");
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
        assertEquals(errs.errs().size(), 0);
        assertEquals(value.packStr(), "11,(22,abc)");
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
        assertEquals(errs.errs().size(), 0);
        assertEquals(value.packStr(), "(11,aaa),(22,abc)");
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
        assertEquals(errs.errs().size(), 0);
        assertEquals(value.packStr(), "11,(22,33,44)");
        assertTrue(value instanceof VStruct v && v.values().size() == 2);
        VStruct v = (VStruct) value;
        VList vl = (VList) v.values().get(1);
        assertEquals(vl.valueList().size(), 3);
    }
}
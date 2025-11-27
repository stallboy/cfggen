package configgen.write;

import configgen.data.Source;
import configgen.schema.FieldFormat;
import configgen.schema.FieldSchema;
import configgen.schema.FieldType;
import configgen.schema.Metadata;
import configgen.schema.StructSchema;
import configgen.value.CfgValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValueToSepStrTest {

    @Test
    void testToSepStrVList() {
        // Field: list<int> (sep=',')
        FieldSchema field = new FieldSchema("list",
                new FieldType.FList(FieldType.Primitive.INT),
                new FieldFormat.Sep(','),
                Metadata.of());

        Source source = Source.of();
        List<CfgValue.SimpleValue> values = List.of(
                new CfgValue.VInt(1, source),
                new CfgValue.VInt(2, source),
                new CfgValue.VInt(3, source));
        CfgValue.VList vList = new CfgValue.VList(values, source);

        String result = ValueToSepStr.toSepStr(vList, field);
        assertEquals("1,2,3", result);
    }

    @Test
    void testToSepStrVListInvalidFormat() {
        FieldSchema field = new FieldSchema("list",
                new FieldType.FList(FieldType.Primitive.INT),
                FieldFormat.AutoOrPack.AUTO,
                Metadata.of());

        Source source = Source.of();
        CfgValue.VList vList = new CfgValue.VList(List.of(), source);

        assertThrows(IllegalArgumentException.class, () -> ValueToSepStr.toSepStr(vList, field));
    }

    @Test
    void testToSepStrVListInvalidType() {
        FieldSchema field = new FieldSchema("int",
                FieldType.Primitive.INT,
                new FieldFormat.Sep(','),
                Metadata.of());

        Source source = Source.of();
        CfgValue.VList vList = new CfgValue.VList(List.of(), source);

        assertThrows(IllegalArgumentException.class, () -> ValueToSepStr.toSepStr(vList, field));
    }

    @Test
    void testToSepStrVStruct() {
        // Struct: struct A { a:int; b:str; } (sep=';')
        FieldSchema f1 = new FieldSchema("a", FieldType.Primitive.INT, FieldFormat.AutoOrPack.AUTO, Metadata.of());
        FieldSchema f2 = new FieldSchema("b", FieldType.Primitive.STRING, FieldFormat.AutoOrPack.AUTO, Metadata.of());

        StructSchema schema = new StructSchema("A",
                new FieldFormat.Sep(';'),
                Metadata.of(),
                List.of(f1, f2),
                List.of());

        Source source = Source.of();
        List<CfgValue.Value> values = List.of(
                new CfgValue.VInt(10, source),
                new CfgValue.VString("hello", source));

        CfgValue.VStruct vStruct = new CfgValue.VStruct(schema, values, source);

        String result = ValueToSepStr.toSepStr(vStruct);
        assertEquals("10;hello", result);
    }

    @Test
    void testToSepStrVStructInvalidFormat() {
        StructSchema schema = new StructSchema("A",
                FieldFormat.AutoOrPack.AUTO,
                Metadata.of(),
                List.of(),
                List.of());

        Source source = Source.of();
        CfgValue.VStruct vStruct = new CfgValue.VStruct(schema, List.of(), source);

        assertThrows(IllegalArgumentException.class, () -> ValueToSepStr.toSepStr(vStruct));
    }

    @Test
    void testToSepStrVListWithStruct() {
        // Struct: struct Point { x:int; y:int } (pack)
        FieldSchema fx = new FieldSchema("x", FieldType.Primitive.INT, FieldFormat.AutoOrPack.AUTO, Metadata.of());
        FieldSchema fy = new FieldSchema("y", FieldType.Primitive.INT, FieldFormat.AutoOrPack.AUTO, Metadata.of());
        StructSchema pointSchema = new StructSchema("Point", FieldFormat.AutoOrPack.PACK, Metadata.of(),
                List.of(fx, fy), List.of());

        // Field: list<Point> (sep=';')
        FieldSchema field = new FieldSchema("points",
                new FieldType.FList(new FieldType.StructRef("Point")),
                new FieldFormat.Sep(';'),
                Metadata.of());

        Source source = Source.of();

        // Point(1, 2) -> (1,2)
        CfgValue.VStruct p1 = new CfgValue.VStruct(pointSchema, List.of(
                new CfgValue.VInt(1, source),
                new CfgValue.VInt(2, source)), source);

        // Point(3, 4) -> (3,4)
        CfgValue.VStruct p2 = new CfgValue.VStruct(pointSchema, List.of(
                new CfgValue.VInt(3, source),
                new CfgValue.VInt(4, source)), source);

        CfgValue.VList vList = new CfgValue.VList(List.of(p1, p2), source);

        String result = ValueToSepStr.toSepStr(vList, field);
        // Expected: 1,2;3,4 (pack structs don't have parentheses)
        assertEquals("1,2;3,4", result);
    }
}

package configgen.genjava;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static configgen.genjava.SchemaPrimitive.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 嵌入 schema 的写出/读回 roundtrip：重点锁定 SText（TEXT 字段），
 * 曾因 SchemaDeserializer 缺 TEXT 分支、SchemaParser 把 TEXT 标成 SStr，
 * 导致按 schema 读数据的一方读错字符串池。
 */
class SchemaSerializerTest {

    @Test
    void primitivesRoundtrip() {
        SchemaBean bean = new SchemaBean(false);
        bean.addColumn("b", SBool);
        bean.addColumn("i", SInt);
        bean.addColumn("l", SLong);
        bean.addColumn("f", SFloat);
        bean.addColumn("s", SStr);
        bean.addColumn("t", SText);
        SchemaInterface root = new SchemaInterface();
        root.addImp("allPrimitives", bean);

        SchemaInterface parsed = serializeAndDeserialize(root);

        SchemaBean out = (SchemaBean) parsed.implementations.get("allPrimitives");
        assertEquals(SBool, out.columns.get(0).schema());
        assertEquals(SInt, out.columns.get(1).schema());
        assertEquals(SLong, out.columns.get(2).schema());
        assertEquals(SFloat, out.columns.get(3).schema());
        assertEquals(SStr, out.columns.get(4).schema());
        assertEquals(SText, out.columns.get(5).schema());
    }

    @Test
    void textInsideContainersRoundtrip() {
        SchemaBean bean = new SchemaBean(true);
        bean.addColumn("names", new SchemaList(SText));
        bean.addColumn("map", new SchemaMap(SStr, SText));
        SchemaInterface root = new SchemaInterface();
        root.addImp("t", bean);

        SchemaInterface parsed = serializeAndDeserialize(root);

        SchemaBean out = (SchemaBean) parsed.implementations.get("t");
        SchemaList list = assertInstanceOf(SchemaList.class, out.columns.get(0).schema());
        assertEquals(SText, list.ele());
        SchemaMap map = assertInstanceOf(SchemaMap.class, out.columns.get(1).schema());
        assertEquals(SStr, map.key());
        assertEquals(SText, map.value());
    }

    private static SchemaInterface serializeAndDeserialize(SchemaInterface root) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ConfigOutput out = new ConfigOutput(bos)) {
            new SchemaSerializer(out).serializeInterface(root);
        }
        ConfigInput input = new ConfigInput(bos.toByteArray());
        return (SchemaInterface) SchemaDeserializer.deserialize(input);
    }
}

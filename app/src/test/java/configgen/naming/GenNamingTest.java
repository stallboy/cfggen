package configgen.naming;

import configgen.schema.ForeignKeySchema;
import configgen.schema.KeySchema;
import configgen.schema.Metadata;
import configgen.schema.RefKey;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenNamingTest {

    private static ForeignKeySchema fk(String name, RefKey refKey) {
        KeySchema key = new KeySchema(List.of("id"));
        return new ForeignKeySchema(name, key, "refTable", refKey, new Metadata(new LinkedHashMap<>()));
    }

    @Test
    void refFieldName_普通引用() {
        assertEquals("RefUser", GenNaming.refFieldName(fk("user", new RefKey.RefPrimary(false))));
    }

    @Test
    void refFieldName_可空引用() {
        assertEquals("NullableRefUser", GenNaming.refFieldName(fk("user", new RefKey.RefPrimary(true))));
    }

    @Test
    void refFieldName_列表引用() {
        assertEquals("ListRefUsers", GenNaming.refFieldName(fk("users", new RefKey.RefList(new KeySchema(List.of("id"))))));
    }

    @Test
    void refFieldName_外键名首字母大写() {
        assertEquals("RefItem", GenNaming.refFieldName(fk("item", new RefKey.RefPrimary(false))));
        assertEquals("RefItem", GenNaming.refFieldName(fk("Item", new RefKey.RefPrimary(false))));
    }
}

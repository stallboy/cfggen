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

    private static final KeySchema COMPOSITE_KEY = new KeySchema(List.of("name", "id"));

    @Test
    void pascal族_词干与成品() {
        assertEquals("NameId", GenNaming.keyFieldsPascalName(COMPOSITE_KEY));
        assertEquals("NameIdMap", GenNaming.uniqueKeyMapName(COMPOSITE_KEY));
        assertEquals("NameIdKey", GenNaming.compositeKeyClassName(COMPOSITE_KEY));
    }

    @Test
    void pascal族_查找函数名_主键与唯一键() {
        assertEquals("Get", GenNaming.uniqueKeyGetByName(List.of()));
        assertEquals("Get", GenNaming.uniqueKeyGetByName(COMPOSITE_KEY, true));
        assertEquals("GetByNameId", GenNaming.uniqueKeyGetByName(COMPOSITE_KEY));
        assertEquals("GetByNameId", GenNaming.uniqueKeyGetByName(COMPOSITE_KEY, false));
    }

    @Test
    void snake族_gd风格() {
        assertEquals("find_by_name_id", GenNaming.uniqueKeyGetByNameSnake(COMPOSITE_KEY));
        assertEquals("find_by_name_id", GenNaming.uniqueKeyGetByNameSnake(List.of("name", "id")));
        assertEquals("_name_id_map", GenNaming.uniqueKeyMapNameSnake(COMPOSITE_KEY));
    }
}

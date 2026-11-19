package configgen.naming;

import configgen.schema.FieldFormat;
import configgen.schema.ForeignKeySchema;
import configgen.schema.KeySchema;
import configgen.schema.Metadata;
import configgen.schema.RefKey;
import configgen.schema.StructSchema;
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
    void refFieldNameForRequiredReference() {
        assertEquals("RefUser", GenNaming.refFieldName(fk("user", new RefKey.RefPrimary(false))));
    }

    @Test
    void refFieldNameForNullableReference() {
        assertEquals("NullableRefUser", GenNaming.refFieldName(fk("user", new RefKey.RefPrimary(true))));
    }

    @Test
    void refFieldNameForListReference() {
        assertEquals("ListRefUsers", GenNaming.refFieldName(fk("users", new RefKey.RefList(new KeySchema(List.of("id"))))));
    }

    @Test
    void refFieldNameNormalizesInitialLetter() {
        assertEquals("RefItem", GenNaming.refFieldName(fk("item", new RefKey.RefPrimary(false))));
        assertEquals("RefItem", GenNaming.refFieldName(fk("Item", new RefKey.RefPrimary(false))));
    }

    private static final KeySchema COMPOSITE_KEY = new KeySchema(List.of("name", "id"));

    @Test
    void pascalCaseCompositeKeyNames() {
        assertEquals("NameId", GenNaming.keyFieldsPascalName(COMPOSITE_KEY));
        assertEquals("NameIdMap", GenNaming.uniqueKeyMapName(COMPOSITE_KEY));
        assertEquals("NameIdKey", GenNaming.compositeKeyClassName(COMPOSITE_KEY));
    }

    @Test
    void pascalCaseLookupNamesForPrimaryAndUniqueKeys() {
        assertEquals("Get", GenNaming.uniqueKeyGetByName(List.of()));
        assertEquals("Get", GenNaming.uniqueKeyGetByName(COMPOSITE_KEY, true));
        assertEquals("GetByNameId", GenNaming.uniqueKeyGetByName(COMPOSITE_KEY));
        assertEquals("GetByNameId", GenNaming.uniqueKeyGetByName(COMPOSITE_KEY, false));
    }

    @Test
    void snakeCaseKeyNamesForGdScript() {
        assertEquals("find_by_name_id", GenNaming.uniqueKeyGetByNameSnake(COMPOSITE_KEY));
        assertEquals("find_by_name_id", GenNaming.uniqueKeyGetByNameSnake(List.of("name", "id")));
        assertEquals("_name_id_map", GenNaming.uniqueKeyMapNameSnake(COMPOSITE_KEY));
    }

    @Test
    void classNameSegmentsSplitOnDots() {
        StructSchema struct = new StructSchema("task.extra", FieldFormat.AutoOrPack.AUTO,
                new Metadata(new LinkedHashMap<>()), List.of(), List.of());
        assertEquals(List.of("task", "extra"), GenNaming.classNameSegments(struct));
    }
}

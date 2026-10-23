package configgen.schema;

import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CfgSchemaFilterByTagTest {

    @ParameterizedTest
    @ValueSource(strings = {"-editor", "c"})
    void preservesResolvedInterfaceForeignKeys(String tag) {
        CfgSchema original = CfgReader.parse("""
                table td.buff[id] (c) { id:int (c); }
                table td.damagetype[id] (c) { id:int (c); }
                table td.hero[id] (c) { id:int (c); }
                interface td.Action (c) {
                    struct ApplyBuff { buff:int ->buff (c); }
                    struct Damage { damageType:int ->damagetype (c); }
                    struct Summon { hero:int ->hero (c); }
                }
                """);
        original.resolve().checkErrors();
        CfgSchemaErrs diagnostics = CfgSchemaErrs.of();

        CfgSchema filtered = new CfgSchemaFilterByTag(original, tag, diagnostics).filter();
        filtered.resolve().checkErrors();

        InterfaceSchema originalAction = (InterfaceSchema) original.findFieldable("td.Action");
        InterfaceSchema filteredAction = (InterfaceSchema) filtered.findFieldable("td.Action");
        assertEquals(original.items().size(), filtered.items().size());
        assertEquals(3, filteredAction.impls().size());
        for (var entry : Map.of("ApplyBuff", "buff", "Damage", "damageType", "Summon", "hero").entrySet()) {
            StructSchema originalImpl = originalAction.findImpl(entry.getKey());
            StructSchema filteredImpl = filteredAction.findImpl(entry.getKey());
            assertNotNull(filteredImpl.findField(entry.getValue()));
            assertEquals(1, filteredImpl.foreignKeys().size());
            ForeignKeySchema originalRef = originalImpl.findForeignKey(entry.getValue());
            ForeignKeySchema filteredRef = filteredImpl.findForeignKey(entry.getValue());
            assertNotNull(filteredRef);
            assertEquals(originalRef.refTableNormalized(), filteredRef.refTableNormalized());
            assertSame(filtered.findTable(originalRef.refTableNormalized()), filteredRef.refTableSchema());
            assertEquals(1, originalImpl.foreignKeys().size());
        }
        assertTrue(diagnostics.weakWarns().isEmpty());
    }

    @Test
    void minusEditorRemovesTaggedFieldsAndTheirForeignKeys() {
        CfgSchema original = CfgReader.parse("""
                table td.buff[id] { id:int; }
                interface td.Action {
                    struct ApplyBuff {
                        buff:int ->buff;
                        previewBuff:int ->buff (editor);
                        note:str (editor);
                    }
                }
                """);
        original.resolve().checkErrors();
        CfgSchemaErrs diagnostics = CfgSchemaErrs.of();

        CfgSchema filtered = new CfgSchemaFilterByTag(original, "-editor", diagnostics).filter();
        filtered.resolve().checkErrors();

        StructSchema impl = ((InterfaceSchema) filtered.findFieldable("td.Action")).findImpl("ApplyBuff");
        assertNotNull(impl.findField("buff"));
        ForeignKeySchema buffRef = impl.findForeignKey("buff");
        assertNotNull(buffRef);
        assertEquals("td.buff", buffRef.refTableNormalized());
        assertNull(impl.findField("previewBuff"));
        assertNull(impl.findForeignKey("previewBuff"));
        assertNull(impl.findField("note"));
        assertTrue(diagnostics.weakWarns().isEmpty());
        StructSchema originalImpl = ((InterfaceSchema) original.findFieldable("td.Action")).findImpl("ApplyBuff");
        assertNotNull(originalImpl.findField("previewBuff"));
        assertNotNull(originalImpl.findForeignKey("previewBuff"));
    }

    @Test
    void minusEditorDoesNotRedirectRemovedTargetToSameNamedGlobalTable() {
        CfgSchema original = CfgReader.parse("""
                table buff[id] { id:int; }
                table td.buff[id] (editor) { id:int; }
                interface td.Action { struct ApplyBuff { buff:int ->buff; } }
                """);
        original.resolve().checkErrors();
        StructSchema originalImpl = ((InterfaceSchema) original.findFieldable("td.Action")).findImpl("ApplyBuff");
        assertEquals("td.buff", originalImpl.findForeignKey("buff").refTableNormalized());
        CfgSchemaErrs diagnostics = CfgSchemaErrs.of();

        CfgSchema filtered = new CfgSchemaFilterByTag(original, "-editor", diagnostics).filter();
        filtered.resolve().checkErrors();

        assertNull(filtered.findTable("td.buff"));
        assertNotNull(filtered.findTable("buff"));
        StructSchema impl = ((InterfaceSchema) filtered.findFieldable("td.Action")).findImpl("ApplyBuff");
        assertNotNull(impl.findField("buff"));
        assertNull(impl.findForeignKey("buff"));
        assertEquals(1, diagnostics.weakWarns().size());
        assertInstanceOf(CfgSchemaErrs.FilterRefIgnoredByRefTableNotFound.class, diagnostics.weakWarns().getFirst());
    }

    @Test
    void minusEditorRemovesReferencesToFilteredUniqueAndListKeys() {
        CfgSchema original = CfgReader.parse("""
                table td.buff[id] {
                    id:int;
                    code:str (editor);
                    category:int (editor);
                    [code];
                }
                interface td.Action {
                    struct ApplyBuff {
                        buff:int ->buff;
                        byCode:str ->buff[code];
                        byCategory:int =>buff[category];
                    }
                }
                """);
        original.resolve().checkErrors();
        CfgSchemaErrs diagnostics = CfgSchemaErrs.of();

        CfgSchema filtered = new CfgSchemaFilterByTag(original, "-editor", diagnostics).filter();
        filtered.resolve().checkErrors();

        TableSchema target = filtered.findTable("td.buff");
        assertNotNull(target.findField("id"));
        assertNull(target.findField("code"));
        assertNull(target.findField("category"));
        assertTrue(target.uniqueKeys().isEmpty());
        StructSchema impl = ((InterfaceSchema) filtered.findFieldable("td.Action")).findImpl("ApplyBuff");
        ForeignKeySchema buffRef = impl.findForeignKey("buff");
        assertNotNull(buffRef);
        assertEquals("td.buff", buffRef.refTableNormalized());
        assertNotNull(impl.findField("byCode"));
        assertNotNull(impl.findField("byCategory"));
        assertNull(impl.findForeignKey("byCode"));
        assertNull(impl.findForeignKey("byCategory"));
        assertEquals(2, diagnostics.weakWarns().size());
        assertTrue(diagnostics.weakWarns().stream()
                .allMatch(CfgSchemaErrs.FilterRefIgnoredByRefKeyNotFound.class::isInstance));
    }

    @Test
    public void emptyIfNoTag() {
        String str = """
                table tab1[id] {
                    id:int;
                    v:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, cfg.items().size());

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        cfg.resolve();

        assertEquals(0, errs.warns().size());
        assertEquals(0, errs.errs().size());

        assertEquals(0, cfg.items().size());
    }

    @Test
    public void filterByFieldTag() {
        String str = """
                table tab1[id] {
                    id:int (c);
                    v:int (c);
                    v2:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, cfg.items().size());
        TableSchema tab1 = cfg.findTable("tab1");
        assertEquals(3, tab1.fields().size());

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        CfgSchemaErrs errs2 = cfg.resolve();
        assertEquals(0, errs.errs().size());
        assertEquals(0, errs2.errs().size());

        tab1 = cfg.findTable("tab1");
        assertEquals(2, tab1.fields().size());
    }


    @Test
    public void filter_ifNotIncludePrimaryKey_resolveErr() {
        String str = """
                table tab1[id] {
                    id:int;
                    v:int (c);
                    v2:int (c);
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        assertEquals(0, errs.warns().size());
        assertEquals(0, errs.errs().size());
        errs = cfg.resolve();

        assertInstanceOf(CfgSchemaErrs.KeyNotFound.class, errs.errs().getFirst());
        TableSchema tab1 = cfg.findTable("tab1");
        assertEquals(2, tab1.fields().size());
        assertNotNull(tab1.findField("v"));
        assertNotNull(tab1.findField("v2"));
    }


    @Test
    public void filterAllFields_forTaggedTable() {
        String str = """
                table tab1[id] (c) {
                    id:int;
                    v:int;
                    v2:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        cfg.resolve();

        TableSchema tab1 = cfg.findTable("tab1");
        assertEquals(3, tab1.fields().size());
    }

    @Test
    public void filterTaggedFields_forTaggedTable() {
        String str = """
                table tab1[id] (c) {
                    id:int (c);
                    v:int (c);
                    v2:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        cfg.resolve();

        TableSchema tab1 = cfg.findTable("tab1");
        assertEquals(2, tab1.fields().size());
        assertNotNull(tab1.findField("id"));
        assertNotNull(tab1.findField("v"));
    }


    @Test
    public void filterNoMinusTaggedFields_forTaggedTable() {
        String str = """
                table tab1[id] (c) {
                    id:int ;
                    v:int ;
                    v2:int (-c);
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        cfg.resolve();

        TableSchema tab1 = cfg.findTable("tab1");
        assertEquals(2, tab1.fields().size());
        assertNotNull(tab1.findField("id"));
        assertNotNull(tab1.findField("v"));
    }


    @Test
    public void filterOnlyTaggedFields_forTaggedTable() {
        String str = """
                table tab1[id] (c) {
                    id:int (c);
                    v:int ;
                    v2:int (-c);
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        cfg.resolve();

        TableSchema tab1 = cfg.findTable("tab1");
        assertEquals(1, tab1.fields().size());
        assertNotNull(tab1.findField("id"));
    }

    @Test
    public void filterAllImpls_forTaggedInterface() {
        String str = """
                interface action (c) {
                    struct impl1 {
                        v:int;
                    }
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        CfgSchemaErrs errs2 = cfg.resolve();
        assertEquals(0, errs2.errs().size());

        InterfaceSchema action = (InterfaceSchema) cfg.findFieldable("action");
        assertEquals(1, action.impls().size());
        StructSchema first = action.impls().getFirst();
        assertEquals(1, first.fields().size());
    }



    @Test
    public void tagImplToFilterOnlyImplName_forTaggedInterface() {
        String str = """
                interface action (c) {
                    struct impl1 (c) {
                        v:int;
                    }
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        CfgSchemaErrs errs2 = cfg.resolve();
        assertEquals(0, errs2.errs().size());

        InterfaceSchema action = (InterfaceSchema) cfg.findFieldable("action");
        assertEquals(1, action.impls().size());
        StructSchema first = action.impls().getFirst();
        assertEquals(0, first.fields().size());
    }


    @Test
    public void tagImplNotAutomaticallyTagInterface() {
        String str = """
                interface action {
                    struct impl1 (c) {
                        v:int;
                    }
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        cfg = clientCfgSchema.filter();
        CfgSchemaErrs errs2 = cfg.resolve();

        assertEquals(0, errs2.errs().size());
        assertEquals(0, cfg.items().size());
    }


    @Test
    public void warn_FilterRefIgnoredByRefTableNotFound() {
        String str = """
                table tab1[id] (c) {
                    id:int;
                    v:int ->tab2;
                }
                table tab2[id] {
                    id:int;
                    v2:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(0, errs.warns().size());
        assertEquals(0, errs.errs().size());

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        clientCfgSchema.filter();

        assertEquals(1, errs.weakWarns().size());
        assertEquals(0, errs.errs().size());
        CfgSchemaErrs.WeakWarn warn = errs.weakWarns().getFirst();
        assertInstanceOf(CfgSchemaErrs.FilterRefIgnoredByRefTableNotFound.class, warn);
        CfgSchemaErrs.FilterRefIgnoredByRefTableNotFound w = (CfgSchemaErrs.FilterRefIgnoredByRefTableNotFound) warn;
        assertEquals("tab1", w.name());
        assertEquals("v", w.foreignKey());
        assertEquals("tab2", w.notFoundRefTable());
    }

    @Test
    public void warn_FilterRefIgnoredByRefKeyNotFound() {
        String str = """
                table tab1[id] (c) {
                    id:int;
                    v:int ->tab2[uk];
                }
                table tab2[id](c) {
                    [uk];
                    id:int;
                    v2:int;
                    uk:int (-c);
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(0, errs.warns().size());
        assertEquals(0, errs.errs().size());

        CfgSchemaFilterByTag clientCfgSchema = new CfgSchemaFilterByTag(cfg, "c", errs);
        clientCfgSchema.filter();

        assertEquals(1, errs.weakWarns().size());
        assertEquals(0, errs.errs().size());
        CfgSchemaErrs.WeakWarn warn = errs.weakWarns().getFirst();
        assertInstanceOf(CfgSchemaErrs.FilterRefIgnoredByRefKeyNotFound.class, warn);
        CfgSchemaErrs.FilterRefIgnoredByRefKeyNotFound w = (CfgSchemaErrs.FilterRefIgnoredByRefKeyNotFound) warn;
        assertEquals("tab1", w.name());
        assertEquals("v", w.foreignKey());
        assertEquals("tab2", w.refTable());
        assertEquals("uk", w.notFoundRefKey().getFirst());
    }


}

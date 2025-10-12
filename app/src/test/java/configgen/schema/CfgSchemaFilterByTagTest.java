package configgen.schema;

import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CfgSchemaFilterByTagTest {

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
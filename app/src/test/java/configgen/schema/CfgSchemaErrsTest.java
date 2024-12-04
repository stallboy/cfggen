package configgen.schema;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CfgSchemaErrsTest {

    @Test
    public void test_create_schema_errs_with_empty_lists() {
        CfgSchemaErrs cfgSchemaErrs = CfgSchemaErrs.of();
        assertNotNull(cfgSchemaErrs);
        assertTrue(cfgSchemaErrs.errs().isEmpty());
        assertTrue(cfgSchemaErrs.warns().isEmpty());
    }

    @Test
    public void test_create_schema_errs_with_null_lists() {
        assertThrows(NullPointerException.class, () -> new CfgSchemaErrs(null, new ArrayList<>()));
        assertThrows(NullPointerException.class, () -> new CfgSchemaErrs(new ArrayList<>(), null));
    }

    // Adding a null error or warning should not be allowed
    @Test
    public void test_add_null_error_or_warning() {
        CfgSchemaErrs cfgSchemaErrs = CfgSchemaErrs.of();
        assertThrows(NullPointerException.class, () -> cfgSchemaErrs.addErr(null));
        assertThrows(NullPointerException.class, () -> cfgSchemaErrs.addWarn(null));
    }

    // Adding an error to SchemaErrs should store the error correctly
    @Test
    public void test_add_error_to_schemaerrs_should_store_error_correctly() {
        CfgSchemaErrs cfgSchemaErrs = CfgSchemaErrs.of();
        CfgSchemaErrs.Err err = new CfgSchemaErrs.TableNameNotLowerCase("TableName");
        cfgSchemaErrs.addErr(err);
        assertEquals(1, cfgSchemaErrs.errs().size());
        assertTrue(cfgSchemaErrs.errs().contains(err));
    }

    // Adding a warning to SchemaErrs should store the warning correctly
    @Test
    public void test_add_warning_to_schemaerrs_correctly() {
        CfgSchemaErrs cfgSchemaErrs = CfgSchemaErrs.of();
        cfgSchemaErrs.addWarn(new CfgSchemaErrs.NameMayConflictByRef("interface1.name1", "name1"));
        assertEquals(1, cfgSchemaErrs.warns().size());
        assertInstanceOf(CfgSchemaErrs.NameMayConflictByRef.class, cfgSchemaErrs.warns().getFirst());
        assertEquals("interface1.name1", ((CfgSchemaErrs.NameMayConflictByRef) cfgSchemaErrs.warns().getFirst()).name1());
        assertEquals("name1", ((CfgSchemaErrs.NameMayConflictByRef) cfgSchemaErrs.warns().getFirst()).name2());
    }

    // assureNoError should throw SchemaError if errors are present
    @Test
    public void test_assure_no_error_throws_schema_error_if_errors_present() {
        CfgSchemaErrs cfgSchemaErrs = CfgSchemaErrs.of();
        cfgSchemaErrs.addErr(new CfgSchemaErrs.TableNameNotLowerCase("TableName"));
        cfgSchemaErrs.addWarn(new CfgSchemaErrs.StructNotUsed("StructName"));

        CfgSchemaException cfgSchemaException = assertThrows(CfgSchemaException.class, cfgSchemaErrs::checkErrors);
        assertEquals(cfgSchemaErrs, cfgSchemaException.getErrs());
    }
}
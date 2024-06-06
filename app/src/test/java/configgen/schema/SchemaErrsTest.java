package configgen.schema;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class SchemaErrsTest {

    @Test
    public void test_create_schema_errs_with_empty_lists() {
        SchemaErrs schemaErrs = SchemaErrs.of();
        assertNotNull(schemaErrs);
        assertTrue(schemaErrs.errs().isEmpty());
        assertTrue(schemaErrs.warns().isEmpty());
    }

    @Test
    public void test_create_schema_errs_with_null_lists() {
        assertThrows(NullPointerException.class, () -> new SchemaErrs(null, new ArrayList<>()));
        assertThrows(NullPointerException.class, () -> new SchemaErrs(new ArrayList<>(), null));
    }

    // Adding a null error or warning should not be allowed
    @Test
    public void test_add_null_error_or_warning() {
        SchemaErrs schemaErrs = SchemaErrs.of();
        assertThrows(NullPointerException.class, () -> schemaErrs.addErr(null));
        assertThrows(NullPointerException.class, () -> schemaErrs.addWarn(null));
    }

    // Adding an error to SchemaErrs should store the error correctly
    @Test
    public void test_add_error_to_schemaerrs_should_store_error_correctly() {
        SchemaErrs schemaErrs = SchemaErrs.of();
        SchemaErrs.Err err = new SchemaErrs.TableNameNotLowerCase("TableName");
        schemaErrs.addErr(err);
        assertEquals(1, schemaErrs.errs().size());
        assertTrue(schemaErrs.errs().contains(err));
    }

    // Adding a warning to SchemaErrs should store the warning correctly
    @Test
    public void test_add_warning_to_schemaerrs_correctly() {
        SchemaErrs schemaErrs = SchemaErrs.of();
        schemaErrs.addWarn(new SchemaErrs.NameMayConflictByRef("interface1.name1", "name1"));
        assertEquals(1, schemaErrs.warns().size());
        assertInstanceOf(SchemaErrs.NameMayConflictByRef.class, schemaErrs.warns().getFirst());
        assertEquals("interface1.name1", ((SchemaErrs.NameMayConflictByRef) schemaErrs.warns().getFirst()).name1());
        assertEquals("name1", ((SchemaErrs.NameMayConflictByRef) schemaErrs.warns().getFirst()).name2());
    }

    // assureNoError should throw SchemaError if errors are present
    @Test
    public void test_assure_no_error_throws_schema_error_if_errors_present() {
        SchemaErrs schemaErrs = SchemaErrs.of();
        schemaErrs.addErr(new SchemaErrs.TableNameNotLowerCase("TableName"));
        schemaErrs.addWarn(new SchemaErrs.StructNotUsed("StructName"));

        SchemaError schemaError = assertThrows(SchemaError.class, schemaErrs::checkErrors);
        assertEquals(schemaErrs, schemaError.getErrs());
    }
}
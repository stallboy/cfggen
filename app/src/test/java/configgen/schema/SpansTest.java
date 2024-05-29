package configgen.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static org.junit.jupiter.api.Assertions.*;

class SpansTest {
    @Test
    public void test_preCalculateAllNeededSpans_validCfgSchema() {
        CfgSchema cfgSchema = CfgSchema.of();
        Metadata meta = Metadata.of();
        List<FieldSchema> fields = List.of(new FieldSchema("field1", FieldType.Primitive.INT, AUTO, meta));
        TableSchema table = new TableSchema("table1",
                new KeySchema(List.of("field1")), EntryType.ENo.NO, false, meta, fields, List.of(), List.of());
        cfgSchema.add(table);
        cfgSchema.resolve();

        SchemaErrs errs = SchemaErrs.of();
        Spans.preCalculateAllNeededSpans(cfgSchema, errs);

        assertTrue(errs.errs().isEmpty());
        assertEquals(1, Spans.span(table));
    }
}
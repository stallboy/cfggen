package configgen.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static org.junit.jupiter.api.Assertions.*;

class NameableTest {

    @Test
    void name() {
        InterfaceSchema action = new InterfaceSchema("ns1.ns2.action", "", "", AUTO,
                Metadata.of(), List.of());
        assertEquals("ns1.ns2", action.namespace());
        assertEquals("action", action.lastName());
        assertEquals("ns1.ns2.action", action.fullName());
    }

    @Test
    void comment() {
        String comment = "comment123";
        Metadata meta = Metadata.of();
        meta.putComment(comment);

        InterfaceSchema action = new InterfaceSchema("ns1.ns2.action", "", "", AUTO,
                meta, List.of());
        assertEquals(comment, action.comment());
    }


}
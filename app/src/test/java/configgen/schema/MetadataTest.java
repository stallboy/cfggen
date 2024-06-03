package configgen.schema;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.SequencedMap;


public class MetadataTest {
    @Test
    public void creates_empty_metadata() {
        Metadata metadata = Metadata.of();
        assertTrue(metadata.data().isEmpty());
    }

    @Test
    public void putTag_throws_exception_for_reserved_tags() {
        Metadata metadata = Metadata.of();
        String reservedTag = "json";
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            metadata.putTag(reservedTag);
        });
        assertEquals("'json' reserved", exception.getMessage());
    }

    @Test
    public void putTag_throws_exception_for_duplicate_tags() {
        Metadata metadata = Metadata.of();
        String tag = "customTag";
        metadata.putTag(tag);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            metadata.putTag(tag);
        });
        assertEquals("'customTag' duplicated", exception.getMessage());
    }

    @Test
    public void put_get_retrieves_correct_value() {
        Metadata.MetaStr metaStr = new Metadata.MetaStr("value");
        Metadata metadata = Metadata.of();
        metadata.data().put("key", metaStr);

        assertEquals(metaStr, metadata.get("key"));
    }

    @Test
    public void copy_creates_deep_copy() {
        Metadata.MetaInt metaInt = new Metadata.MetaInt(10);
        Metadata.MetaStr metaStr = new Metadata.MetaStr("test");
        SequencedMap<String, Metadata.MetaValue> data = new LinkedHashMap<>();
        data.put("int", metaInt);
        data.put("str", metaStr);
        Metadata originalMetadata = new Metadata(data);

        Metadata copiedMetadata = originalMetadata.copy();

        assertNotSame(originalMetadata, copiedMetadata);
        assertEquals(originalMetadata.data(), copiedMetadata.data());
        assertNotSame(originalMetadata.data(), copiedMetadata.data());
    }


    @Test
    public void tag() {
        Metadata metadata = Metadata.of();
        metadata.putTag("test_tag");

        assertTrue(metadata.hasTag("test_tag"));
        assertFalse(metadata.hasTag("non_existent_tag"));
    }

    @Test
    public void hasRef() {
        Metadata metadata = Metadata.of();
        metadata.putHasRef(true);
        assertTrue(metadata.getHasRef() instanceof Metadata.MetaInt v && v.value() == 1);
    }

    @Test
    public void hasBlock() {
        Metadata metadata = Metadata.of();
        metadata.putHasBlock(true);
        assertTrue(metadata.getHasBlock() instanceof Metadata.MetaInt v && v.value() == 1);
    }

    @Test
    public void span() {
        Metadata metadata = Metadata.of();
        int spanValue = 10;
        metadata.putSpan(spanValue);
        assertTrue(metadata.getSpan() instanceof Metadata.MetaInt v && v.value() == spanValue);
    }


    @Test
    public void comment() {
        Metadata metadata = Metadata.of();
        String comment = "This is a test comment";
        metadata.putComment(comment);
        assertEquals(comment, metadata.getComment());
    }

    @Test
    public void remove_comment() {
        Metadata metadata = Metadata.of();
        metadata.putComment("Test Comment");

        String removedComment = metadata.removeComment();

        assertEquals("Test Comment", removedComment);
        assertEquals("", metadata.getComment());
    }

    @Test
    public void nullable() {
        Metadata metadata = Metadata.of();
        assertFalse(metadata.hasTag("nullable"));

        metadata.putNullable();
        assertTrue(metadata.hasTag("nullable"));
    }

    @Test
    public void remove_nullable_tag() {
        Metadata metadata = Metadata.of();
        metadata.putNullable();

        assertTrue(metadata.removeNullable());
        assertFalse(metadata.hasTag("nullable"));
    }

    @Test
    public void enumRef() {
        Metadata metadata = Metadata.of();
        metadata.putEnumRef("enum");

        assertEquals("enum", metadata.removeEnumRef());
        assertFalse(metadata.hasTag("enumRef"));
    }


    @Test
    public void defaultImpl() {
        Metadata metadata = Metadata.of();
        metadata.putDefaultImpl("defImpl");

        assertEquals("defImpl", metadata.removeDefaultImpl());
        assertFalse(metadata.hasTag("defaultImpl"));
    }

    @Test
    public void entry_enum() {
        Metadata metadata = Metadata.of();
        metadata.putEntry(new EntryType.EEnum("enumField"));

        assertTrue(metadata.removeEntry() instanceof EntryType.EEnum ee && ee.field().equals("enumField"));
        assertFalse(metadata.hasTag("enum"));
    }

    @Test
    public void entry_entry() {
        Metadata metadata = Metadata.of();
        metadata.putEntry(new EntryType.EEntry("entryField"));

        assertTrue(metadata.removeEntry() instanceof EntryType.EEntry ee && ee.field().equals("entryField"));
        assertFalse(metadata.hasTag("entry"));
    }

    @Test
    public void columnMode() {
        Metadata metadata = Metadata.of();
        metadata.putColumnMode();

        assertTrue(metadata.removeColumnMode());
    }


    @Test
    public void fmt_auto() {
        Metadata metadata = Metadata.of();
        metadata.putFmt(AUTO);

        assertEquals(AUTO, metadata.removeFmt());
    }

    @Test
    public void fmt_pack() {
        Metadata metadata = Metadata.of();
        metadata.putFmt(PACK);

        assertEquals(PACK, metadata.removeFmt());
    }

    @Test
    public void fmt_sep() {
        Metadata metadata = Metadata.of();
        metadata.putFmt(new FieldFormat.Sep(','));

        assertTrue(metadata.removeFmt() instanceof FieldFormat.Sep sep && sep.sep() == ',');
    }

    @Test
    public void fmt_fix() {
        Metadata metadata = Metadata.of();
        metadata.putFmt(new FieldFormat.Fix(3));

        assertTrue(metadata.removeFmt() instanceof FieldFormat.Fix fix && fix.count() == 3);
    }

    @Test
    public void fmt_block() {
        Metadata metadata = Metadata.of();
        metadata.putFmt(new FieldFormat.Block(3));

        assertTrue(metadata.removeFmt() instanceof FieldFormat.Block block && block.fix() == 3);
    }


}
package configgen.schema;

import org.jspecify.annotations.Nullable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatTest {
    static class StatImpl implements Stat {
        int field1;
        int field2;
    }

    static class StatImpl2 implements Stat {
        int field1;
        @Nullable String field2;
    }


    @Test
    public void merge_correctly_sums_integer_fields() {
        StatImpl stat1 = new StatImpl();
        stat1.field1 = 10;
        stat1.field2 = 20;

        StatImpl stat2 = new StatImpl();
        stat2.field1 = 5;
        stat2.field2 = 15;

        stat1.merge(stat2);

        assertEquals(15, stat1.field1);
        assertEquals(35, stat1.field2);
    }

    @Test
    public void merge_rejects_an_unrelated_stat_type() {
        StatImpl stat1 = new StatImpl();
        stat1.field1 = 10;
        StatImpl2 stat2 = new StatImpl2();
        stat2.field2 = "aa";

        assertThrows(IllegalArgumentException.class, () -> stat1.merge(stat2));
    }


    @Test
    void merge_sums_integer_fields_and_preserves_non_integer_fields() {
        StatImpl2 target = new StatImpl2();
        target.field1 = 10;
        target.field2 = "target";
        StatImpl2 source = new StatImpl2();
        source.field1 = 5;
        source.field2 = "source";

        target.merge(source);

        assertEquals(15, target.field1);
        assertEquals("target", target.field2);
        assertEquals(5, source.field1);
        assertEquals("source", source.field2);
    }

}

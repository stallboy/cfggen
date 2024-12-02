package configgen.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatTest {
    static class StatImpl implements Stat {
        int field1;
        int field2;
    }

    static class StatImpl2 implements Stat {
        int field1;
        String field2;
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
    public void merge_exception_if_has_not_integer_field() {
        StatImpl stat1 = new StatImpl();
        stat1.field1 = 10;
        StatImpl2 stat2 = new StatImpl2();
        stat2.field2 = "aa";

        assertThrows(RuntimeException.class , () -> stat1.merge(stat2));
    }


}
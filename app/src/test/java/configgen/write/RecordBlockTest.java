package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordBlockTest {

    @Test
    void testSetCellAndGetRowCount() {
        RecordBlock block = new RecordBlock(3);
        assertEquals(0, block.getRowCount());

        block.setCell(0, 0, "0-0");
        assertEquals(1, block.getRowCount());

        block.setCell(2, 1, "2-1");
        assertEquals(3, block.getRowCount());
    }

    @Test
    void testExpandIfNeeded() {
        RecordBlock block = new RecordBlock(2);
        // Initial capacity is 4
        block.setCell(5, 0, "5-0");
        assertEquals(6, block.getRowCount());

        // Should expand
        block.setCell(10, 0, "10-0");
        assertEquals(11, block.getRowCount());
    }

    @Test
    void testSetCellInvalidArgs() {
        RecordBlock block = new RecordBlock(2);
        assertThrows(IllegalArgumentException.class, () -> block.setCell(-1, 0, "val"));
        assertThrows(IllegalArgumentException.class, () -> block.setCell(0, -1, "val"));
        assertThrows(IllegalArgumentException.class, () -> block.setCell(0, 2, "val"));
        assertThrows(NullPointerException.class, () -> block.setCell(0, 0, null));
    }

    @Test
    void testTransformed() {
        RecordBlock block = new RecordBlock(3);
        block.setCell(0, 0, "A");
        block.setCell(0, 1, "B");
        block.setCell(0, 2, "C");

        // Map 0->0, 1->2, 2->3
        List<Integer> indices = List.of(0, 2, 3);
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, indices);

        assertEquals(1, transformed.getRowCount());
        String[] row = transformed.getRow(0);

        // Expected: size 4.
        // 0 -> A
        // 1 -> null
        // 2 -> B
        // 3 -> C
        assertEquals(4, row.length);
        assertEquals("A", row[0]);
        assertNull(row[1]);
        assertEquals("B", row[2]);
        assertEquals("C", row[3]);
    }

    @Test
    void testTransformedInvalidArgs() {
        RecordBlock block = new RecordBlock(2);
        List<Integer> indices = List.of(1);
        assertThrows(IllegalArgumentException.class, () -> new RecordBlockTransformed(block, indices));
    }
}

package configgen.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UnicodeReaderTest {

    @Test
    public void test_empty_input_stream() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
        UnicodeReader reader = new UnicodeReader(input, StandardCharsets.UTF_8);
        assertEquals("UTF8", reader.getEncoding());
        assertEquals(-1, reader.read());
        reader.close();
    }


    @Test
    public void test_multi_element_list() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("test".getBytes());
        UnicodeReader reader = new UnicodeReader(input, StandardCharsets.UTF_8);
        char[] buffer = new char[10];
        int read = reader.read(buffer, 0, 10);
        assertEquals(4, read);
        assertEquals('t', buffer[0]);
        assertEquals('e', buffer[1]);
        assertEquals('s', buffer[2]);
        assertEquals('t', buffer[3]);
        reader.close();
    }

    @Test
    void detectsBomWhenInputReturnsOnlyOneBytePerRead() throws IOException {
        byte[] bytes = "\uFEFF字".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(bytes) {
            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                return super.read(buffer, offset, Math.min(length, 1));
            }
        };
        try (UnicodeReader reader = new UnicodeReader(input, StandardCharsets.UTF_16BE)) {
            assertEquals('字', reader.read());
            assertEquals(-1, reader.read());
        }
    }
}
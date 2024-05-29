package configgen.util;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class XorCipherOutputStreamTest {

    @Test
    public void constructor_NullOrEmptyCipher_ThrowsException() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class, () -> new XorCipherOutputStream(out, null));
        assertThrows(IllegalArgumentException.class, () -> new XorCipherOutputStream(out, ""));
    }

    @Test
    public void write_SingleByte_EncryptsCorrectly() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String cipher = "key";
        XorCipherOutputStream xorStream = new XorCipherOutputStream(out, cipher);
        byte inputByte = 0x01;
        byte expectedByte = (byte) (inputByte ^ cipher.getBytes()[0]);

        xorStream.write(inputByte);

        assertEquals(expectedByte, out.toByteArray()[0]);
    }

    @Test
    public void write_ByteArray_EncryptsCorrectly() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String cipher = "key";
        XorCipherOutputStream xorStream = new XorCipherOutputStream(out, cipher);
        byte[] inputBytes = {0x01, 0x02, 0x03};
        byte[] expectedBytes = {
                (byte) (inputBytes[0] ^ cipher.getBytes()[0]),
                (byte) (inputBytes[1] ^ cipher.getBytes()[1]),
                (byte) (inputBytes[2] ^ cipher.getBytes()[2])
        };

        xorStream.write(inputBytes, 0, inputBytes.length);

        byte[] resultBytes = out.toByteArray();
        for (int i = 0; i < expectedBytes.length; i++) {
            assertEquals(expectedBytes[i], resultBytes[i]);
        }
    }
}
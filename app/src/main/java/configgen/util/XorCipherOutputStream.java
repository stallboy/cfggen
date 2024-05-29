package configgen.util;


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class XorCipherOutputStream extends FilterOutputStream {
    private final byte[] cipherBytes;
    private int index;

    public XorCipherOutputStream(OutputStream out, String cipher) {
        super(out);
        if (cipher == null || cipher.isEmpty()) {
            throw new IllegalArgumentException("Cipher cannot be null or empty");
        }
        this.cipherBytes = cipher.getBytes(StandardCharsets.UTF_8);
        this.index = 0;
    }

    @Override
    public void write(int b) throws IOException {
        // Perform XOR operation with the current cipher byte
        int encryptedByte = b ^ cipherBytes[index % cipherBytes.length];
        super.write(encryptedByte);
        index++;
        if (index == cipherBytes.length) {
            index = 0;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) {
            int encryptedByte = b[i] ^ cipherBytes[index % cipherBytes.length];
            super.write(encryptedByte);
            index++;
            if (index == cipherBytes.length) {
                index = 0;
            }
        }
    }
}

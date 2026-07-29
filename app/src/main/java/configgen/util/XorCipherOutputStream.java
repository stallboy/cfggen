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
        super.write(xorNext(b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] encrypted = new byte[len];
        for (int i = 0; i < len; i++) {
            encrypted[i] = (byte) xorNext(b[off + i]);
        }
        // 注意不能调 super.write(byte[],off,len)：FilterOutputStream 会逐字节回调 write(int) 导致二次异或
        out.write(encrypted, 0, len);
    }

    /**
     * 用当前密钥字节异或，并推进密钥循环
     */
    private int xorNext(int b) {
        int encryptedByte = b ^ cipherBytes[index % cipherBytes.length];
        index++;
        if (index == cipherBytes.length) {
            index = 0;
        }
        return encryptedByte;
    }
}

package configgen.util;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Generic Unicode text reader, which uses a BOM (Byte Order Mark) to identify
 * the encoding to be used. This also has the side effect of removing the BOM
 * from the input stream (when present).
 *
 * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058">
 * JDK Bug 4508058</a>
 */
public class UnicodeReader extends Reader {

    private final InputStreamReader internalInputStreamReader;
    private final Charset defaultEnc;

    private static final int BOM_SIZE = 4;

    /**
     * @param in         input stream
     * @param defaultEnc default encoding (used only if BOM is not found) or
     *                   <code>null</code> to use system default
     * @throws IOException if an I/O error occurs
     */
    public UnicodeReader(InputStream in, Charset defaultEnc) throws IOException {
        this.defaultEnc = defaultEnc;

        // Read ahead four bytes and check for BOM marks. Extra bytes are unread
        // back to the stream; only BOM bytes are skipped.
        byte[] bom = new byte[BOM_SIZE];
        PushbackInputStream pushbackStream = new PushbackInputStream(in, BOM_SIZE);
        int n = pushbackStream.readNBytes(bom, 0, bom.length);
        int size = bomSize(bom, n);
        Charset encoding = charsetForBom(bom, size);
        int unread = n - size;
        if (unread > 0) {
            pushbackStream.unread(bom, size, unread);
        }

        // Use given encoding
        internalInputStreamReader = new InputStreamReader(pushbackStream, encoding != null ? encoding : defaultEnc);
    }

    public static @Nullable Charset bomCharset(byte[] bytes) {
        return charsetForBom(bytes, bomSize(bytes, bytes.length));
    }

    private static @Nullable Charset charsetForBom(byte[] bytes, int size) {
        return switch (size) {
            case 4 -> Charset.forName(bytes[0] == 0 ? "UTF-32BE" : "UTF-32LE");
            case 3 -> StandardCharsets.UTF_8;
            case 2 -> bytes[0] == (byte) 0xFE ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
            default -> null;
        };
    }

    private static int bomSize(byte[] bytes, int length) {
        if (length >= 4 && ((bytes[0] == 0 && bytes[1] == 0 && bytes[2] == (byte) 0xFE && bytes[3] == (byte) 0xFF)
                || (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE && bytes[2] == 0 && bytes[3] == 0))) {
            return 4;
        }
        if (length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return 3;
        }
        if (length >= 2 && ((bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF)
                || (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE))) {
            return 2;
        }
        return 0;
    }

    public Charset getDefaultEncoding() {
        return defaultEnc;
    }

    public String getEncoding() {
        return internalInputStreamReader.getEncoding();
    }

    @Override
    public void close() throws IOException {
        internalInputStreamReader.close();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return internalInputStreamReader.read(cbuf, off, len);
    }
}

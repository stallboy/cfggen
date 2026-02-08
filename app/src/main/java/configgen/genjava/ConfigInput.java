package configgen.genjava;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// 小端字节序（LE）
public class ConfigInput implements Closeable {
    private final InputStream input;

    public ConfigInput(InputStream input) {
        this.input = input;
    }

    public boolean readBool() {
        try {
            return input.read() != 0;
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    public int readInt() {
        try {
            int b0 = input.read();
            int b1 = input.read();
            int b2 = input.read();
            int b3 = input.read();
            if ((b0 | b1 | b2 | b3) < 0) {
                throw new ConfigErr(new IOException("EOF reading int"));
            }
            return (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    public long readLong() {
        try {
            long b0 = input.read();
            long b1 = input.read();
            long b2 = input.read();
            long b3 = input.read();
            long b4 = input.read();
            long b5 = input.read();
            long b6 = input.read();
            long b7 = input.read();
            if ((b0 | b1 | b2 | b3 | b4 | b5 | b6 | b7) < 0) {
                throw new ConfigErr(new IOException("EOF reading long"));
            }
            return (b0 & 0xFFL) | ((b1 & 0xFFL) << 8) | ((b2 & 0xFFL) << 16) | ((b3 & 0xFFL) << 24)
                    | ((b4 & 0xFFL) << 32) | ((b5 & 0xFFL) << 40) | ((b6 & 0xFFL) << 48) | ((b7 & 0xFFL) << 56);
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public String readStr() {
        int len = readInt();
        byte[] bytes = readRawBytes(len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readRawBytes(int len) {
        try {
            byte[] data = new byte[len];
            int total = 0;
            while (total < len) {
                int n = input.read(data, total, len - total);
                if (n <= 0) {
                    throw new ConfigErr(new IOException("EOF reading " + len + " bytes"));
                }
                total += n;
            }
            return data;
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    public int skipBytes(int n) {
        try {
            return (int) input.skip(n);
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}

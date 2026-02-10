package configgen.genjava;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
public class ConfigInput implements Closeable {
    private final InputStream input;

    // 专门用于 primitives (int, long, float) 的小缓冲区，避免频繁 new 数组
    private final byte[] localBuffer = new byte[8];
    private final ByteBuffer converter = ByteBuffer.wrap(localBuffer).order(ByteOrder.LITTLE_ENDIAN);

    private String[] stringPool;
    private LangTextPool[] langTextPools;

    public ConfigInput(InputStream input) {
        // 核心优化：确保外层有缓冲，减少系统调用次数
        this.input = (input instanceof BufferedInputStream) ? input : new BufferedInputStream(input);
    }

    /**
     * 内部辅助：确保读满指定的字节到 localBuffer
     */
    private void fillLocalBuffer(int len) {
        readFully(localBuffer, 0, len);
        converter.rewind();
    }

    /**
     * 核心逻辑：确保从流中读取出“确切长度”的字节
     */
    @SuppressWarnings("SameParameterValue")
    private void readFully(byte[] b, int off, int len) {
        int n = 0;
        try {
            while (n < len) {
                int count = input.read(b, off + n, len - n);
                if (count < 0) {
                    throw new ConfigErr("EOF: Need " + len + " bytes, but only got " + n);
                }
                n += count;
            }
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    public int readInt() {
        fillLocalBuffer(4);
        return converter.getInt();
    }

    public long readLong() {
        fillLocalBuffer(8);
        return converter.getLong();
    }

    public float readFloat() {
        fillLocalBuffer(4);
        return converter.getFloat();
    }

    public boolean readBool() {
        try {
            int b = input.read();
            if (b < 0) throw new ConfigErr("EOF reading bool");
            return b != 0;
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    /**
     * 读取指定长度的原始字节。
     * 优化点：使用了循环读取确保数据完整性。
     */
    public byte[] readRawBytes(int len) {
        if (len < 0) throw new ConfigErr("Invalid length: " + len);
        if (len == 0) return new byte[0];

        byte[] bytes = new byte[len];
        readFully(bytes, 0, len);
        return bytes;
    }


    /**
     * 跳过指定字节。
     * 优化点：处理了 skip() 可能返回 0 的情况。
     */
    public void skipBytes(int n) {
        if (n <= 0) return;
        try {
            long totalSkipped = 0;
            while (totalSkipped < n) {
                long skipped = input.skip(n - totalSkipped);
                if (skipped <= 0) {
                    // 如果 skip 返回 0，尝试读取一个字节来确认是否到文件尾
                    if (input.read() == -1) {
                        throw new ConfigErr("EOF skip " + n + " bytes");
                    }
                    totalSkipped++;
                } else {
                    totalSkipped += skipped;
                }
            }
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    // --- 字符串与池逻辑 ---

    public String readString() {
        int len = readInt();
        if (len == 0) return "";
        // 这种方式虽然会产生临时 byte[]，但对于 readString 来说是最稳妥的
        byte[] bytes = readRawBytes(len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // ------Pool------

    /**
     * 读取全局 StringPool（在读取表数据之前调用）
     */
    public void readStringPool() {
        int count = readInt();
        stringPool = new String[count];
        for (int i = 0; i < count; i++) {
            stringPool[i] = readString();
        }
    }

    /**
     * 读取 LangTextPool（在读取表数据之前调用）
     * 每个 TextPool 内嵌自己的 StringPool
     */
    public void readLangTextPool() {
        int langCount = readInt();
        langTextPools = new LangTextPool[langCount];
        for (int i = 0; i < langCount; i++) {
            langTextPools[i] = new LangTextPool(this);
        }
    }

    private static class LangTextPool {
        final String langName;
        final int[] indices; // 文本索引数组
        final String[] pool; // 该语言的字符串池

        LangTextPool(ConfigInput input) {
            this.langName = input.readString();
            int indexCount = input.readInt();
            this.indices = new int[indexCount];
            for (int i = 0; i < indexCount; i++) {
                indices[i] = input.readInt();
            }
            // 读取该语言的 StringPool
            int poolCount = input.readInt();
            this.pool = new String[poolCount];
            for (int i = 0; i < poolCount; i++) {
                pool[i] = input.readString();
            }
        }

        String getText(int index) {
            return pool[indices[index]];
        }
    }

    /**
     * 从全局 StringPool 读取字符串（用于 str 类型字段）
     */
    public String readStringInPool() {
        int index = readInt();
        if (stringPool == null) {
            throw new ConfigErr("StringPool not initialized. Call readStringPool() first.");
        }
        if (index < 0 || index >= stringPool.length) {
            throw new ConfigErr("String index out of bounds: " + index);
        }
        return stringPool[index];
    }

    /**
     * 从 LangTextPool 读取所有语言的文本（返回 String[]，用于 langswitch 模式的 Text 类）
     * 返回的数组包含所有语言的文本，顺序与 langTextPools 一致
     */
    public String[] readTextsInPool() {
        int index = readInt();
        if (langTextPools == null) {
            throw new ConfigErr("LangTextPool not initialized. Call readLangTextPool() first.");
        }

        String[] texts = new String[langTextPools.length];
        for (int i = 0; i < langTextPools.length; i++) {
            LangTextPool pool = langTextPools[i];
            if (index < 0 || index >= pool.indices.length) {
                throw new ConfigErr("Text index out of bounds: " + index + " for language " + i);
            }
            texts[i] = pool.getText(index);
        }
        return texts;
    }

    /**
     * 从 LangTextPool 读取当前语言的文本
     */
    public String readTextInPool() {
        int index = readInt();
        if (langTextPools == null) {
            throw new ConfigErr("LangTextPool not initialized. Call readLangTextPool() first.");
        }

        LangTextPool pool = langTextPools[0]; // 用第一个
        if (index < 0 || index >= pool.indices.length) {
            throw new ConfigErr("Text index out of bounds: " + index);
        }
        return pool.getText(index);
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}

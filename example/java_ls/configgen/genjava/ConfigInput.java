package configgen.genjava;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfFloat;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.foreign.ValueLayout.OfLong;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigInput {
    // 小端序的 layout 常量，UNALIGNED 避免 MemorySegment 按地址对齐做额外检查
    private static final OfInt INT_LE = OfInt.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final OfLong LONG_LE = OfLong.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final OfFloat FLOAT_LE = OfFloat.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final byte[] EMPTY_BYTES = new byte[0];

    // 构造时一次性读入内存，后续全程基于 MemorySegment 直接读取，
    // 避免 InputStream 逐段读取 + ByteBuffer 中转的开销。
    private final byte[] fileBytes;
    private final MemorySegment segment;
    private long pos;
    private final long limit;

    private String[] stringPool;
    private LangTextPool[] langTextPools;
    private LoadValueErrs errors;

    public LoadValueErrs errors() {
        if (errors == null) {
            errors = new LoadValueErrs();
        }
        return errors;
    }
    void setErrors(LoadValueErrs errors) { this.errors = errors; }

    public ConfigInput(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    private ConfigInput(byte[] bytes, long start, long end) {
        this.fileBytes = bytes;
        this.segment = MemorySegment.ofArray(bytes);
        this.pos = start;
        this.limit = end;
    }

    public ConfigInput(Path filePath) throws IOException {
        this(Files.readAllBytes(filePath));
    }

    public int readInt() {
        require(4);
        int v = segment.get(INT_LE, pos);
        pos += 4;
        return v;
    }

    public long readLong() {
        require(8);
        long v = segment.get(LONG_LE, pos);
        pos += 8;
        return v;
    }

    public float readFloat() {
        require(4);
        float v = segment.get(FLOAT_LE, pos);
        pos += 4;
        return v;
    }

    public boolean readBool() {
        require(1);
        byte v = segment.get(ValueLayout.JAVA_BYTE, pos);
        pos++;
        return v != 0;
    }

    public byte[] readRawBytes(int len) {
        require(len);
        if (len == 0) {
            return EMPTY_BYTES;
        }
        byte[] bytes = new byte[len];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, pos, bytes, 0, len);
        pos += len;
        return bytes;
    }

    /**
     * 跳过指定字节。数据已在内存，直接移动游标。
     */
    public void skipBytes(int n) {
        require(n);
        pos += n;
    }

    public long remaining() { return limit - pos; }

    private void require(int length) {
        if (length < 0 || length > remaining()) {
            throw errors().outOfBounds(length, remaining());
        }
    }

    public void requireEnd() {
        if (remaining() != 0) {
            throw errors().unconsumedBytes(remaining());
        }
    }

    /** A bounded view over the same bytes; advancing it cannot consume the following block. */
    public ConfigInput slice(int length) {
        require(length);
        ConfigInput result = new ConfigInput(fileBytes, pos, pos + length);
        result.errors = errors;
        result.stringPool = stringPool;
        result.langTextPools = langTextPools;
        pos += length;
        return result;
    }

    public ConfigInput fork() {
        ConfigInput result = new ConfigInput(fileBytes, pos, limit);
        result.stringPool = stringPool;
        result.langTextPools = langTextPools;
        return result;
    }

    ConfigInput withPools(ConfigInput source) {
        stringPool = source.stringPool;
        langTextPools = source.langTextPools;
        return this;
    }

    void readLanguageBlocks(String[] names, ConfigInput[] inputs) {
        langTextPools = new LangTextPool[names.length];
        int textCount = -1;
        for (int i = 0; i < names.length; i++) {
            LangTextPool pool = new LangTextPool(inputs[i], names[i]);
            inputs[i].requireEnd();
            if (textCount >= 0 && textCount != pool.indices.length) {
                throw errors().languageCountMismatch(names[i], textCount, pool.indices.length);
            }
            textCount = pool.indices.length;
            langTextPools[i] = pool;
        }
    }

    public int readCount(int minimumBytes) {
        int count = readInt();
        if (count < 0 || minimumBytes > 0 && count > remaining() / minimumBytes) {
            throw errors().invalidCount(count, minimumBytes, remaining());
        }
        return count;
    }

    // --- 字符串与池逻辑 ---

    public String readString() {
        int len = readInt();
        require(len);
        if (len == 0) {
            return "";
        }
        // 直接从已有 byte[] 解码，省掉 readRawBytes 的临时数组。
        String s = new String(fileBytes, (int) pos, len, StandardCharsets.UTF_8);
        pos += len;
        return s;
    }

    // ------Pool------

    /**
     * 读取全局 StringPool（在读取表数据之前调用）
     */
    public void readStringPool() {
        int count = readCount(4);
        stringPool = new String[count];
        for (int i = 0; i < count; i++) {
            stringPool[i] = readString();
        }
    }

    private static class LangTextPool {
        final String langName;
        final int[] indices; // 文本索引数组
        final String[] pool; // 该语言的字符串池

        LangTextPool(ConfigInput input, String name) {
            this.langName = name;
            int indexCount = input.readCount(4);
            this.indices = new int[indexCount];
            for (int i = 0; i < indexCount; i++) {
                indices[i] = input.readInt();
            }
            // 读取该语言的 StringPool
            int poolCount = input.readCount(4);
            this.pool = new String[poolCount];
            for (int i = 0; i < poolCount; i++) {
                pool[i] = input.readString();
            }
            for (int index : indices) {
                if (index < 0 || index >= pool.length) {
                    throw input.errors().invalidPoolIndex("text/" + name, index, pool.length);
                }
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
            throw new IllegalStateException("StringPool not initialized. Call readStringPool() first.");
        }
        if (index < 0 || index >= stringPool.length) {
            throw errors().invalidPoolIndex("strings", index, stringPool.length);
        }
        return stringPool[index];
    }

    /**
     * 从 LangTextPool 读取所有语言的文本（返回 String[]，用于 langswitch 模式的 Text 类）
     * 返回的数组包含所有语言的文本，顺序与 langTextPools 一致
     */
    public String[] readTextsInPool() {
        int index = readTextIndex();
        String[] texts = new String[langTextPools.length];
        for (int i = 0; i < langTextPools.length; i++) {
            texts[i] = textInPool(i, index);
        }
        return texts;
    }

    public int readTextIndex() {
        int index = readInt();
        if (langTextPools == null) {
            throw new IllegalStateException("Language pools not initialized");
        }

        if (langTextPools.length == 0) {
            throw errors().invalidPoolIndex("text", index, 0);
        }
        LangTextPool pool = langTextPools[0];
        if (index < 0 || index >= pool.indices.length) {
            throw errors().invalidPoolIndex("text/" + pool.langName, index, pool.indices.length);
        }
        return index;
    }

    public String textInPool(int language, int index) { return langTextPools[language].getText(index); }

    public String readTextInPool() { return textInPool(0, readTextIndex()); }
}

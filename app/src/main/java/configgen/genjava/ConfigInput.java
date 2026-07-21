package configgen.genjava;

import java.io.Closeable;
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

public class ConfigInput implements Closeable {
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

    private String[] stringPool;
    private LangTextPool[] langTextPools;

    public ConfigInput(byte[] bytes) {
        this.fileBytes = bytes;
        this.segment = MemorySegment.ofArray(fileBytes);
    }

    public ConfigInput(Path filePath) throws IOException {
        this(Files.readAllBytes(filePath));
    }

    public int readInt() {
        int v = segment.get(INT_LE, pos);
        pos += 4;
        return v;
    }

    public long readLong() {
        long v = segment.get(LONG_LE, pos);
        pos += 8;
        return v;
    }

    public float readFloat() {
        float v = segment.get(FLOAT_LE, pos);
        pos += 4;
        return v;
    }

    public boolean readBool() {
        byte v = segment.get(ValueLayout.JAVA_BYTE, pos);
        pos++;
        return v != 0;
    }

    /**
     * 读取指定长度的原始字节。len==0 时复用共享的 EMPTY_BYTES，避免每次 new byte[0]。
     */
    public byte[] readRawBytes(int len) {
        if (len < 0) throw new ConfigErr("Invalid length: " + len);
        if (len == 0) return EMPTY_BYTES;
        byte[] bytes = new byte[len];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, pos, bytes, 0, len);
        pos += len;
        return bytes;
    }

    /**
     * 跳过指定字节。数据已在内存，直接移动游标。
     */
    public void skipBytes(int n) {
        pos += n;
    }

    // --- 字符串与池逻辑 ---

    public String readString() {
        int len = readInt();
        if (len == 0) return "";
        // 数据已在堆内 byte[]，直接零拷贝构造 String，省掉 readRawBytes 的临时数组
        String s = new String(fileBytes, (int) pos, len, StandardCharsets.UTF_8);
        pos += len;
        return s;
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
        // InputStream 在构造时（readAllBytes 后）已通过 try-with-resources 关闭，
        // fileBytes 由 GC 回收，这里无需操作。保留签名以满足 Closeable 与现有 try-with-resources 调用。
    }
}

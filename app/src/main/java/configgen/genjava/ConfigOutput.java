package configgen.genjava;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ConfigOutput implements Closeable {
    private final OutputStream output;
    // 使用 ByteBuffer 作为中转缓冲区，预设一个合理大小（如 1024 字节）
    private final ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

    public ConfigOutput(OutputStream output) {
        // 建议外部传入时包裹 BufferedOutputStream，或者在这里包裹一层
        this.output = (output instanceof BufferedOutputStream) ? output : new BufferedOutputStream(output);
    }

    // 内部辅助方法：确保 buffer 有足够空间，或者将 buffer 刷新到流
    private void ensureCapacity(int len) {
        if (buffer.remaining() < len) {
            flushBuffer();
        }
    }

    private void flushBuffer() {
        if (buffer.position() > 0) {
            try {
                output.write(buffer.array(), 0, buffer.position());
                buffer.clear();
            } catch (IOException e) {
                throw new ConfigErr(e);
            }
        }
    }

    public void writeBool(boolean v) {
        ensureCapacity(1);
        buffer.put((byte) (v ? 1 : 0));
    }

    public void writeInt(int v) {
        ensureCapacity(4);
        buffer.putInt(v);
    }

    public void writeLong(long v) {
        ensureCapacity(8);
        buffer.putLong(v);
    }

    public void writeFloat(float v) {
        ensureCapacity(4);
        buffer.putFloat(v);
    }

    public void writeString(String v) {
        byte[] bytes = v.getBytes(StandardCharsets.UTF_8);
        writeInt(bytes.length);
        writeRawBytes(bytes, 0, bytes.length);
    }

    public void writeRawBytes(byte[] data) {
        writeRawBytes(data, 0, data.length);
    }

    public void writeRawBytes(byte[] data, int off, int len) {
        // 如果数据量大于 buffer 剩余空间，先刷新 buffer，再直接写大块数据
        if (len > buffer.remaining()) {
            flushBuffer();
            try {
                output.write(data, off, len);
            } catch (IOException e) {
                throw new ConfigErr(e);
            }
        } else {
            buffer.put(data, off, len);
        }
    }

    @Override
    public void close() {
        flushBuffer();
        try {
            output.close();
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }
}
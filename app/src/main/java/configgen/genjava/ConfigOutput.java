package configgen.genjava;

import java.io.*;
import java.nio.charset.StandardCharsets;

// 小端字节序（LE）
public class ConfigOutput implements Closeable {
    private final DataOutputStream output;

    public ConfigOutput(OutputStream output) {
        this.output = new DataOutputStream(output);
    }

    public void writeBool(boolean v) {
        try {
            output.writeBoolean(v);
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    public void writeInt(int v) {
        try {
            output.write(v);
            output.write(v >>> 8);
            output.write(v >>> 16);
            output.write(v >>> 24);
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    public void writeLong(long v) {
        try {
            output.write((int) v);
            output.write((int) (v >>> 8));
            output.write((int) (v >>> 16));
            output.write((int) (v >>> 24));
            output.write((int) (v >>> 32));
            output.write((int) (v >>> 40));
            output.write((int) (v >>> 48));
            output.write((int) (v >>> 56));
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    public void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
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
        try {
            output.write(data, off, len);
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }

    @Override
    public void close() {
        try {
            output.close();
        } catch (IOException e) {
            throw new ConfigErr(e);
        }
    }
}
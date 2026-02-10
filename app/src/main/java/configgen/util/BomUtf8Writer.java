package configgen.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BomUtf8Writer implements Closeable {
    private final OutputStream stream;
    private final OutputStreamWriter writer;
    private boolean touched;
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public BomUtf8Writer(OutputStream out) {
        stream = out;
        writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    }

    public BomUtf8Writer(Path path) throws IOException {
        this(Files.newOutputStream(path));
    }

    public void write(String str) {
        try {
            if (!touched) {
                stream.write(UTF8_BOM);
                touched = true;
            }
            writer.write(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

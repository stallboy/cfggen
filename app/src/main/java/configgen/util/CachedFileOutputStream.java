package configgen.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class CachedFileOutputStream extends ByteArrayOutputStream {
    private final Path path;

    public CachedFileOutputStream(Path path) {
        this(path, 512);
    }

    public CachedFileOutputStream(Path path, int size) {
        super(size);
        this.path = path.toAbsolutePath().normalize();
    }

    public static OutputStreamWriter createUtf8Writer(Path path) {
        return new OutputStreamWriter(new CachedFileOutputStream(path), StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        CachedFiles.writeFile(path, toByteArray());
    }
}

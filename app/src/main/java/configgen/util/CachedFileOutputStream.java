package configgen.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class CachedFileOutputStream extends ByteArrayOutputStream {
    private final Path path;
    private final CachedFiles cachedFiles;

    public CachedFileOutputStream(Path path, CachedFiles cachedFiles) {
        this(path, 512, cachedFiles);
    }

    public CachedFileOutputStream(Path path, int size, CachedFiles cachedFiles) {
        super(size);
        this.path = path.toAbsolutePath().normalize();
        this.cachedFiles = cachedFiles;
    }

    public static OutputStreamWriter createUtf8Writer(Path path, CachedFiles cachedFiles) {
        return new OutputStreamWriter(new CachedFileOutputStream(path, cachedFiles), StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        cachedFiles.writeFile(path, toByteArray());
    }
}

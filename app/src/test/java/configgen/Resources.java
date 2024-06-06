package configgen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Resources {

    public static Path addTempFileFromResourceFile(String fn, Path tempDir) {
        return addTempFileFromResourceFile(fn, tempDir, fn);
    }

    public static Path addTempFileFromResourceFile(String toFile, Path tempDir, String resourceFile) {
        Path tmp = tempDir.resolve(toFile);
        try (InputStream is = Resources.class.getClassLoader().getResourceAsStream(resourceFile)) {
            Files.copy(Objects.requireNonNull(is), tmp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tmp;
    }

    public static Path addTempFileFromText(String toFile, Path tempDir, String content) {
        Path tmp = tempDir.resolve(toFile);
        try {
            Files.writeString(tmp, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tmp;
    }

    public static String readResourceFile(String fn) {
        return readResourceFile(fn, StandardCharsets.UTF_8);
    }

    public static String readResourceFile(String fn, Charset charset) {
        try (InputStream is = Resources.class.getClassLoader().getResourceAsStream(fn)) {
            return new String(Objects.requireNonNull(is).readAllBytes(), charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

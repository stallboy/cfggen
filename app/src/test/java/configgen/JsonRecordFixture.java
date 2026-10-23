package configgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonRecordFixture {
    private JsonRecordFixture() {}

    public static void write(Path data, String table, String json) throws IOException {
        Path directory = Files.createDirectories(data.resolve("_" + table));
        Files.writeString(directory.resolve("1.json"), json);
    }
}

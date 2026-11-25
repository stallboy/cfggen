package configgen.tool;

import configgen.gen.ParameterParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CsvEncodingToolTest {
    private static final Charset GBK = Charset.forName("GBK");

    @TempDir
    Path dataDir;

    @Test
    void checkLeavesFilesAloneAndFixUsesDirectoryEncoding() throws IOException {
        Files.writeString(dataDir.resolve(".cfggen-data"), "csvEncoding=GBK\n");
        String content = "name\r\n表\r\n";
        Path csv = Files.writeString(dataDir.resolve("table.csv"), content);
        Path withBom = Files.writeString(dataDir.resolve("bom.csv"), "\uFEFF" + content);
        byte[] bomBytes = Files.readAllBytes(withBom);

        run("");
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(csv));

        run(",fix");
        assertArrayEquals(content.getBytes(GBK), Files.readAllBytes(csv));
        assertArrayEquals(bomBytes, Files.readAllBytes(withBom));
    }

    @Test
    void ambiguousInputNeedsExplicitSourceAndTargetOverridesDirectory() throws IOException {
        String settings = "\uFEFF# Read settings\r\nheadRow=3\r\ncsvEncoding=UTF-8\r\nother=keep\r\n";
        Path config = Files.writeString(dataDir.resolve(".cfggen-data"), settings, StandardCharsets.UTF_16LE);
        String content = "name\n你好\n";
        Path csv = Files.writeString(dataDir.resolve("ambiguous.csv"), content);
        String definite = "name\n表\n";
        Path another = Files.writeString(dataDir.resolve("definite.csv"), definite);

        run(",to=GBK,fix");
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(csv));
        assertArrayEquals(definite.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(another));
        assertEquals(settings, Files.readString(config, StandardCharsets.UTF_16LE));

        run(",to=GBK,source=UTF-8,fix");
        assertArrayEquals(content.getBytes(GBK), Files.readAllBytes(csv));
        assertArrayEquals(definite.getBytes(GBK), Files.readAllBytes(another));
        Properties updated = readSettings(config);
        assertEquals("GBK", updated.getProperty("csvEncoding"));
        assertEquals("3", updated.getProperty("headRow"));
        assertEquals("keep", updated.getProperty("other"));
    }

    @Test
    void fixLeavesUnrepresentableTextAlone() throws IOException {
        String content = "name\n😀\n";
        Path csv = Files.writeString(dataDir.resolve("emoji.csv"), content);

        run(",to=GBK,source=UTF-8,fix");
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(csv));
        assertFalse(Files.exists(dataDir.resolve(".cfggen-data")));
    }

    @Test
    void onlyFixCreatesMissingDirectorySettings() throws IOException {
        Files.writeString(dataDir.resolve("table.csv"), "name\nvalue\n");
        Path config = dataDir.resolve(".cfggen-data");

        run(",to=UTF-8");
        assertFalse(Files.exists(config));

        run(",to=UTF-8,fix");
        assertEquals("UTF-8", readSettings(config).getProperty("csvEncoding"));
    }

    @Test
    void equivalentEncodingLeavesOriginalSettingsBytesIntact() throws IOException {
        Path config = Files.writeString(dataDir.resolve(".cfggen-data"),
                "\uFEFF# Read settings\r\ncsvEncoding=utf8\r\n", StandardCharsets.UTF_16LE);
        byte[] original = Files.readAllBytes(config);
        var modified = Files.getLastModifiedTime(config);

        run(",to=UTF-8,fix");

        assertArrayEquals(original, Files.readAllBytes(config));
        assertEquals(modified, Files.getLastModifiedTime(config));
    }

    private static Properties readSettings(Path config) throws IOException {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(config)) {
            properties.load(reader);
        }
        return properties;
    }

    private void run(String options) {
        new CsvEncodingTool(new ParameterParser("csvencoding,datadir=" + dataDir + options)).call();
    }
}

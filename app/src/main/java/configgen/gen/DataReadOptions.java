package configgen.gen;

import configgen.data.HeadRow;
import configgen.data.HeadRows;
import configgen.util.FileUtil;
import configgen.util.UnicodeReader;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

public record DataReadOptions(HeadRow headRow, Charset csvEncoding) {
    private static final Charset DEFAULT_CSV_ENCODING = Charset.forName("GBK");
    private static final String FILE_NAME = ".cfggen-data";
    private static final String CSV_ENCODING = "csvEncoding";

    static DataReadOptions resolve(Path dataDir, @Nullable String headRowArg,
                                   @Nullable String encodingArg) {
        Path file = dataDir.resolve(FILE_NAME);
        Properties properties = readProperties(file);

        HeadRow headRow = HeadRows.A2_Default;
        String headRowId = properties.getProperty("headRow");
        if (headRowId != null) {
            try {
                headRow = HeadRows.getById(headRowId.trim());
            } catch (IllegalArgumentException e) {
                throw new CliException(file + ": invalid headRow '" + headRowId + "' (expected 2 or 3)");
            }
        }

        Charset configuredEncoding = csvEncoding(file, properties);
        Charset csvEncoding = configuredEncoding != null ? configuredEncoding : DEFAULT_CSV_ENCODING;
        if (encodingArg != null) {
            try {
                csvEncoding = Charset.forName(encodingArg.trim());
            } catch (IllegalArgumentException e) {
                throw new CliException("Invalid -encoding '" + encodingArg + "'");
            }
        }

        String headRowOverride = headRowArg != null ? headRowArg : System.getProperty("configgen.headrow");
        if (headRowOverride != null) {
            headRow = HeadRows.getById(headRowOverride);
        }
        return new DataReadOptions(headRow, csvEncoding);
    }

    public static @Nullable Charset configuredCsvEncoding(Path dataDir) {
        Path file = dataDir.resolve(FILE_NAME);
        return csvEncoding(file, readProperties(file));
    }

    public static void updateCsvEncoding(Path dataDir, Charset encoding) throws IOException {
        Path file = dataDir.resolve(FILE_NAME);
        byte[] original = Files.notExists(file) ? null : Files.readAllBytes(file);
        Properties properties = original == null ? new Properties() : loadProperties(new ByteArrayInputStream(original));
        String current = properties.getProperty(CSV_ENCODING, "").trim();
        if (encoding.name().equalsIgnoreCase(current) || encoding.aliases().stream().anyMatch(current::equalsIgnoreCase)) {
            return;
        }
        properties.setProperty(CSV_ENCODING, encoding.name());
        FileUtil.writeReplacement(file, staged -> {
            try (var writer = Files.newBufferedWriter(staged, StandardCharsets.UTF_8)) {
                properties.store(writer, null);
            }
            byte[] currentBytes = Files.notExists(file) ? null : Files.readAllBytes(file);
            if (!Arrays.equals(original, currentBytes)) {
                throw new IOException("Settings changed during conversion: " + file);
            }
        });
    }

    private static @Nullable Charset csvEncoding(Path file, Properties properties) {
        String encoding = properties.getProperty(CSV_ENCODING);
        if (encoding == null) {
            return null;
        }
        try {
            return Charset.forName(encoding.trim());
        } catch (IllegalArgumentException e) {
            throw new CliException(file + ": invalid csvEncoding '" + encoding + "'");
        }
    }

    private static Properties readProperties(Path file) {
        try {
            return loadProperties(Files.newInputStream(file));
        } catch (NoSuchFileException e) {
            return new Properties();
        } catch (IOException | IllegalArgumentException e) {
            throw new CliException("Cannot read " + file + ": " + e.getMessage());
        }
    }

    private static Properties loadProperties(InputStream input) throws IOException {
        Properties properties = new Properties();
        try (input; var reader = new UnicodeReader(input, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}

package configgen.gen;

import configgen.data.HeadRow;
import configgen.data.HeadRows;
import configgen.util.UnicodeReader;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;

record DataReadOptions(HeadRow headRow, Charset csvEncoding) {
    private static final Charset DEFAULT_CSV_ENCODING = Charset.forName("GBK");

    static DataReadOptions resolve(Path dataDir, @Nullable String headRowArg,
                                   @Nullable String encodingArg) {
        Path file = dataDir.resolve(".cfggen-data");
        Properties properties = new Properties();
        try (var source = Files.newInputStream(file);
             var reader = new UnicodeReader(source, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (NoSuchFileException e) {
            // Older data directories have no settings file.
        } catch (IOException | IllegalArgumentException e) {
            throw new CliException("Cannot read " + file + ": " + e.getMessage());
        }

        HeadRow headRow = HeadRows.A2_Default;
        String headRowId = properties.getProperty("headRow");
        if (headRowId != null) {
            try {
                headRow = HeadRows.getById(headRowId.trim());
            } catch (IllegalArgumentException e) {
                throw new CliException(file + ": invalid headRow '" + headRowId + "' (expected 2 or 3)");
            }
        }

        Charset csvEncoding = DEFAULT_CSV_ENCODING;
        String encoding = properties.getProperty("csvEncoding");
        if (encoding != null) {
            try {
                csvEncoding = Charset.forName(encoding.trim());
            } catch (IllegalArgumentException e) {
                throw new CliException(file + ": invalid csvEncoding '" + encoding + "'");
            }
        }
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
}

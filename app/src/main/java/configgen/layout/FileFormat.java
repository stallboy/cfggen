package configgen.layout;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Locale;

public enum FileFormat {
    CSV,
    EXCEL,
    CFG,
    JSON;

    static final String CSV_SUFFIX = ".csv";

    public static @Nullable FileFormat getFileFormat(Path path) {
        return getFileFormat(path.getFileName().toString());
    }

    public static @Nullable FileFormat getFileFormat(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }

        return switch (fileName.substring(dot).toLowerCase(Locale.ROOT)) {
            case CSV_SUFFIX -> CSV;
            case ".xls", ".xlsx" -> EXCEL;
            case "." + SchemaLayout.CFG_EXTENSION -> CFG;
            case JsonLayout.RECORD_SUFFIX -> JSON;
            default -> null;
        };
    }
}

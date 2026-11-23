package configgen.layout;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.StringJoiner;

public final class TableFileName {
    private TableFileName() {
    }

    public static String csvFileName(String tableName) {
        return tableName + FileFormat.CSV_SUFFIX;
    }

    public record TableNameIndex(String tableName,
                                 int index) {
    }

    public static @Nullable TableNameIndex getTableNameIndex(Path filePath, String sheetName) {
        Path parent = filePath.getParent();
        return getTableNameIndex(parent != null ? parent.resolve(sheetName) : Path.of(sheetName));
    }

    public static @Nullable TableNameIndex getTableNameIndex(Path filePath) {
        StringJoiner codeNames = new StringJoiner(".");
        for (Path path : filePath) {
            String codeName = FileNameUtil.getCodeName(path.getFileName().toString());
            if (codeName == null) {
                return null;
            }
            codeNames.add(codeName);
        }
        String fullName = codeNames.toString();
        int underscore = fullName.lastIndexOf('_');
        if (underscore >= 0) {
            try {
                int index = Integer.parseInt(fullName.substring(underscore + 1).trim());
                return new TableNameIndex(fullName.substring(0, underscore).trim(), index);
            } catch (NumberFormatException ignore) {
                // A non-numeric suffix remains part of the table name.
            }
        }
        return new TableNameIndex(fullName.trim(), 0);
    }
}

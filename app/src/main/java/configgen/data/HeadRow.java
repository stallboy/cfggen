package configgen.data;

import org.jspecify.annotations.Nullable;

public interface HeadRow {
    enum ParseBoolResult {
        TRUE, FALSE, INVALID
    }

    int rowCount();

    int commentRow();

    int nameRow();

    long parseLong(String str);

    ParseBoolResult parseBool(@Nullable String str);
}
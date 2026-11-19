package configgen.data;

import org.jspecify.annotations.Nullable;

import configgen.schema.FieldType;

public interface HeadRow {
    enum ParseBoolResult {
        TRUE, FALSE, INVALID
    }

    int rowCount();

    int commentRow();

    int nameRow();

    int suggestedTypeRow();

    @Nullable FieldType parseType(String type);

    long parseLong(String str);

    ParseBoolResult parseBool(@Nullable String str);
}
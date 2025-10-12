package configgen.ctx;

import configgen.schema.FieldType;

public interface HeadRow {
    enum ParseBoolResult {
        TRUE, FALSE, INVALID
    }

    int rowCount();

    int commentRow();

    int nameRow();

    int suggestedTypeRow();

    FieldType parseType(String type);

    long parseLong(String str);

    ParseBoolResult parseBool(String str);
}
package configgen.ctx;

import configgen.schema.FieldType;

public class HeadRows {

    public static HeadRow getById(String name) {
        return switch (name) {
            case "2" -> A2_Default;
            case "3" -> A3;
            case "4" -> A4;
            default -> throw new IllegalArgumentException("Unknown HeadRow name: " + name);
        };
    }

    static abstract class Default implements HeadRow {

        @Override
        public int commentRow() {
            return 0;
        }

        @Override
        public int nameRow() {
            return 1;
        }

        @Override
        public int suggestedTypeRow() {
            return -1;
        }

        @Override
        public FieldType parseType(String type) {
            return FieldType.Primitive.STRING;
        }

        @Override
        public long parseLong(String str) {
            return parseLongImpl(str);
        }

        @Override
        public ParseBoolResult parseBool(String str) {
            if (str == null || str.isEmpty()) {
                return ParseBoolResult.FALSE;
            }
            switch (str.toLowerCase()) {
                case "1", "true" -> {
                    return ParseBoolResult.TRUE;
                }
                case "0", "false" -> {
                    return ParseBoolResult.FALSE;
                }
                default -> {
                    return ParseBoolResult.INVALID;
                }
            }
        }
    }

    public static final HeadRow A2_Default = new Default() {
        @Override
        public int rowCount() {
            return 2;
        }
    };


    public static final HeadRow A3 = new Default() {

        @Override
        public int rowCount() {
            return 3;
        }
    };

    public static final HeadRow A4 = new HeadRow() {

        @Override
        public int rowCount() {
            return 4;
        }

        @Override
        public int commentRow() {
            return 3;
        }

        @Override
        public int nameRow() {
            return 0;
        }

        @Override
        public int suggestedTypeRow() {
            return 1;
        }

        @Override
        public FieldType parseType(String type) {
            switch (type.toUpperCase()) {
                case "INT", "SHORT", "BYTE" -> {
                    return FieldType.Primitive.INT;
                }
                case "LONG", "INT64" -> {
                    return FieldType.Primitive.LONG;
                }
                case "FLOAT" -> {
                    return FieldType.Primitive.FLOAT;
                }
                case "BOOL" -> {
                    return FieldType.Primitive.BOOL;
                }
                case "STRING", "SLICEBYTE", "HASHID" -> {
                    return FieldType.Primitive.STRING;
                }
            }
            return null;
        }

        @Override
        public long parseLong(String str) {
            return parseLongImpl(str);
        }

        @Override
        public ParseBoolResult parseBool(String str) {
            return str.equals("0") || str.equalsIgnoreCase("false") ? ParseBoolResult.FALSE : ParseBoolResult.TRUE;
        }
    };


    private static long parseLongImpl(String str) {
        if (str.isEmpty()) {
            return 0;
        }

        if (str.charAt(0) == '*') { // 为了避免用 Excel 打开大数时自动转换成科学计数法。
            str = str.substring(1);
        }
        return Long.decode(str);
    }
}

package configgen.ctx;

import configgen.schema.FieldType.Primitive;

public record HeadStructure(int commentRow,
                            int nameRow,
                            int suggestedTypeRow) {

    public static final HeadStructure DEFAULT_COMMENT_NAME = new HeadStructure(0, 1, -1);
    public static final HeadStructure ROW4_NAME_TYPE_E_COMMENT = new HeadStructure(3, 0, 1);


    public static Primitive parseType(String type) {
        switch (type.toUpperCase()) {
            case "INT", "SHORT", "BYTE" -> {
                return Primitive.INT;
            }
            case "LONG", "INT64" -> {
                return Primitive.LONG;
            }
            case "FLOAT" -> {
                return Primitive.FLOAT;
            }
            case "BOOL" -> {
                return Primitive.BOOL;
            }
            case "STRING", "SLICEBYTE", "HASHID" -> {
                return Primitive.STRING;
            }
        }
        return null;
    }

    public static long parseLong(String str) {
        if (str.isEmpty()) {
            return 0;
        }

        if (str.charAt(0) == '*') { // 不知道为啥老的配置里有这个约定
            str = str.substring(1);
        }
        return Long.decode(str);
    }
}
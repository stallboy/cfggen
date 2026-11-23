package configgen.data;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

public class HeadRows {

    public static HeadRow getById(String name) {
        return switch (name) {
            case "2" -> A2_Default;
            case "3" -> A3;
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
        public long parseLong(String str) {
            return parseLongImpl(str);
        }

        @Override
        public ParseBoolResult parseBool(@Nullable String str) {
            if (str == null || str.isEmpty()) {
                return ParseBoolResult.FALSE;
            }
            switch (str.toLowerCase(Locale.ROOT)) {
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

    private static long parseLongImpl(String str) {
        if (str.isEmpty()) {
            return 0;
        }

        if (str.charAt(0) == '*') { // 为了避免用 Excel 打开大数时自动转换成科学计数法。
            str = str.substring(1);
        }
        // 十六进制（0x/0X/# 前缀）沿用 decode，其余按十进制解析，
        // 避免前导 0 被当作八进制（"010" 静默变 8，"08"/"09" 抛异常）
        if (str.startsWith("0x") || str.startsWith("0X") || str.startsWith("#")) {
            return Long.decode(str);
        }
        return Long.parseLong(str);
    }
}

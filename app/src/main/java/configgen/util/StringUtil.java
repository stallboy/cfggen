package configgen.util;

public class StringUtil {
    public static String upper1(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    public static String lower1(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    public static String removeLineSep(String value) {
        return value.replace("\n", "---");
    }

    public static String underscoreToPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if (currentChar == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(currentChar));
                    capitalizeNext = false;
                } else {
                    result.append(currentChar);
                }
            }
        }

        return result.toString();
    }

    /**
     * 将名称转为 SCREAMING_SNAKE_CASE（Java 枚举常量风格）。
     * <p>
     * 兼容 camelCase 与已含下划线的输入：ResetDuration、Reset_Duration 都得到 RESET_DURATION。
     * 连续大写按缩写词处理，不在中间拆分：HTTPServer -> HTTP_SERVER、XMLParser -> XML_PARSER。
     * 多个连续下划线合并为一个，前导/尾随下划线丢弃；结果全部大写。
     */
    public static String toScreamingSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length() + 4);
        // 惰性分隔符：遇到 '_' 先标记，只有后面还有真实字符时才插入，从而自然丢弃前导/尾随下划线并合并连续下划线
        boolean pendingSeparator = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '_') {
                pendingSeparator = true;
                continue;
            }

            if (!result.isEmpty()) {
                char prev = input.charAt(i - 1);
                boolean prevLower = Character.isLowerCase(prev);
                boolean prevUpper = Character.isUpperCase(prev);
                boolean curUpper = Character.isUpperCase(c);
                boolean nextLower = i + 1 < input.length() && Character.isLowerCase(input.charAt(i + 1));
                // 惰性下划线、或 camelCase 边界（小写->大写）、或缩写词结尾边界（大写->大写且下一个是小写）
                if (pendingSeparator || (prevLower && curUpper) || (prevUpper && curUpper && nextLower)) {
                    result.append('_');
                }
            }
            result.append(Character.toUpperCase(c));
            pendingSeparator = false;
        }

        return result.toString();
    }

}

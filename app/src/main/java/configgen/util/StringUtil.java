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

}

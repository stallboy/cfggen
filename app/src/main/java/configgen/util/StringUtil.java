package configgen.util;

public class StringUtil {
    public static String upper1(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    public static String lower1(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    public static String firstLine(String value) {
        int idx = value.indexOf('\n');
        return idx < 0 ? value : value.substring(0, idx);
    }

    public static String removeLineSep(String value) {
        return value.replace("\n", "---");
    }
}

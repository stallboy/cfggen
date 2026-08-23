package configgen.util;

import java.text.MessageFormat;
import java.util.*;

public final class LocaleUtil {
    // 懒加载缓存：setLocale 置 null 失效；volatile 保证换语言后其他线程能立刻看到重建
    private static volatile ResourceBundle resourceBundle;
    private static final Set<Locale> availableLocalesSet;

    static {
        availableLocalesSet = new HashSet<>(Arrays.asList(Locale.getAvailableLocales()));
    }

    public static boolean isSupported(Locale l) {
        return availableLocalesSet.contains(l);
    }

    public static void setLocale(Locale l) {
        Locale.setDefault(l);
        resourceBundle = null;
    }

    public static String getLocaleString(String key, String defaultMsg) {
        String localeString = findLocaleString(key);
        if (localeString != null) {
            return localeString;
        }
        return defaultMsg;
    }

    public static String findLocaleString(String key) {
        if (resourceBundle == null) {
            resourceBundle = ResourceBundle.getBundle("messages");
        }
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException ignored) {
            return null;
        }
    }

    public static String getFormatedLocaleString(String key, String defaultMsg, Object... args) {
        String localeString = getLocaleString(key, defaultMsg);
        if (args.length > 0) {
            return MessageFormat.format(localeString, args);
        }
        return localeString;
    }
}

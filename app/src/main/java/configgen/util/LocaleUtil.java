package configgen.util;

import org.jetbrains.annotations.Contract;

import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.*;

public final class LocaleUtil {
    // 懒加载缓存：setLocale 置 null 失效；volatile 保证换语言后其他线程能立刻看到重建
    private static volatile @Nullable ResourceBundle resourceBundle;
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

    @Contract("_, !null -> !null")
    public static @Nullable String getLocaleString(String key, @Nullable String defaultMsg) {
        String localeString = findLocaleString(key);
        if (localeString != null) {
            return localeString;
        }
        return defaultMsg;
    }

    public static @Nullable String findLocaleString(String key) {
        ResourceBundle bundle = resourceBundle;
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("messages");
            resourceBundle = bundle;
        }
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ignored) {
            return null;
        }
    }

    public static String getFormatedLocaleString(String key, String defaultMsg, @Nullable Object... args) {
        String localeString = getLocaleString(key, defaultMsg);
        if (args.length > 0) {
            return MessageFormat.format(localeString, args);
        }
        return localeString;
    }
}

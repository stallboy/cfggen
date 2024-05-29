package configgen.util;

import java.text.MessageFormat;
import java.util.*;

public final class LocaleUtil {
    private static ResourceBundle resourceBundle;
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

    public static String getMessage(String key) {
        if (resourceBundle == null){
            resourceBundle = ResourceBundle.getBundle("messages");
        }
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException ignored) {
            return "";
        }
    }

    public static String getMessage(String key, Object... arguments) {
        return MessageFormat.format(getMessage(key), arguments);
    }

}

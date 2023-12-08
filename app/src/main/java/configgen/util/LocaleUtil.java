package configgen.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class LocaleUtil {
    private static ResourceBundle resourceBundle;

    public static boolean isSupported(Locale l) {
        Locale[] availableLocales = Locale.getAvailableLocales();
        return Arrays.asList(availableLocales).contains(l);
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

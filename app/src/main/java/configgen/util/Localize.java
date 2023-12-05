package configgen.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Localize {
    private final static String MESSAGES_KEY = "messages";

    private static ResourceBundle bundle;

    public static boolean isSupported(Locale l) {
        Locale[] availableLocales = Locale.getAvailableLocales();
        return Arrays.asList(availableLocales).contains(l);
    }

    public static void setLocale(Locale l) {
        Locale.setDefault(l);
        bundle = null;
    }

    public static String getMessage(String key) {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(MESSAGES_KEY);
        }
        try {
            return bundle.getString(key);
        } catch (MissingResourceException _) {
            return "";
        }
    }

    public static String getMessage(String key, Object... arguments) {
        return MessageFormat.format(getMessage(key), arguments);
    }

    public static void main(String[] args) {
        setLocale(Locale.FRENCH);
        System.out.println(Locale.getDefault());
        System.out.println(getMessage("InterfaceCellEmptyButHasNoDefaultImpl"));

        setLocale(Locale.CHINESE);
        System.out.println(Locale.getDefault());
        System.out.println(getMessage("InterfaceCellEmptyButHasNoDefaultImpl"));
    }
}

package configgen.schema;

import configgen.util.LocaleUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;

public interface Msg {

    default String msg() {
        String simpleName = this.getClass().getSimpleName();
        String formatedLocaleStr = LocaleUtil.findLocaleString("F." + simpleName);
        String simpleLocaleStr = null;
        if (formatedLocaleStr == null) {
            simpleLocaleStr = LocaleUtil.findLocaleString(simpleName);
        }
        Field[] declaredFields = this.getClass().getDeclaredFields();
        String[] args = new String[declaredFields.length];
        int i = 0;
        for (Field df : declaredFields) {
            try {
                df.setAccessible(true);
                Object v = df.get(this);
                args[i] = df.getName() + "=" + v.toString();
                i++;
            } catch (IllegalAccessException ignored) {
            }
        }

        if (formatedLocaleStr != null) {
            return MessageFormat.format(formatedLocaleStr, (Object[]) args);
        } else if (simpleLocaleStr != null) {
            return simpleLocaleStr + " " + String.join(",", args);
        }

        return toString();
    }
}

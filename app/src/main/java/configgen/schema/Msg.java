package configgen.schema;

import configgen.util.LocaleUtil;

import java.lang.reflect.Field;
import java.text.MessageFormat;

public interface Msg {

    default String msg() {
        String simpleName = this.getClass().getSimpleName();
        String formatedLocaleStr = LocaleUtil.findLocaleString("F." + simpleName);
        if (formatedLocaleStr != null) {
            Field[] declaredFields = this.getClass().getDeclaredFields();
            Object[] args = new Object[declaredFields.length];
            int i = 0;
            for (Field df : declaredFields) {
                try {
                    df.setAccessible(true);
                    Object v = df.get(this);
                    args[i] = v;
                    i++;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return MessageFormat.format(formatedLocaleStr, args);

        } else {
            String simpleLocaleStr = LocaleUtil.findLocaleString(simpleName);
            if (simpleLocaleStr != null) {
                Field[] declaredFields = this.getClass().getDeclaredFields();
                String[] args = new String[declaredFields.length];
                int i = 0;
                for (Field df : declaredFields) {
                    try {
                        df.setAccessible(true);
                        Object v = df.get(this);
                        args[i] = df.getName() + "=" + v.toString();
                        i++;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                return simpleLocaleStr + " " + String.join(",", args);
            }
        }

        return toString();
    }
}

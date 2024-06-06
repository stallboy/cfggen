package configgen.schema;

import configgen.util.Logger;

import java.lang.reflect.Field;

public interface Stat {
    default void print() {
        for (Field df : this.getClass().getDeclaredFields()) {
            try {
                df.setAccessible(true);
                int v = df.getInt(this);
                Logger.log("%20s: %d", df.getName(), v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    default void merge(Stat s) {
        for (Field df : this.getClass().getDeclaredFields()) {
            try {
                df.setAccessible(true);
                df.setInt(this, df.getInt(this) + df.getInt(s));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

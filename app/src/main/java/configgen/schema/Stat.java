package configgen.schema;

import configgen.Logger;

import java.lang.reflect.Field;

import static java.util.FormatProcessor.FMT;

public interface Stat {
    default void print() {
        for (Field df : this.getClass().getDeclaredFields()) {
            try {
                df.setAccessible(true);
                int v = df.getInt(this);
                Logger.verbose(FMT. "%20s\{ df.getName() }: %d\{ v }" );
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    default void merge(Stat s){
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

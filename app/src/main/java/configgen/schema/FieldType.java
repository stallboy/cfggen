package configgen.schema;

import java.util.Objects;

public sealed interface FieldType {

    enum Primitive implements FieldType {
        BOOL,
        INT,
        LONG,
        FLOAT,

        STR,
        /**
         * text表示要国际化
         */
        TEXT,
        /**
         * res表示资源路径
         */
        RES,
    }

    sealed interface Container extends FieldType {
    }

    record FList(FieldType item) implements Container {
        public FList {
            Objects.requireNonNull(item);
        }
    }

    record FMap(FieldType key, FieldType value) implements Container {
        public FMap {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }
    }

    final class StructRef implements FieldType {
        private final String name;
        private Fieldable obj;

        public StructRef(String name) {
            Objects.requireNonNull(name);
            this.name = name;
        }

        public String name() {
            return name;
        }

        public Fieldable obj() {
            return obj;
        }

        public void setObj(Fieldable obj) {
            this.obj = obj;
        }

        @Override
        public String toString() {
            return "StructRef{" +
                    "name='" + name + '\'' +
                    '}';
        }

        /**
         * 注意这里用obj == 即引用相同来作为比较的手段，因为我们知道 Fieldable构建后都通过引用传递
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StructRef structRef = (StructRef) o;
            return obj == structRef.obj;
        }

        @Override
        public int hashCode() {
            return Objects.hash(obj);
        }
    }

}

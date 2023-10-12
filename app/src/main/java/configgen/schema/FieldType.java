package configgen.schema;

import java.util.Objects;

public sealed interface FieldType {

    FieldType copy();

    sealed interface SimpleType extends FieldType {
        SimpleType copy();
    }

    sealed interface ContainerType extends FieldType {
        ContainerType copy();
    }


    enum Primitive implements SimpleType {
        BOOL,
        INT,
        LONG,
        FLOAT,

        STRING,
        /**
         * text表示要国际化
         */
        TEXT;

        @Override
        public Primitive copy() {
            return this;
        }
    }


    record FList(SimpleType item) implements ContainerType {
        public FList {
            Objects.requireNonNull(item);
        }

        @Override
        public FList copy() {
            return new FList(item.copy());
        }
    }

    record FMap(SimpleType key, SimpleType value) implements ContainerType {
        public FMap {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }

        @Override
        public FMap copy() {
            return new FMap(key.copy(), value.copy());
        }
    }

    final class StructRef implements SimpleType {
        private final String name;
        private Fieldable obj;

        public StructRef(String name) {
            Objects.requireNonNull(name);
            this.name = name;
        }

        @Override
        public StructRef copy() {
            return new StructRef(name);
        }


        public String name() {
            return name;
        }

        public String nameNormalized() {
            return obj.name();
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


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StructRef structRef = (StructRef) o;
            return Objects.equals(name, structRef.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

    }

}

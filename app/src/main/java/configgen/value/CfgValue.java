package configgen.value;

import configgen.ctx.TextI18n;
import configgen.data.CfgData;
import configgen.data.Source;
import configgen.schema.*;

import java.util.*;

public record CfgValue(CfgSchema schema,
                       Map<String, VTable> vTableMap) {
    public CfgValue {
        Objects.requireNonNull(schema);
        Objects.requireNonNull(vTableMap);
    }

    public Iterable<VTable> tables() {
        return vTableMap().values();
    }

    public Iterable<VTable> sortedTables() {
        Map<String, VTable> sorted = new TreeMap<>(vTableMap);
        return sorted.values();
    }

    public VTable getTable(String tableName) {
        return vTableMap.get(tableName);
    }

    public record VTable(TableSchema schema,
                         List<VStruct> valueList,

                         SequencedMap<Value, VStruct> primaryKeyMap,
                         SequencedMap<List<String>, SequencedMap<Value, VStruct>> uniqueKeyMaps,
                         SequencedSet<String> enumNames, //可为null
                         SequencedMap<String, Integer> enumNameToIntegerValueMap) { //可为null
        public VTable {
            Objects.requireNonNull(schema);
            Objects.requireNonNull(valueList);
            Objects.requireNonNull(primaryKeyMap);
            Objects.requireNonNull(uniqueKeyMaps);
        }

        public String name() {
            return schema.name();
        }
    }

    public sealed interface Value {
        Source source();

        default String packStr() {
            return ValuePack.pack(this);
        }
    }

    public sealed interface SimpleValue extends Value {
    }

    public sealed interface ContainerValue extends Value {
    }

    public static sealed abstract class CompositeValue implements Value {
        protected Source source;

        /**
         * shared用于lua生成时最小化内存占用，所以对同一个表中相同的table，就共享， 算是个优化
         */
        private boolean shared = false;

        @Override
        public Source source() {
            return source;
        }

        public void setShared() {
            shared = true;
        }

        public boolean isShared() {
            return shared;
        }
    }

    public sealed interface PrimitiveValue extends SimpleValue {
    }

    public static final class VStruct extends CompositeValue implements SimpleValue {
        private final Structural schema;
        private final List<Value> values;
        private String note;

        public VStruct(Structural schema,
                       List<Value> values,
                       Source source) {
            this.schema = schema;
            this.values = values;
            this.source = source;
        }

        public String name() {
            return schema.name();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VStruct vStruct = (VStruct) o;
            // 这里用schema == vStruct.schema，这里要保证我们不会用动态新建的struct schema去查询
            return schema == vStruct.schema && Objects.equals(values, vStruct.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(values);
        }

        public Structural schema() {
            return schema;
        }

        public List<Value> values() {
            return values;
        }

        @Override
        public String toString() {
            return "VStruct[" +
                    "schema=" + schema + ", " +
                    "values=" + values + ", " +
                    "source=" + source + ']';
        }

        public String note() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static final class VInterface extends CompositeValue implements SimpleValue {
        private final InterfaceSchema schema;
        private final VStruct child;
        private String note;

        public VInterface(InterfaceSchema schema,
                          VStruct child,
                          Source source) {
            this.schema = schema;
            this.child = child;
            this.source = source;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VInterface that = (VInterface) o;
            return schema == that.schema && Objects.equals(child, that.child);
        }

        @Override
        public int hashCode() {
            return Objects.hash(child);
        }

        public InterfaceSchema schema() {
            return schema;
        }

        public VStruct child() {
            return child;
        }

        public Source getImplNameSource() {
            if (source instanceof Source.DCellList list && !list.cells().isEmpty()) {
                return list.cells().getFirst();
            }
            return source;
        }


        @Override
        public String toString() {
            return "VInterface[" +
                    "schema=" + schema + ", " +
                    "child=" + child + ", " +
                    "source=" + source + ']';
        }

        public String note() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static final class VList extends CompositeValue implements ContainerValue {
        private final List<SimpleValue> valueList;

        public VList(List<SimpleValue> valueList,
                     Source source) {
            this.valueList = valueList;
            this.source = source;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VList vList = (VList) o;
            return Objects.equals(valueList, vList.valueList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(valueList);
        }

        public List<SimpleValue> valueList() {
            return valueList;
        }

        @Override
        public String toString() {
            return "VList[" +
                    "valueList=" + valueList + ", " +
                    "source=" + source + ']';
        }
    }

    public static final class VMap extends CompositeValue implements ContainerValue {
        private final Map<SimpleValue, SimpleValue> valueMap;

        public VMap(Map<SimpleValue, SimpleValue> valueMap,
                    Source source) {
            this.valueMap = valueMap;
            this.source = source;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VMap vMap = (VMap) o;
            return Objects.equals(valueMap, vMap.valueMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(valueMap);
        }

        public Map<SimpleValue, SimpleValue> valueMap() {
            return valueMap;
        }

        @Override
        public String toString() {
            return "VMap[" +
                    "valueMap=" + valueMap + ", " +
                    "source=" + source + ']';
        }

    }


    public record VBool(boolean value, Source source) implements PrimitiveValue {
        public VBool {
            Objects.requireNonNull(source);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VBool vBool = (VBool) o;
            return value == vBool.value;
        }

        @Override
        public int hashCode() {
            return (value ? 1 : 0);
        }
    }

    public record VInt(int value, Source source) implements PrimitiveValue {
        public VInt {
            Objects.requireNonNull(source);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VInt vInt = (VInt) o;
            return value == vInt.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public record VLong(long value, Source source) implements PrimitiveValue {
        public VLong {
            Objects.requireNonNull(source);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VLong vLong = (VLong) o;
            return value == vLong.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public record VFloat(float value, Source source) implements PrimitiveValue {
        public VFloat {
            Objects.requireNonNull(source);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VFloat vFloat = (VFloat) o;
            return Float.compare(value, vFloat.value) == 0;
        }

        public String repr() {
            if (source instanceof CfgData.DCell cell) {
                return cell.value().trim();
            } else {
                return String.valueOf(value);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public sealed interface StringValue extends PrimitiveValue {
        String value();
    }

    public record VString(String value, Source source) implements StringValue {
        public VString {
            Objects.requireNonNull(source);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VString vStr = (VString) o;
            return Objects.equals(value, vStr.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }


    public record VText(String value, String original, String nullableI18n, Source source) implements StringValue {
        public VText {
            Objects.requireNonNull(value);
            Objects.requireNonNull(original);
            Objects.requireNonNull(source);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VText vText = (VText) o;
            return Objects.equals(value, vText.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

}

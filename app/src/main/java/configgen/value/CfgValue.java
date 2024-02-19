package configgen.value;

import configgen.schema.*;

import java.util.*;
import java.util.stream.Collectors;

import static configgen.data.CfgData.DCell;

public record CfgValue(CfgSchema schema,
                       Map<String, VTable> vTableMap) {

    public Iterable<VTable> tables() {
        return vTableMap().values();
    }

    public Iterable<VTable> sortedTables() {
        Map<String, VTable> sorted = new TreeMap<>(vTableMap);
        return sorted.values();
    }

    public record VTable(TableSchema schema,
                         List<VStruct> valueList,

                         SequencedMap<Value, VStruct> primaryKeyMap,
                         SequencedMap<List<String>, SequencedMap<Value, VStruct>> uniqueKeyMaps,
                         SequencedSet<String> enumNames, //可为null
                         SequencedMap<String, Integer> enumNameToIntegerValueMap) { //可为null

        public String name() {
            return schema.name();
        }
    }

    public sealed interface Value {
        List<DCell> cells();

        default String repr() {
            return cells().stream().map(DCell::value).collect(Collectors.joining("#"));
        }

        default String packStr() {
            return ValuePack.pack(this);
        }
    }

    public sealed interface SimpleValue extends Value {
    }

    public sealed interface ContainerValue extends Value {
    }

    public static sealed abstract class CompositeValue implements Value {
        /**
         * shared用于lua生成时最小化内存占用，所以对同一个表中相同的table，就共享， 算是个优化
         */
        private boolean shared = false;

        public void setShared() {
            shared = true;
        }

        public boolean isShared() {
            return shared;
        }
    }

    public sealed interface PrimitiveValue extends SimpleValue {
        DCell cell();

        @Override
        default String repr() {
            return cell().value();
        }

        @Override
        default List<DCell> cells() {
            return List.of(cell());
        }
    }

    public static final class VStruct extends CompositeValue implements SimpleValue {
        private final Structural schema;
        private final List<Value> values;
        private final List<DCell> cells;

        public VStruct(Structural schema,
                       List<Value> values,
                       List<DCell> cells) {
            this.schema = schema;
            this.values = values;
            this.cells = cells;
        }

        public static VStruct of(Structural schema, List<Value> values) {
            List<DCell> cells = new ArrayList<>();
            for (Value value : values) {
                cells.addAll(value.cells());
            }
            return new VStruct(schema, values, cells);
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
        public List<DCell> cells() {
            return cells;
        }

        @Override
        public String toString() {
            return "VStruct[" +
                    "schema=" + schema + ", " +
                    "values=" + values + ", " +
                    "cells=" + cells + ']';
        }

    }

    public static final class VInterface extends CompositeValue implements SimpleValue {
        private final InterfaceSchema schema;
        private final VStruct child;
        private final List<DCell> cells;

        public VInterface(InterfaceSchema schema,
                          VStruct child,
                          List<DCell> cells) {
            this.schema = schema;
            this.child = child;
            this.cells = cells;
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

        @Override
        public List<DCell> cells() {
            return cells;
        }

        @Override
        public String toString() {
            return "VInterface[" +
                    "schema=" + schema + ", " +
                    "child=" + child + ", " +
                    "cells=" + cells + ']';
        }

    }

    public static final class VList extends CompositeValue implements ContainerValue {
        private final List<SimpleValue> valueList;
        private final List<DCell> cells;

        public VList(List<SimpleValue> valueList,
                     List<DCell> cells) {
            this.valueList = valueList;
            this.cells = cells;
        }

        public static VList of(List<SimpleValue> valueList) {
            List<DCell> cells = new ArrayList<>();
            for (SimpleValue value : valueList) {
                cells.addAll(value.cells());
            }
            return new VList(valueList, cells);
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
        public List<DCell> cells() {
            return cells;
        }

        @Override
        public String toString() {
            return "VList[" +
                    "valueList=" + valueList + ", " +
                    "cells=" + cells + ']';
        }
    }

    public static final class VMap extends CompositeValue implements ContainerValue {
        private final Map<SimpleValue, SimpleValue> valueMap;
        private final List<DCell> cells;

        public VMap(Map<SimpleValue, SimpleValue> valueMap,
                    List<DCell> cells) {
            this.valueMap = valueMap;
            this.cells = cells;
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
        public List<DCell> cells() {
            return cells;
        }

        @Override
        public String toString() {
            return "VMap[" +
                    "valueMap=" + valueMap + ", " +
                    "cells=" + cells + ']';
        }

    }


    public record VBool(boolean value, DCell cell) implements PrimitiveValue {
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

    public record VInt(int value, DCell cell) implements PrimitiveValue {
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

    public record VLong(long value, DCell cell) implements PrimitiveValue {

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

    public record VFloat(float value, DCell cell) implements PrimitiveValue {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VFloat vFloat = (VFloat) o;
            return Float.compare(value, vFloat.value) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public sealed interface StringValue extends PrimitiveValue {
        String value();
    }

    public record VString(String value, DCell cell) implements StringValue {

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


    public record VText(String value, String original, String nullableI18n, DCell cell) implements StringValue {

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

package configgen.value;

import configgen.schema.CfgSchema;
import configgen.schema.InterfaceSchema;
import configgen.schema.Structural;
import configgen.schema.TableSchema;

import java.util.*;
import java.util.stream.Collectors;

import static configgen.data.CfgData.DCell;

public record CfgValue(CfgSchema schema,
                       Map<String, VTable> vTableMap) {

    public Iterable<VTable> tables() {
        return vTableMap().values();
    }

    public record VTable(TableSchema schema,
                         List<VStruct> valueList,

                         Set<Value> primaryKeyValueSet,
                         Map<List<String>, Set<Value>> uniqueKeyValueSetMap,
                         Set<String> enumNames, //可为null
                         Map<String, Integer> enumNameToIntegerValueMap) { //可为null

        public String name() {
            return schema.name();
        }
    }

    public sealed interface Value {
        List<DCell> cells();

        default String repr() {
            return cells().stream().map(DCell::value).collect(Collectors.joining("#"));
        }
    }

    public sealed interface SimpleValue extends Value {
    }

    public sealed interface ContainerValue extends Value {
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

    public record VStruct(Structural schema,
                          List<Value> values,
                          List<DCell> cells) implements SimpleValue {

        public String name() {
            return schema.name();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VStruct vStruct = (VStruct) o;
            return schema == vStruct.schema && Objects.equals(values, vStruct.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(values);
        }
    }

    public record VInterface(InterfaceSchema schema,
                             VStruct child,
                             List<DCell> cells) implements SimpleValue {
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
    }

    public record VList(List<SimpleValue> valueList,
                        List<DCell> cells) implements ContainerValue {

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
    }

    public record VMap(Map<SimpleValue, SimpleValue> valueMap,
                       List<DCell> cells) implements ContainerValue {
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


    public record VText(String value, DCell cell) implements StringValue {

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

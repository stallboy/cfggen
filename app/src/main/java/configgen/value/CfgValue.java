package configgen.value;

import configgen.schema.InterfaceSchema;
import configgen.schema.Structural;
import configgen.schema.TableSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static configgen.data.CfgData.DCell;

public record CfgValue(Map<String, VTable> vTableMap) {

    public record VTable(TableSchema schema,
                         List<VStruct> valueList,

                         Set<Value> primaryKeyValueSet,
                         Map<List<String>, Set<Value>> uniqueKeyValueSetMap,
                         Set<String> enumNames,
                         Map<String, Integer> enumNameToIntegerValueMap) {

    }

    public interface Value {
    }

    public record VStruct(Structural schema,
                          List<Value> values) implements Value {
    }

    public record VInterface(InterfaceSchema schema,
                             VStruct child) implements Value {
    }

    public record VList(List<Value> valueList) implements Value {

    }

    public record VMap(Map<Value, Value> valueMap) implements Value {
    }


    public record VBool(boolean value, DCell cell) implements Value {
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

    public record VInt(int value, DCell cell) implements Value {
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

    public record VLong(long value, DCell cell) implements Value {

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

    public record VFloat(float value, DCell cell) implements Value {

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


    public record VString(String value, DCell cell) implements Value {

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


    public record VText(String value, DCell cell) implements Value {

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

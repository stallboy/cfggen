package configgen.value;

import configgen.data.CfgData;
import configgen.schema.FieldType;
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
                         CfgData.DTable data,

                         List<VStruct> valueList,

                         Set<Value> primaryKeyValueSet,
                         Map<String, Set<Value>> uniqueKeyValueSetMap,
                         Set<String> enumNames,
                         Map<String, Integer> enumNameToIntegerValueMap) {

    }

    public interface Value {
    }

    public record VStruct(Structural schema,
                          List<Value> values,
                          List<DCell> cells) implements Value {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VStruct vStruct = (VStruct) o;
            return Objects.equals(schema, vStruct.schema) && Objects.equals(values, vStruct.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(schema, values);
        }
    }

    public record VInterface(InterfaceSchema schema,
                             VStruct child,
                             List<DCell> cells) implements Value {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VInterface that = (VInterface) o;
            return Objects.equals(schema, that.schema) && Objects.equals(child, that.child);
        }

        @Override
        public int hashCode() {
            return Objects.hash(schema, child);
        }
    }

    public record VList(FieldType.FList schema,
                        List<Value> valueList,
                        List<DCell> cells) implements Value {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VList vList = (VList) o;
            return Objects.equals(schema, vList.schema) && Objects.equals(valueList, vList.valueList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(schema, valueList);
        }
    }

    public record VMap(FieldType.FMap schema,
                       Map<Value, Value> valueMap,
                       List<DCell> cells) implements Value {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VMap vMap = (VMap) o;
            return Objects.equals(schema, vMap.schema) && Objects.equals(valueMap, vMap.valueMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(schema, valueMap);
        }
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


    public record VStr(String value, DCell cell) implements Value {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VStr vStr = (VStr) o;
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

    public record VRes(String value, DCell cell) implements Value {


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VRes vRes = (VRes) o;
            return Objects.equals(value, vRes.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

}

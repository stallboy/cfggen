package configgen.value;

import configgen.data.CfgData;
import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.value.CfgValue.*;

public class ValuePack {

    /**
     * @param value 值
     * @return packStr(value)返回的值，就是你应该往excel格子里写的
     */
    public static String pack(Value value) {
        return packStr(value, false);
    }


    private static final Collector<CharSequence, ?, String> parenthesesJoin =
            Collectors.joining(",", "(", ")");
    private static final Collector<CharSequence, ?, String> join =
            Collectors.joining(",");

    private static Collector<CharSequence, ?, String> getJoin(boolean hasParenthesesAround) {
        return hasParenthesesAround ? parenthesesJoin : join;
    }

    public static String packStr(Value value, boolean hasParenthesesAround) {
        return switch (value) {
            case StringValue stringValue -> stringValue.value();
            case VBool vBool -> vBool.value() ? "true" : "false";
            case VFloat vFloat -> vFloat.repr().isEmpty() ? "0" : vFloat.repr();
            case VInt vInt -> String.valueOf(vInt.value());
            case VLong vLong -> String.valueOf(vLong.value());

            case VList vList -> vList.valueList().stream().map(v -> packStr(v, true))
                    .collect(getJoin(hasParenthesesAround));


            case VMap vMap -> vMap.valueMap().entrySet().stream()
                    .map(e -> packStr(e.getKey(), true) + "," + packStr(e.getValue(), true))
                    .collect(getJoin(hasParenthesesAround));

            case VStruct vStruct -> vStruct.values().stream()
                    .map(v -> packStr(v, true))
                    .collect(getJoin(hasParenthesesAround));


            case VInterface vInterface -> vInterface.child().schema().lastName() + vInterface.child().values().stream()
                    .map(v -> packStr(v, true))
                    .collect(parenthesesJoin);
        };
    }

    public static Value unpack(String str, FieldType type, ValueErrs errs) {
        FieldSchema field = new FieldSchema("fieldName", type, AUTO, Metadata.of());
        ValueParser parser = new ValueParser(errs, null, ValueParser.BlockParser.dummy);

        CfgData.DCell dCell = CfgData.DCell.of(str);
        return parser.parseField(field, List.of(dCell), field, true, true, 0, "tableName");
    }

    /**
     * @param id 主键pack后的字符串
     * @param tableSchema 表结构
     * @param errs 错误
     * @return 解析后的主键值，跟VTable里的primaryKeyMap的Key相对应
     */
    public static Value unpackTablePrimaryKey(String id, TableSchema tableSchema, ValueErrs errs) {
        List<FieldSchema> keyFields = tableSchema.primaryKey().fieldSchemas();
        if (keyFields.size() == 1) {
            FieldType pkFieldType = keyFields.getFirst().type();
            return unpack(id, pkFieldType, errs);
        } else {
            StructSchema obj = new StructSchema("key", AUTO, Metadata.of(), keyFields, List.of());
            FieldType.StructRef ref = new FieldType.StructRef("key");
            ref.setObj(obj);
            VStruct vStruct = (VStruct) unpack(id, ref, errs);
            List<SimpleValue> values = new ArrayList<>(vStruct.values().size());
            for (Value value : vStruct.values()) {
                if (value instanceof SimpleValue simpleValue) {
                    values.add(simpleValue);
                } else {
                    throw new IllegalStateException("multi primary key not simple type, should not happen!");
                }
            }
            return VList.of(values);
        }
    }

}

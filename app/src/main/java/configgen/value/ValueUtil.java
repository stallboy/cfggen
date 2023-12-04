package configgen.value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static configgen.value.CfgValue.*;

public class ValueUtil {

    public static Value extractKeyValue(VStruct vStruct, int[] keyIndices) {
        try {
            if (keyIndices.length == 1) {
                return vStruct.values().get(keyIndices[0]);
            } else {
                List<SimpleValue> values = new ArrayList<>(keyIndices.length);
                for (int keyIndex : keyIndices) {
                    values.add((SimpleValue) vStruct.values().get(keyIndex));
                }
                return VList.of(values);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    public static boolean isValueCellsNotAllEmpty(Value value) {
        return value.cells().stream().anyMatch(c -> !c.isCellEmpty());
    }

    /**
     * @param value 值
     * @return packStr(value)返回的值，就是你应该往excel格子里写的
     */
    public static String pack(Value value) {
        return packStr(value, false);
    }

    @SuppressWarnings("unchecked")
    private static final Collector<CharSequence, String, String> parenthesesJoin = (Collector<CharSequence, String, String>) Collectors.joining(",", "(", ")");
    @SuppressWarnings("unchecked")
    private static final Collector<CharSequence, String, String> join = (Collector<CharSequence, String, String>) Collectors.joining(",");

    public static String packStr(Value value, boolean parentheses) {
        return switch (value) {
            case StringValue stringValue -> "\"" + stringValue.value() + "\"";
            case VBool vBool -> vBool.value() ? "true" : "false";
            case VFloat vFloat -> vFloat.repr().isEmpty() ? "0" : vFloat.repr();
            case VInt vInt -> String.valueOf(vInt.value());
            case VLong vLong -> String.valueOf(vLong.value());

            case VList vList -> vList.valueList().stream().map(v -> packStr(v, true))
                    .collect(parentheses ? parenthesesJoin : join);


            case VMap vMap -> vMap.valueMap().entrySet().stream()
                    .map(e -> packStr(e.getKey(), true) + "," + packStr(e.getValue(), true))
                    .collect(parentheses ? parenthesesJoin : join);

            case VStruct vStruct -> vStruct.values().stream()
                    .map(v -> packStr(v, true))
                    .collect(parentheses ? parenthesesJoin : join);


            case VInterface vInterface -> vInterface.child().schema().lastName() + vInterface.child().values().stream()
                    .map(v -> packStr(v, true))
                    .collect(parentheses ? parenthesesJoin : join);
        };
    }

}

package configgen.value;

import java.util.ArrayList;
import java.util.List;

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
        }catch (Exception e){
            System.out.println(e.getMessage());
            throw e;
        }
    }

    public static boolean isValueCellsNotAllEmpty(Value value) {
        return value.cells().stream().anyMatch(c -> !c.isCellEmpty());
    }

}

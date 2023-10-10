package configgen.value;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.DCell;
import static configgen.value.CfgValue.SimpleValue;
import static configgen.value.CfgValue.VStruct;

class ValueUtil {

    static CfgValue.Value extractKeyValue(VStruct vStruct, int[] keyIndices) {
        if (keyIndices.length == 1) {
            return vStruct.values().get(keyIndices[0]);
        } else {
            List<SimpleValue> values = new ArrayList<>(keyIndices.length);
            for (int keyIndex : keyIndices) {
                values.add((SimpleValue) vStruct.values().get(keyIndex));
            }
            return CfgValue.VList.of(values);
        }
    }

    static boolean isValueCellsNotAllEmpty(CfgValue.Value value) {
        return value.cells().stream().anyMatch(c -> !c.isCellEmpty());
    }

}

package configgen.value;

import java.util.ArrayList;
import java.util.List;

class ValueUtil {

    static CfgValue.Value extract(CfgValue.VStruct vStruct, int[] keyIndices) {
        if (keyIndices.length == 1) {
            return vStruct.values().get(keyIndices[0]);
        } else {
            List<CfgValue.Value> values = new ArrayList<>(keyIndices.length);
            for (int keyIndex : keyIndices) {
                values.add(vStruct.values().get(keyIndex));
            }
            return new CfgValue.VList(values);
        }
    }

}

package configgen.value;

import configgen.data.CfgData;
import configgen.data.CfgData.DTable;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.value.CfgValue.Value;

public class VTableStorage {

    public static boolean addOrUpdateRecord(CfgValue cfgValue,
                                            VTable vTable,
                                            DTable dTable,
                                            Value pkValue,
                                            VStruct newRecord) {

        CfgValue.VStruct oldRecord = vTable.primaryKeyMap().get(pkValue);
        if (oldRecord != null) {
            // update
        } else {
            // add
        }

        return false;
    }

    public static boolean deleteRecord(CfgValue cfgValue,
                                       VTable vTable,
                                       VStruct oldRecord) {

        return false;
    }

}

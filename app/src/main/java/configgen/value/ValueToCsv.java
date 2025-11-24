package configgen.value;

import configgen.util.CSVUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ValueToCsv {

    public static void writeAsCsv(StringBuilder sb, CfgValue.VTable vTable, Set<String> fieldNames) {
        List<List<String>> result = new ArrayList<>(vTable.valueList().size() + 1);
        result.add(new ArrayList<>(fieldNames));

        for (CfgValue.VStruct vStruct : vTable.valueList()) {
            List<String> line = new ArrayList<>(fieldNames.size());
            for (String fieldName : fieldNames) {
                line.add(getFieldValueStr(vStruct, fieldName));
            }
            result.add(line);
        }

        CSVUtil.write(sb, result);
    }

    private static String getFieldValueStr(CfgValue.VStruct vStruct, String fieldName) {
        CfgValue.Value fv = ValueUtil.extractFieldValue(vStruct, fieldName);
        if (fv == null) {
            return "";
        }
        if (fv instanceof CfgValue.StringValue stringValue) {
            return stringValue.value();
        } else {
            return fv.packStr();
        }
    }
}

package configgen.value;

import configgen.util.CSVUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ValueToCsv {

    public static void writeAsCsv(StringBuilder sb, CfgValue.VTable vTable, Set<String> fieldNames, int offset, int limit) {
        if (offset < 0 || limit <= 0 || offset >= vTable.valueList().size()) {
            return;
        }

        if (offset + limit > vTable.valueList().size()) {
            limit = vTable.valueList().size() - offset;
        }

        List<List<String>> result = new ArrayList<>(limit + 1);
        result.add(new ArrayList<>(fieldNames));
        for (CfgValue.VStruct vStruct : vTable.valueList().subList(offset, offset + limit)) {
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

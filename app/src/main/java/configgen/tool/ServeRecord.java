package configgen.tool;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import configgen.schema.FieldType;
import configgen.value.*;
import org.apache.poi.ss.usermodel.Table;

public class ServeRecord {

    public enum ResultCode {
        ok,
        tableNotSet,
        idNotSet,
        tableNotFound,
        idFormatErr,
        idNotFound,
    }

    public record TableRecord(ResultCode resultCode,
                              JSONObject result) {
    }


    public static TableRecord getRecord(CfgValue cfgValue, String tableName, String id) {
        System.out.printf("/record %s[%s]\n", tableName, id);

        JSONObject result = new JSONObject();
        if (tableName == null) {
            return new TableRecord(ResultCode.tableNotSet, result);
        }
        if (id == null) {
            return new TableRecord(ResultCode.idNotSet, result);
        }

        CfgValue.VTable vTable = cfgValue.vTableMap().get(tableName);
        if (vTable == null) {
            return new TableRecord(ResultCode.tableNotFound, result);
        }

        FieldType pkFieldType = ValueUtil.getKeyFieldType(vTable.schema().primaryKey());

        ValueErrs errs = ValueErrs.of();
        CfgValue.Value pkValue = ValuePack.unpack(id, pkFieldType, errs);

        if (!errs.errs().isEmpty()) {
            for (ValueErrs.VErr err : errs.errs()) {
                System.out.println(err);
            }
            return new TableRecord(ResultCode.idFormatErr, result);
        }

        if (pkValue instanceof CfgValue.VStruct vPkValue){
            pkValue = ValueUtil.vStructToVList(vPkValue); // key里不会有VStruct，用VList
        }

        CfgValue.VStruct vRecord = vTable.primaryKeyMap().get(pkValue);
        if (vRecord == null) {
            return new TableRecord(ResultCode.idNotFound, result);
        }

        return new TableRecord(ResultCode.ok, ValueJson.toJson(vRecord));
    }

}

package configgen.tool;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import configgen.schema.FieldType;
import configgen.value.*;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueRefCollector.RefId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static configgen.tool.ServeRecord.ResultCode.*;

public class ServeRecord {


    public enum ResultCode {
        ok,
        tableNotSet,
        idNotSet,
        tableNotFound,
        idFormatErr,
        idNotFound,
        paramErr,
    }

    public record Refs(String value,
                       @JSONField(name = "$refs")
                       Map<String, List<RefId>> refs) {
    }

    public record TableRecord(ResultCode resultCode,
                              String table,
                              String id,
                              int depth,
                              boolean in,
                              int maxObjs,
                              JSONObject object,
                              Map<String, Map<String, Refs>> refs) {


    }

    private final CfgValue cfgValue;
    private final String table;
    private final String id;
    private final int depth;
    private final boolean in;
    private final int maxObjs;


    public ServeRecord(CfgValue cfgValue, String tableName, String id, int depth, boolean in, int maxObjs) {
        this.cfgValue = cfgValue;
        this.table = tableName;
        this.id = id;
        this.depth = depth;
        this.in = in;
        this.maxObjs = maxObjs;
    }

    private TableRecord ofErr(ResultCode code) {
        return new TableRecord(code, table, id, depth, in, maxObjs, null, null);
    }

    public TableRecord retrieve() {
        if (table == null) {
            return ofErr(tableNotSet);
        }
        if (id == null) {
            return ofErr(idNotSet);
        }
        if (depth < 0 || maxObjs <= 0) {
            return ofErr(paramErr);
        }

        CfgValue.VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return ofErr(tableNotFound);
        }

        FieldType pkFieldType = ValueUtil.getKeyFieldType(vTable.schema().primaryKey());
        ValueErrs errs = ValueErrs.of();
        CfgValue.Value pkValue = ValuePack.unpack(id, pkFieldType, errs);

        if (!errs.errs().isEmpty()) {
            for (ValueErrs.VErr err : errs.errs()) {
                System.out.println(err);
            }
            return ofErr(idFormatErr);
        }

        if (pkValue instanceof VStruct vPkValue) {
            pkValue = ValueUtil.vStructToVList(vPkValue); // key里不会有VStruct，用VList
        }
        String id = pkValue.packStr();

        VStruct vRecord = vTable.primaryKeyMap().get(pkValue);
        if (vRecord == null) {
            return ofErr(idNotFound);
        }


        Map<RefId, VStruct> frontier = new LinkedHashMap<>();
        ValueToJson vj = new ValueToJson(frontier);
        JSONObject object = vj.toJson(vRecord);
        RefId thisObjId = new RefId(table, id);
        frontier.remove(thisObjId);


        Map<RefId, Refs> result = new LinkedHashMap<>();
        int curDepth = 0;
        while (curDepth < depth) {
            Map<RefId, VStruct> newFrontier = new LinkedHashMap<>();

            for (Map.Entry<RefId, VStruct> e : frontier.entrySet()) {
                RefId refId = e.getKey();
                VStruct record = e.getValue();

                Map<String, List<RefId>> refIdMap = new LinkedHashMap<>();
                ValueRefCollector collector = new ValueRefCollector(newFrontier, refIdMap);
                collector.collect(record, List.of());

                result.put(refId, new Refs(record.packStr(), refIdMap));

                if (result.size() > maxObjs) {
                    break;
                }
            }

            if (result.size() > maxObjs) {
                break;
            }

            for (RefId refId : result.keySet()) {
                newFrontier.remove(refId);
            }
            newFrontier.remove(thisObjId);
            frontier = newFrontier;
            curDepth++;
        }


        Map<String, Map<String, Refs>> refs = new LinkedHashMap<>();
        for (Map.Entry<RefId, Refs> e : result.entrySet()) {
            RefId refId = e.getKey();
            Map<String, Refs> tableValues = refs.computeIfAbsent(refId.table(), k -> new LinkedHashMap<>());
            tableValues.put(refId.id(), e.getValue());
        }

        return new TableRecord(ok, table, id, depth, in, maxObjs, object, refs);
    }

}

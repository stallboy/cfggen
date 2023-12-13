package configgen.tool;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import configgen.schema.FieldType;
import configgen.schema.TableSchemaRefGraph;
import configgen.value.*;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueRefCollector.RefId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static configgen.tool.ServeRecord.ResultCode.*;
import static configgen.value.CfgValue.VTable;
import static configgen.value.CfgValue.Value;

public class ServeRecord {

    public interface RecordResponse {
    }

    public record TableRecord(ResultCode resultCode,
                              String table,
                              String id,
                              int maxObjs,
                              JSONObject object,
                              // table -> (id -> refs)
                              Map<String, Map<String, Refs>> refs) implements RecordResponse {
    }

    public record TableRecordRefs(ResultCode resultCode,
                                  String table,
                                  String id,
                                  int depth,
                                  boolean in,
                                  int maxObjs,
                                  // table -> (id -> refs)
                                  Map<String, Map<String, Refs>> refs) implements RecordResponse {
    }


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
                       // refName -> [refId]
                       @JSONField(name = "$refs")
                       Map<String, List<RefId>> refs,
                       int depth) {
    }

    public enum RequestType {
        requestRecord,
        requestRefs
    }

    private final CfgValue cfgValue;
    private final TableSchemaRefGraph graph;

    private final String table;
    private final String id;
    private final int depth;
    private final boolean in;
    private final int maxObjs;
    private final RequestType requestType;

    public ServeRecord(CfgValue cfgValue, TableSchemaRefGraph graph,
                       String tableName, String id,
                       int depth, boolean in, int maxObjs, RequestType requestType) {
        this.cfgValue = cfgValue;
        this.graph = graph;
        this.table = tableName;
        this.id = id;
        this.depth = depth;
        this.in = in;
        this.maxObjs = maxObjs;
        this.requestType = requestType;
    }

    private RecordResponse ofErr(ResultCode code) {
        return switch (requestType) {
            case requestRecord -> new TableRecord(code, table, id, maxObjs, null, null);
            case requestRefs -> new TableRecordRefs(code, table, id, depth, in, maxObjs, null);
        };
    }

    public RecordResponse retrieve() {
        if (table == null) {
            return ofErr(tableNotSet);
        }
        if (id == null) {
            return ofErr(idNotSet);
        }
        if (depth < 0 || maxObjs <= 0) {
            return ofErr(paramErr);
        }

        VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return ofErr(tableNotFound);
        }

        FieldType pkFieldType = ValueUtil.getKeyFieldType(vTable.schema().primaryKey());
        ValueErrs errs = ValueErrs.of();
        Value pkValue = ValuePack.unpack(id, pkFieldType, errs);

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
        RefId thisObjId = new RefId(table, id);
        JSONObject object = null;
        int curDepth = 0;
        switch (requestType) {
            case requestRecord -> {
                object = new ValueToJson(frontier).toJson(vRecord);
                frontier.remove(thisObjId);
                curDepth = 1;
            }
            case requestRefs -> {
                frontier.put(thisObjId, vRecord);
            }
        }


        Map<RefId, Refs> result = new LinkedHashMap<>();

        while (curDepth <= depth) {
            Map<RefId, VStruct> newFrontier = new LinkedHashMap<>();

            for (Map.Entry<RefId, VStruct> e : frontier.entrySet()) {
                RefId refId = e.getKey();
                VStruct record = e.getValue();

                Map<String, List<RefId>> refIdMap = new LinkedHashMap<>();
                ValueRefCollector collector = new ValueRefCollector(newFrontier, refIdMap);
                collector.collect(record, List.of());

                result.put(refId, new Refs(record.packStr(), refIdMap, curDepth));

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

        if (in) {
            ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);
            Map<RefId, Value> refIns = refInCollector.collect(vTable, pkValue);
            if (!refIns.isEmpty()) {
                for (RefId r : result.keySet()) {
                    refIns.remove(r);
                }
            }

            Map<String, List<RefId>> staticRefIn = Map.of("refin", List.of(thisObjId));
            for (Map.Entry<RefId, Value> e : refIns.entrySet()) {
                RefId refId = e.getKey();
                result.put(refId, new Refs(e.getValue().packStr(), staticRefIn, -1));

                if (result.size() > maxObjs + 8) {
                    break;
                }
            }
        }


        Map<String, Map<String, Refs>> refs = new LinkedHashMap<>();
        for (Map.Entry<RefId, Refs> e : result.entrySet()) {
            RefId refId = e.getKey();
            Map<String, Refs> tableValues = refs.computeIfAbsent(refId.table(), k -> new LinkedHashMap<>());
            tableValues.put(refId.id(), e.getValue());
        }

        return switch (requestType) {
            case requestRecord -> new TableRecord(ok, table, id, maxObjs, object, refs);
            case requestRefs -> new TableRecordRefs(ok, table, id, depth, in, maxObjs, refs);
        };

    }

}

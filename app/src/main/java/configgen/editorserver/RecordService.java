package configgen.editorserver;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import configgen.schema.EntryType;
import configgen.schema.TableSchema;
import configgen.schema.TableSchemaRefGraph;
import configgen.value.*;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueRefCollector.RefId;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static configgen.editorserver.RecordService.ResultCode.*;
import static configgen.value.CfgValue.VTable;
import static configgen.value.CfgValue.Value;

public class RecordService {

    public interface RecordResponse {
    }

    public record TableRecord(
            ResultCode resultCode,
            String table,
            String id,
            int maxObjs,
            JSONObject object,
            Collection<BriefRecord> refs) implements RecordResponse {
    }

    public record TableRecordRefs(
            ResultCode resultCode,
            String table,
            String id,
            int depth,
            boolean in,
            int maxObjs,
            Collection<BriefRecord> refs) implements RecordResponse {
    }


    public enum ResultCode {
        ok,
        tableNotSet,
        idNotSet,
        tableNotFound,
        idParseErr,
        idNotFound,
        paramErr,
    }

    public record BriefRecord(
            String table,
            String id,

            String img,
            String title,
            String description,
            String value,  // 完整信息
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

    public RecordService(CfgValue cfgValue, TableSchemaRefGraph graph,
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

        ValueErrs errs = ValueErrs.of();
        Value pkValue = ValuePack.unpackTablePrimaryKey(id, vTable.schema(), errs);

        if (!errs.errs().isEmpty()) {
            for (ValueErrs.VErr err : errs.errs()) {
                System.err.println(err);
            }
            return ofErr(idParseErr);
        }

        if (pkValue instanceof VStruct vPkValue && vTable.schema().primaryKey().fields().size() > 1) {
            pkValue = ValueUtil.vStructToVList(vPkValue); // 多key时schema是struct，但value用的是VList，这里要转换下
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
                object = new ValueToJson(cfgValue, frontier).toJson(vRecord);
                frontier.remove(thisObjId);
                curDepth = 1;
            }
            case requestRefs -> {
                frontier.put(thisObjId, vRecord);
            }
        }


        Map<RefId, BriefRecord> result = new LinkedHashMap<>();

        while (curDepth <= depth) {
            Map<RefId, VStruct> newFrontier = new LinkedHashMap<>();

            for (Map.Entry<RefId, VStruct> e : frontier.entrySet()) {
                RefId refId = e.getKey();
                VStruct record = e.getValue();

                Map<String, List<RefId>> refIdMap = new LinkedHashMap<>();
                ValueRefCollector collector = new ValueRefCollector(cfgValue, newFrontier, refIdMap);
                collector.collect(record, List.of());

                result.put(refId, vStructToBriefRecord(refId, record, refIdMap, curDepth));

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
            Map<RefId, VStruct> refIns = refInCollector.collect(vTable, pkValue);
            if (!refIns.isEmpty()) {
                for (RefId r : result.keySet()) {
                    refIns.remove(r);
                }
            }

            Map<String, List<RefId>> staticRefIn = Map.of("refin", List.of(thisObjId));
            for (Map.Entry<RefId, VStruct> e : refIns.entrySet()) {
                RefId refId = e.getKey();
                result.put(refId, vStructToBriefRecord(refId, e.getValue(), staticRefIn, -1));

                if (result.size() > maxObjs + 8) {
                    break;
                }
            }
        }


        return switch (requestType) {
            case requestRecord -> new TableRecord(ok, table, id, maxObjs, object, result.values());
            case requestRefs -> new TableRecordRefs(ok, table, id, depth, in, maxObjs, result.values());
        };

    }

    private static BriefRecord vStructToBriefRecord(RefId refId, VStruct vStruct, Map<String, List<RefId>> refs, int depth) {
        String img = getBriefValue(vStruct, "img");
        String title = getBriefTitle(vStruct);
        String description = getBriefValue(vStruct, "description");
        String value = vStruct.packStr();
        return new BriefRecord(refId.table(), refId.id(), img, title, description, value, refs, depth);
    }

    private static String getBriefValue(VStruct vStruct, String briefKey) {
        String fieldName = vStruct.schema().meta().getStr(briefKey, null);
        if (fieldName == null) {
            return null;
        }

        Value fv = ValueUtil.extractFieldValue(vStruct, fieldName);
        if (fv == null) {
            return null;
        }

        if (fv instanceof CfgValue.StringValue stringValue) {
            return stringValue.value();
        }
        return null;
    }

    public static String getBriefTitle(VStruct vStruct) {
        String title = getBriefValue(vStruct, "title");

        String enumName = null;
        if (vStruct.schema() instanceof TableSchema tableSchema) {
            if (tableSchema.entry() instanceof EntryType.EEnum eEnum) {
                Value fv = ValueUtil.extractFieldValue(vStruct, eEnum.field());
                if (fv instanceof CfgValue.VString vString) {
                    enumName = vString.value();
                }
            }
        }
        if (enumName != null) {
            if (title != null) {
                return enumName + ": " + title;
            } else {
                return enumName;
            }
        } else {
            return title;
        }
    }
}

package configgen.editorserver;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import configgen.schema.EntryType;
import configgen.schema.FieldSchema;
import configgen.schema.TableSchema;
import configgen.schema.TableSchemaRefGraph;
import configgen.value.*;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueRefCollector.FieldRef;
import configgen.value.ValueRefCollector.RefId;

import java.util.*;

import static configgen.editorserver.RecordService.ResultCode.*;
import static configgen.value.CfgValue.VTable;
import static configgen.value.CfgValue.Value;

/**
 * cfgeditor里record关系界面，record展示，和edit界面 需要的数据 都是由这个service提供
 */
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

    public record BriefDescription(
            String field,
            String value,
            String comment) {
    }

    public record BriefRecord(
            String table,
            String id,

            String title,
            List<BriefDescription> descriptions,
            String value,  // 完整信息
            // refName -> [refId]
            @JSONField(name = "$refs")
            Collection<FieldRef> refs,
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

        CfgValueErrs errs = CfgValueErrs.of();
        Value pkValue = ValuePack.unpackTablePrimaryKey(id, vTable.schema(), errs);

        if (!errs.errs().isEmpty()) {
            for (CfgValueErrs.VErr err : errs.errs()) {
                System.err.println(err);
            }
            return ofErr(idParseErr);
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
            Map<RefId, VStruct> newFrontier = new LinkedHashMap<>();  // 新节点

            for (Map.Entry<RefId, VStruct> e : frontier.entrySet()) {
                RefId refId = e.getKey();
                VStruct record = e.getValue();

                List<FieldRef> fieldRefs = new ArrayList<>();  // 现有到新节点的  链接
                ValueRefCollector collector = new ValueRefCollector(cfgValue, newFrontier, fieldRefs);
                collector.collect(record, List.of());

                result.put(refId, vStructToBriefRecord(refId, record, fieldRefs, curDepth));

                if (result.size() > maxObjs) {
                    break;
                }
            }

            if (result.size() > maxObjs) {
                break;
            }

            // 去重
            for (RefId refId : result.keySet()) {
                newFrontier.remove(refId);
            }
            newFrontier.remove(thisObjId);

            frontier = newFrontier;
            curDepth++;
        }

        if (in) {
            ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);
            Map<RefId, ForeachVStruct.Context> refIns = refInCollector.collect(vTable, pkValue);
            if (!refIns.isEmpty()) {
                for (RefId r : result.keySet()) {
                    refIns.remove(r);
                }
            }

            for (Map.Entry<RefId, ForeachVStruct.Context> e : refIns.entrySet()) {
                RefId refId = e.getKey();
                VStruct vStruct = e.getValue().recordValue();
                List<FieldRef> fieldRefs = ValueRefCollector.collectRefs(vStruct, cfgValue);
                result.put(refId, vStructToBriefRecord(refId, vStruct, fieldRefs, -1));
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

    private static BriefRecord vStructToBriefRecord(RefId refId, VStruct vStruct, Collection<FieldRef> refs, int depth) {
        String title = getBriefTitle(vStruct);
        List<BriefDescription> descriptions = getBriefDescriptions(vStruct);
        String value = vStruct.packStr();
        return new BriefRecord(refId.table(), refId.id(), title, descriptions, value, refs, depth);
    }

    @SuppressWarnings("SameParameterValue")
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
        } else {
            return fv.packStr();
        }
    }

    public static String getBriefTitle(VStruct vStruct) {
        String title = getBriefValue(vStruct, "title");

        String enumName = null;
        if (vStruct.schema() instanceof TableSchema tableSchema &&
            tableSchema.entry() instanceof EntryType.EEnum eEnum &&
            tableSchema.primaryKey().fieldSchemas().getFirst() != eEnum.fieldSchema()) { // 主键不是enum，才组合enumName

            Value fv = ValueUtil.extractFieldValue(vStruct, eEnum.field());
            if (fv instanceof CfgValue.VString vString) {
                enumName = vString.value();
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

    public static List<BriefDescription> getBriefDescriptions(VStruct vStruct) {
        String fields = vStruct.schema().meta().getStr("description", null);
        if (fields == null) {
            return null;
        }

        List<BriefDescription> descriptions = new ArrayList<>();
        for (String f : fields.split(",")) {
            String fieldName = f.trim();
            Value fv = ValueUtil.extractFieldValue(vStruct, fieldName);
            if (fv == null) {
                continue;
            }

            FieldSchema fs = vStruct.schema().findField(fieldName);
            if (fs == null) {
                continue;
            }

            String value;
            if (fv instanceof CfgValue.StringValue stringValue) {
                value = stringValue.value();
            } else {
                value = fv.packStr();
            }

            descriptions.add(new BriefDescription(fieldName, value, fs.comment()));
        }

        return descriptions;
    }
}

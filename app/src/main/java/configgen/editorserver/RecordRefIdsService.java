package configgen.editorserver;

import configgen.schema.TableSchemaRefGraph;
import configgen.value.*;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueRefCollector.FieldRef;
import configgen.value.ValueRefCollector.RefId;

import java.util.*;

import static configgen.editorserver.RecordService.ResultCode.*;
import static configgen.editorserver.RecordService.getBriefTitle;
import static configgen.value.CfgValue.VTable;
import static configgen.value.CfgValue.Value;

/**
 * 这个相比RecordService返回的更简洁。可以包含更多层record id
 */
public class RecordRefIdsService {

    public record RecordRefIdsResponse(
            RecordService.ResultCode resultCode,
            String table,
            String id,
            int inDepth,
            int outDepth,
            int maxRefIds,
            Collection<RecordRefId> recordRefIds) {
    }

    public record RecordRefId(
            String table,
            String id,
            String title,
            int depth/*-1,-2,1,2..*/) {
    }

    private final CfgValue cfgValue;
    private final TableSchemaRefGraph graph;

    private final String table;
    private final String id;
    private final int inDepth;
    private final int outDepth;
    private final int maxRefIds;

    public RecordRefIdsService(CfgValue cfgValue,
                               TableSchemaRefGraph graph,
                               String tableName, String id,
                               int inDepth, int outDepth, int maxRefIds) {
        this.cfgValue = cfgValue;
        this.graph = graph;
        this.table = tableName;
        this.id = id;
        this.inDepth = inDepth;
        this.outDepth = outDepth;
        this.maxRefIds = maxRefIds;
    }

    private RecordRefIdsResponse ofErr(RecordService.ResultCode code) {
        return new RecordRefIdsResponse(code, table, id, inDepth, outDepth, maxRefIds, null);
    }

    public RecordRefIdsResponse retrieve() {
        if (table == null) {
            return ofErr(tableNotSet);
        }
        if (id == null) {
            return ofErr(idNotSet);
        }
        if (inDepth < 0 || outDepth < 0 || maxRefIds <= 0) {
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


        // 先放入自身节点
        Map<RefId, RecordRefId> result = new LinkedHashMap<>();
        RefId thisObjId = new RefId(table, id);
        result.put(thisObjId, new RecordRefId(thisObjId.table(), thisObjId.id(), getBriefTitle(vRecord), 0));

        // 优先算 refIn的链接
        var refInFrontier = Map.of(thisObjId, new ForeachVStruct.Context(vTable, pkValue, vRecord));
        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);
        int curInDepth = 1;
        while (curInDepth <= inDepth) {
            Map<RefId, ForeachVStruct.Context> newRefInFrontier = new LinkedHashMap<>();
            for (Map.Entry<RefId, ForeachVStruct.Context> e : refInFrontier.entrySet()) {
                VTable vt = e.getValue().fromVTable();
                Value pkV = e.getValue().pkValue();
                refInCollector.collectTo(vt, pkV, newRefInFrontier);
            }

            if (!newRefInFrontier.isEmpty()) {
                for (RefId r : result.keySet()) {
                    newRefInFrontier.remove(r);
                }

                for (Map.Entry<RefId, ForeachVStruct.Context> ri : newRefInFrontier.entrySet()) {
                    RefId refId = ri.getKey();
                    result.put(refId, new RecordRefId(refId.table(), refId.id(), getBriefTitle(ri.getValue().recordValue()), -curInDepth));
                    if (result.size() > maxRefIds) {
                        break;
                    }
                }
            }

            if (result.size() > maxRefIds) {
                break;
            }
            refInFrontier = newRefInFrontier;
            curInDepth++;
        }

        // 然后算 refOut的链接
        var refOutFrontier = Map.of(thisObjId, vRecord);
        int curOutDepth = 1;
        while (curOutDepth <= outDepth) {
            Map<RefId, VStruct> newRefOutFrontier = new LinkedHashMap<>();

            for (Map.Entry<RefId, VStruct> e : refOutFrontier.entrySet()) {
                List<FieldRef> fieldRefs = new ArrayList<>();
                ValueRefCollector collector = new ValueRefCollector(cfgValue, newRefOutFrontier, fieldRefs);
                collector.collect(e.getValue(), List.of());
            }

            if (!newRefOutFrontier.isEmpty()) {
                for (RefId refId : result.keySet()) {
                    newRefOutFrontier.remove(refId);
                }

                for (Map.Entry<RefId, VStruct> ro : newRefOutFrontier.entrySet()) {
                    RefId refId = ro.getKey();
                    result.put(refId, new RecordRefId(refId.table(), refId.id(), getBriefTitle(ro.getValue()), curOutDepth));
                    if (result.size() > maxRefIds) {
                        break;
                    }
                }

                if (result.size() > maxRefIds) {
                    break;
                }
            }
            refOutFrontier = newRefOutFrontier;
            curOutDepth++;
        }

        return new RecordRefIdsResponse(RecordService.ResultCode.ok, table, id, inDepth, outDepth, maxRefIds,
                result.values());

    }

}

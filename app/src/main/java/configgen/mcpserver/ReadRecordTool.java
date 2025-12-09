package configgen.mcpserver;

import com.alibaba.fastjson2.JSONWriter;
import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import com.github.codeboyzhou.mcp.declarative.server.McpStructuredContent;
import configgen.genbyai.TableRelatedInfoFinder;
import configgen.genbyai.TableRelatedInfoFinder.TableRecordList;
import configgen.value.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class ReadRecordTool {

    public enum ErrorCode {
        OK,
        TableNotFound,
    }

    public record ListTableRecordResult(ErrorCode errorCode,
                                        String table,
                                        String recordsInCsv) implements McpStructuredContent {
    }

    @McpTool(description = "list table record")
    public ListTableRecordResult listTableRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                                 String tableName,
                                                 @McpToolParam(name = "extraFields", description = "extra fields to show, use comma to separate")
                                                 String extraFields) {

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return new ListTableRecordResult(ErrorCode.TableNotFound, tableName, "");
        }

        TableRecordList list = TableRelatedInfoFinder.getTableRecordListInCsv(vTable,
                extraFields != null ? extraFields.split(",") : null);
        return new ListTableRecordResult(ErrorCode.OK, tableName, list.contentInCsvFormat());
    }

    public enum ReadRecordErrorCode {
        OK,
        TableNotFound,
        RecordIdParseError,
        RecordNotFound,
    }

    public record ReadRecordResult(ReadRecordErrorCode errorCode,
                                   String table,
                                   String recordId,
                                   List<String> errorMessages,
                                   String recordJson) implements McpStructuredContent {
    }

    @McpTool(description = "read one record")
    public ReadRecordResult readRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                       String tableName,
                                       @McpToolParam(name = "recordId", description = "record id", required = true)
                                       String recordId) {

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return new ReadRecordResult(ReadRecordErrorCode.TableNotFound, tableName, recordId, List.of(), null);
        }

        CfgValueErrs errs = CfgValueErrs.of();
        CfgValue.Value pkValue = ValuePack.unpackTablePrimaryKey(recordId, vTable.schema(), errs);

        if (!errs.errs().isEmpty()) {

            return new ReadRecordResult(ReadRecordErrorCode.RecordIdParseError, tableName, recordId,
                    errs.errs().stream().map(CfgValueErrs.VErr::toString).toList(), null);
        }

        CfgValue.VStruct vRecord = vTable.primaryKeyMap().get(pkValue);
        if (vRecord == null) {
            return new ReadRecordResult(ReadRecordErrorCode.RecordNotFound, tableName, recordId, List.of(), null);
        }

        Map<ValueRefCollector.RefId, CfgValue.VStruct> frontier = new LinkedHashMap<>();
        String object = new ValueToJson(cfgValue, frontier).toJson(vRecord).toJSONString(JSONWriter.Feature.PrettyFormat);
        return new ReadRecordResult(ReadRecordErrorCode.OK, tableName, recordId, List.of(), object);
    }
}

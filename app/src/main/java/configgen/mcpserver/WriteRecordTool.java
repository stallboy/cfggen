package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import com.github.codeboyzhou.mcp.declarative.server.McpStructuredContent;
import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.schema.TableSchema;
import configgen.value.*;
import configgen.write.VTableJsonStorage;
import configgen.write.VTableStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
public class WriteRecordTool {

    public enum AddOrUpdateErrorCode {
        OK,
        PartialNotEditable,
        TableNotFound,
        RecordParseError,
        IOException
    }

    public record AddOrUpdateRecordResult(AddOrUpdateErrorCode errorCode,
                                          String table,
                                          String recordId,
                                          List<String> errorMessages) implements McpStructuredContent {
    }

    @McpTool(description = "add or update record")
    public AddOrUpdateRecordResult addOrUpdateRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                                     String tableName,
                                                     @McpToolParam(name = "recordJsonStr", description = "record json string", required = true)
                                                     String recordJsonStr) {

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        Context context = vc.context();
        CfgValue cfgValue = vc.cfgValue();
        if (cfgValue.schema().isPartial()) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.PartialNotEditable, tableName, null, List.of());
        }

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.TableNotFound, tableName, null, List.of());
        }

        TableSchema tableSchema = vTable.schema();
        CfgValueErrs parseErrs = CfgValueErrs.of();
        CfgValue.VStruct thisValue = new ValueJsonParser(vTable.schema(), parseErrs).fromJson(recordJsonStr);
        parseErrs.checkErrors("check json", true, true);
        if (!parseErrs.errs().isEmpty()) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.RecordParseError, tableName, null,
                    parseErrs.errs().stream().map(CfgValueErrs.VErr::toString).toList());
        }

        CfgValue.Value pkValue = ValueUtil.extractPrimaryKeyValue(thisValue, tableSchema);
        String id = pkValue.packStr();
        try {
            if (vTable.schema().isJson()) {
                Path writePath = VTableJsonStorage.addOrUpdateRecord(thisValue, tableName, id,
                        context.getSourceStructure().getRootDir());
            } else {
                CfgData.DTable dTable = context.cfgData().getDTable(tableName);
                VTableStorage.addOrUpdateRecord(context, vTable, dTable, pkValue, thisValue);
            }
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.OK, tableName, id, List.of());
        } catch (IOException e) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.IOException, tableName, id,
                    List.of(e.getMessage()));
        }
    }

    public enum DeleteErrorCode {
        OK,
        PartialNotEditable,
        TableNotFound,
        RecordIdParseError,
        RecordIdNotFound,
        IOException
    }

    public record DeleteRecordResult(DeleteErrorCode errorCode,
                                     String table,
                                     String recordId,
                                     List<String> errorMessages) implements McpStructuredContent {
    }

    @McpTool(description = "delete record")
    public DeleteRecordResult deleteRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                           String tableName,
                                           @McpToolParam(name = "recordId", description = "record id", required = true)
                                           String recordId) {
        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        Context context = vc.context();
        CfgValue cfgValue = vc.cfgValue();

        if (cfgValue.schema().isPartial()) {
            return new DeleteRecordResult(DeleteErrorCode.PartialNotEditable, tableName, recordId, List.of());
        }

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return new DeleteRecordResult(DeleteErrorCode.TableNotFound, tableName, recordId, List.of());
        }

        CfgValueErrs errs = CfgValueErrs.of();
        CfgValue.Value pkValue = ValuePack.unpackTablePrimaryKey(recordId, vTable.schema(), errs);

        if (!errs.errs().isEmpty()) {
            return new DeleteRecordResult(DeleteErrorCode.RecordIdParseError, tableName, recordId,
                    errs.errs().stream().map(CfgValueErrs.VErr::toString).toList());
        }

        CfgValue.VStruct old = vTable.primaryKeyMap().get(pkValue);
        if (old == null) {
            return new DeleteRecordResult(DeleteErrorCode.RecordIdNotFound, tableName, recordId, List.of());
        }
        try {
            if (vTable.schema().isJson()) {
                VTableJsonStorage.deleteRecord(tableName, recordId,
                        context.getSourceStructure().getRootDir());
            } else {
                VTableStorage.deleteRecord(context, old);
            }
            return new DeleteRecordResult(DeleteErrorCode.OK, tableName, recordId, List.of());
        } catch (Exception e) {
            return new DeleteRecordResult(DeleteErrorCode.IOException, tableName, recordId,
                    List.of(e.getMessage()));
        }
    }
}

package configgen.tool;

import configgen.ctx.Context;
import configgen.ctx.Context.ContextCfg;
import configgen.ctx.HeadRow;
import configgen.ctx.HeadRows;
import configgen.data.CfgData;
import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.schema.CfgSchema;
import configgen.schema.HasBlock;
import configgen.schema.TableSchema;
import configgen.util.Logger;
import configgen.value.CfgValueErrs;
import configgen.value.ComparingBlockParser;
import configgen.value.ComparingBlockParser.BlockDiff;
import configgen.value.VTableParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * block 解析算法迁移检测（-tool blockmigrate）：
 * parseBlock 在 8aa463fc 把嵌套边界判定从「firstColIndex-1 列是否为空」换成「祖先 block 首列集合」，
 * 属破坏性变更。本工具针对项目自身的 schema+data，在 parseBlock 的 CellsWithRowIndex 层级同时跑新旧
 * 两套算法、对比每个 block 字段收集到的行，列出受影响的表/record/行号，供升级后核对。
 */
public class BlockMigrationTool extends Tool {
    private final Path dataDir;
    private final HeadRow headRow;
    private final String encoding;
    private final String to;

    public BlockMigrationTool(Parameter parameter) {
        super(parameter);
        this.dataDir = Path.of(parameter.get("datadir", "."));
        this.headRow = HeadRows.getById(parameter.get("headrow", "2"));
        this.encoding = parameter.get("encoding", "GBK");
        this.to = parameter.get("to", null);
    }

    @Override
    public void call() {
        ContextCfg cfg = new ContextCfg(dataDir, null, headRow, encoding, null, null, null);
        Context ctx = new Context(cfg);
        MigrationReport report = compare(ctx);
        String text = formatReport(report);
        Logger.log(text);
        if (to != null) {
            try {
                Files.writeString(Path.of(to), text);
                Logger.log("report written to %s", to);
            } catch (IOException e) {
                Logger.log("failed to write report: %s", e.getMessage());
            }
        }
    }

    /**
     * 对所有含 block 的表，注入 {@link ComparingBlockParser} 跑一遍 parseTable，
     * 收集新旧算法在每个 block 字段上的行号差异。static 便于测试直接调用。
     */
    public static MigrationReport compare(Context ctx) {
        CfgSchema schema = ctx.cfgSchema();
        HeadRow headRow = ctx.contextCfg().headRow();
        Map<String, List<BlockDiff>> diffs = new LinkedHashMap<>();
        int scanned = 0;
        for (TableSchema table : schema.tableMap().values()) {
            if (!HasBlock.hasBlock(table) || table.isJson()) {
                continue;
            }
            scanned++;
            CfgData.DTable dTable = ctx.cfgData().getDTable(table.name());
            if (dTable == null) {
                continue;
            }
            List<BlockDiff> collector = new ArrayList<>();
            CfgValueErrs errs = CfgValueErrs.of();  // 迁移检测允许带错，不 checkErrors
            new VTableParser(table, dTable, table, headRow, errs,
                    new ComparingBlockParser(dTable, table, collector)).parseTable();
            if (!collector.isEmpty()) {
                diffs.put(table.fullName(), collector);
            }
        }
        return new MigrationReport(scanned, diffs);
    }

    private static String formatReport(MigrationReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("==== Block 解析迁移检测 ====\n");
        sb.append("旧（≤8aa463fc^）：firstColIndex-1 列是否为空  →  ")
          .append("新（≥8aa463fc）：祖先 block 首列集合任一非空即结束\n");
        if (!report.hasDiff()) {
            sb.append(String.format("扫描 %d 个含 block 的表，未发现差异：本次 parseBlock 算法变更不影响你的数据。\n",
                    report.scannedTableCount()));
            return sb.toString();
        }
        sb.append(String.format("扫描 %d 个含 block 的表，%d 个表的 %d 条 record 存在差异：\n\n",
                report.scannedTableCount(), report.diffTableCount(), report.diffRecordCount()));
        for (Map.Entry<String, List<BlockDiff>> e : report.diffs().entrySet()) {
            for (BlockDiff d : e.getValue()) {
                sb.append(String.format("[表 %s] record 第 %d 行 (pk=%s) 字段 %s block 首列=列%d:\n",
                        e.getKey(), d.recordRow(), d.pkDesc(), d.fieldName(), d.firstColIndex()));
                sb.append(String.format("  旧 %d 个%s → 新 %d 个%s\n",
                        d.legacySize(), d.legacyRowIndices(),
                        d.newSize(), d.newRowIndices()));
                List<Integer> lost = d.newOnly();
                if (!lost.isEmpty()) {
                    sb.append(String.format("  旧算法疑似提前 break 丢失：行%s\n", lost));
                }
                List<Integer> extra = d.legacyOnly();
                if (!extra.isEmpty()) {
                    sb.append(String.format("  新算法丢弃（旧多算）：行%s\n", extra));
                }
            }
        }
        return sb.toString();
    }

    public record MigrationReport(int scannedTableCount, Map<String, List<BlockDiff>> diffs) {
        public boolean hasDiff() {
            return !diffs.isEmpty();
        }

        public int diffTableCount() {
            return diffs.size();
        }

        public int diffRecordCount() {
            return diffs.values().stream().mapToInt(List::size).sum();
        }
    }
}

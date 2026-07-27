package configgen.value;

import configgen.data.CfgData.DCell;
import configgen.data.CfgData.DTable;
import configgen.schema.TableSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static configgen.value.ValueParser.BlockParser;
import static configgen.value.ValueParser.CellsWithRowIndex;

/**
 * 迁移检测用的 BlockParser：在 {@code parseBlock} 返回的 {@link CellsWithRowIndex} 列表层级，
 * 同时用新算法（{@link VTableBlockParser}）和旧算法（firstColIndex-1，≤8aa463fc^）各算一份并对比，
 * 把差异记入 collector，再返回新算法结果让 ValueParser 继续构造（最终 value 即正确的新算法产物）。
 * <p>
 * 设计意图：一次解析内完成双算法对比，不重跑整表；差异天然带行号，精确定位到被旧算法误判提前 break
 * 丢失的行。报告里的定位均为 Excel/csv 实际位置（数据文件 + record 首行行号 + 往上扫得到的主键）。
 * <p>
 * 安全性：新旧两份结果共享同一 dTable；DCell.value 不可变、isCellEmpty 只看 value、setModePackOrSep
 * 只改 mode 位（block 判定不用 mode），第二算法拿到改过 mode 的 cell 不影响判定。
 */
public class ComparingBlockParser implements BlockParser {
    private final VTableBlockParser newBp;
    private final DTable dTable;
    private final List<Integer> pkColumnIndices;
    private final String tableFullName;
    private final List<BlockDiff> collector;

    public ComparingBlockParser(DTable dTable, TableSchema tableSchema, List<BlockDiff> collector) {
        this.newBp = new VTableBlockParser(dTable, tableSchema);
        this.dTable = dTable;
        this.pkColumnIndices = VTableParser.getPkColumnIndices(tableSchema);
        this.tableFullName = tableSchema.fullName();
        this.collector = collector;
    }

    @Override
    public List<CellsWithRowIndex> parseBlock(List<DCell> cells, int curRowIndex) {
        DCell firstCell = cells.getFirst();
        int firstColIndex = VTableBlockParser.findColumnIndex(firstCell, dTable.rows().get(curRowIndex));

        List<CellsWithRowIndex> newRes = newBp.parseBlock(cells, curRowIndex);
        List<CellsWithRowIndex> legacyRes = legacyParseBlock(cells, curRowIndex, firstColIndex);

        List<Integer> newRowIndices = excelRows(newRes);
        List<Integer> legacyRowIndices = excelRows(legacyRes);
        if (!Objects.equals(newRowIndices, legacyRowIndices)) {
            // 内层 block 的 curRowIndex 落在 block 行（pk 列空），往上扫到 record 首行取 pk 和定位行号
            int recordFirstRow = findRecordFirstRow(curRowIndex);
            DCell recordCell = dTable.rows().get(recordFirstRow).getFirst();
            collector.add(new BlockDiff(
                    tableFullName,
                    dTable.getSheetByRowId(recordCell.rowId()).id(),
                    recordCell.displayRow(),
                    pkDesc(recordFirstRow),
                    firstCell.displayCol(),
                    newBp.fieldNameOf(firstColIndex),
                    newRowIndices,
                    legacyRowIndices));
        }
        return newRes;
    }

    /** 旧算法（≤8aa463fc^）：用 firstColIndex-1 这一列是否为空判嵌套边界。取自 979d6a80。 */
    private List<CellsWithRowIndex> legacyParseBlock(List<DCell> cells, int curRowIndex, int firstColIndex) {
        int rowSize = dTable.rows().size();
        int colSize = cells.size();

        List<CellsWithRowIndex> res = new ArrayList<>();
        res.add(new CellsWithRowIndex(cells, curRowIndex));

        for (int row = curRowIndex + 1; row < rowSize; row++) {
            List<DCell> line = dTable.rows().get(row);
            if (VTableParser.isPkCellAllEmpty(line, pkColumnIndices)) {  // 主键所在格子为空，还是本 record
                DCell prevCell = line.get(firstColIndex - 1);
                DCell thisCell = line.get(firstColIndex);

                if (prevCell.isCellEmpty()) { // 上一格为空
                    //noinspection StatementWithEmptyBody
                    if (thisCell.isCellEmpty()) { // 本格也为空，内部的嵌套 block，忽略
                    } else { // 本格不为空 -> 是这个 block 了
                        res.add(new CellsWithRowIndex(line.subList(firstColIndex, firstColIndex + colSize), row));
                    }
                } else { // 上一格不为空，结束
                    break;
                }
            } else { // 下一个 record，结束
                break;
            }
        }
        return res;
    }

    /** 从 curRowIndex 往上找 record 首行（主键所在格子非空的第一行）。 */
    private int findRecordFirstRow(int curRowIndex) {
        for (int r = curRowIndex; r >= 0; r--) {
            if (!VTableParser.isPkCellAllEmpty(dTable.rows().get(r), pkColumnIndices)) {
                return r;
            }
        }
        return curRowIndex;
    }

    private List<Integer> excelRows(List<CellsWithRowIndex> blocks) {
        List<Integer> r = new ArrayList<>(blocks.size());
        for (CellsWithRowIndex b : blocks) {
            r.add(b.cells().getFirst().displayRow());
        }
        return r;
    }

    private String pkDesc(int recordRowIndex) {
        List<DCell> recordRow = dTable.rows().get(recordRowIndex);
        StringBuilder sb = new StringBuilder();
        for (int pkIdx : pkColumnIndices) {
            if (!sb.isEmpty()) {
                sb.append(",");
            }
            sb.append(recordRow.get(pkIdx).value());
        }
        return sb.toString();
    }

    /**
     * 一处 block 字段的新旧解析差异。定位均为 Excel/csv 实际位置。
     *
     * @param table           表全名（统计分组用）
     * @param source          数据文件位置：csv 为文件路径，excel 为 文件路径[sheet名]
     * @param recordRow       record 首行的 Excel 行号
     * @param pkDesc          record 首行主键值
     * @param firstCol        block 字段首列的 Excel 列字母
     * @param fieldName       block 字段名
     * @param newRowIndices   新算法收集到的 Excel 行号
     * @param legacyRowIndices 旧算法收集到的 Excel 行号
     */
    public record BlockDiff(String table,
                            String source,
                            int recordRow,
                            String pkDesc,
                            String firstCol,
                            String fieldName,
                            List<Integer> newRowIndices,
                            List<Integer> legacyRowIndices) {
        public int newSize() {
            return newRowIndices.size();
        }

        public int legacySize() {
            return legacyRowIndices.size();
        }

        /** 新算法有、旧算法没有的行号 = 旧算法丢失的行。 */
        public List<Integer> newOnly() {
            List<Integer> r = new ArrayList<>(newRowIndices);
            r.removeAll(legacyRowIndices);
            return r;
        }

        /** 旧算法有、新算法没有的行号 = 新算法丢弃的行（少见）。 */
        public List<Integer> legacyOnly() {
            List<Integer> r = new ArrayList<>(legacyRowIndices);
            r.removeAll(newRowIndices);
            return r;
        }
    }
}

package configgen.value;

import configgen.schema.HasBlock;
import configgen.schema.TableSchema;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.DCell;
import static configgen.data.CfgData.DTable;
import static configgen.value.CfgValue.VStruct;
import static configgen.value.CfgValue.VTable;
import static configgen.value.TextI18n.TableI18n;
import static configgen.value.ValueParser.BlockParser;
import static configgen.value.ValueParser.CellsWithRowIndex;

public class VTableParser implements BlockParser {
    private final TableSchema subTableSchema;
    private final DTable dTable;
    private final TableSchema tableSchema;
    private final ValueErrs errs;
    private final ValueParser parser;
    private List<DCell> curRow;

    public VTableParser(TableSchema subTableSchema, DTable dTable, TableSchema tableSchema,
                        TableI18n nullableTableI18n, ValueErrs errs) {
        this.subTableSchema = subTableSchema;
        this.dTable = dTable;
        this.tableSchema = tableSchema;
        this.errs = errs;
        this.parser = new ValueParser(errs, nullableTableI18n, this);
    }

    public VTable parseTable() {
        boolean hasBlock = HasBlock.hasBlock(tableSchema);

        int rowCnt = dTable.rows().size();
        List<VStruct> valueList = new ArrayList<>(); //可能会多，无所谓
        for (int curRecordRow = 0; curRecordRow < rowCnt; ) {
            curRow = dTable.rows().get(curRecordRow);
            VStruct vStruct = parser.parseStructural(subTableSchema, curRow, tableSchema, false, true, curRecordRow);
            if (vStruct != null) {
                valueList.add(vStruct);
            }
            curRecordRow++;

            if (hasBlock) {
                while (curRecordRow < rowCnt) {
                    List<DCell> nr = dTable.rows().get(curRecordRow);
                    // 用第一列 格子是否为空来判断这行是属于上一个record的block，还是新的一格record
                    if (nr.getFirst().value().isEmpty()) {
                        curRecordRow++;  // 具体提取让VList，VMap，通郭parseBlock自己去提取
                    } else {
                        break;
                    }
                }
            }
        }

        return new VTableCreator(subTableSchema, tableSchema, errs).create(valueList);
    }


    // 要允许block<struct>,struct里仍然有block，如下所示
    // xxxaabbxccc
    //      bb ccc
    //      bb
    //    aabb
    //      bb
    // aabb前面一列要有空格，bb前一列格子也要是空，ccc前一列也是有个空，
    // 用这个空来做为标记，支持block aabb嵌套block bb，来判断此行bb是否属于嵌套的bb还是新起的aabb
    // 这样也强制了2个同级的block不要直接衔接，视觉上不好区分，
    // 可以可以在中间加入一个列，比如以上的aabb和ccc直接有x来分割
    // 以上规则现在没有做检测，要检测有点复杂，人工保证吧。
    @Override
    public List<CellsWithRowIndex> parseBlock(List<DCell> cells, int curRowIndex) {
        DCell firstCell = cells.getFirst();
        int rowSize = dTable.rows().size();
        int firstColIndex = findColumnIndex(firstCell);

        int colSize = cells.size();

        List<CellsWithRowIndex> res = new ArrayList<>();
        res.add(new CellsWithRowIndex(cells, curRowIndex));

        for (int row = curRowIndex + 1; row < rowSize; row++) {
            List<DCell> line = dTable.rows().get(row);

            // 属于上一个record的block
            if (line.getFirst().isCellEmpty()) {
                // 上一格为空，本格不为空 -》 是这个block了
                if (line.get(firstColIndex - 1).isCellEmpty() && !line.get(firstColIndex).isCellEmpty()) {
                    res.add(new CellsWithRowIndex(line.subList(firstColIndex, firstColIndex + colSize), row));
                }
                // else 这里不会break，这样来支持嵌套
            } else {
                // 下一个record了
                break;
            }
        }

        return res;
    }


    private int findColumnIndex(DCell cell) {
        int i = 0;
        for (DCell c : curRow) {
            if (c.col() == cell.col()) {
                return i;
            }
            i++;
        }
        return i;
    }

}

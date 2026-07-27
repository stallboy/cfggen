package configgen.value;

import configgen.ctx.HeadRow;
import configgen.schema.FieldSchema;
import configgen.schema.HasBlock;
import configgen.schema.Span;
import configgen.schema.TableSchema;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.DCell;
import static configgen.data.CfgData.DTable;
import static configgen.value.CfgValue.VStruct;
import static configgen.value.CfgValue.VTable;

/**
 * 单表 value 解析驱动：按主键遍历 record 行，把每行交给 ValueParser.parseStructural。
 * block 提取由注入的 {@link ValueParser.BlockParser} 决定——生产路径默认 {@link VTableBlockParser}，
 * 迁移工具可注入 {@link ComparingBlockParser} 做新旧算法对比。
 * <p>
 * block 嵌套边界判定算法、祖先首列预算已移至 VTableBlockParser。
 */
public class VTableParser {
    private final TableSchema subTableSchema;
    private final DTable dTable;
    private final TableSchema tableSchema;
    private final CfgValueErrs errs;
    private final ValueParser parser;
    private final List<Integer> pkColumnIndices;

    public VTableParser(TableSchema subTableSchema,
                        DTable dTable,
                        TableSchema tableSchema,
                        HeadRow headRow,
                        CfgValueErrs errs) {
        this(subTableSchema, dTable, tableSchema, headRow, errs, new VTableBlockParser(dTable, tableSchema));
    }

    /**
     * 注入自定义 BlockParser（迁移对比用）。生产路径走默认 VTableBlockParser，应调 5 参构造；
     * 此 public 重载供 {@code configgen.tool.BlockMigrationTool} 跨包注入 {@link ComparingBlockParser}。
     */
    public VTableParser(TableSchema subTableSchema,
                        DTable dTable,
                        TableSchema tableSchema,
                        HeadRow headRow,
                        CfgValueErrs errs,
                        ValueParser.BlockParser blockParser) {
        this.subTableSchema = subTableSchema;
        this.dTable = dTable;
        this.tableSchema = tableSchema;
        this.errs = errs;
        this.parser = new ValueParser(errs, headRow, blockParser);
        this.pkColumnIndices = getPkColumnIndices(tableSchema);
    }

    public VTable parseTable() {
        boolean hasBlock = HasBlock.hasBlock(tableSchema);

        int rowCnt = dTable.rows().size();
        List<VStruct> valueList = new ArrayList<>(); //可能会多，无所谓
        for (int curRecordRow = 0; curRecordRow < rowCnt; ) {
            List<DCell> curRow = dTable.rows().get(curRecordRow);
            VStruct vStruct = parser.parseStructural(subTableSchema, curRow, tableSchema,
                    new ValueParser.ParseContext(tableSchema.fullName(), false, true, curRecordRow));
            if (vStruct != null) {
                valueList.add(vStruct);
            }
            curRecordRow++;

            if (hasBlock) {
                while (curRecordRow < rowCnt) {
                    List<DCell> nr = dTable.rows().get(curRecordRow);
                    // 用主键所在格子是否全为空  来判断这行是属于上一个 record 的 block，还是新的一格 record
                    if (isPkCellAllEmpty(nr, pkColumnIndices)) {
                        curRecordRow++;  // 具体提取让 VList，VMap，通过 parseBlock 自己去提取
                    } else {
                        break;
                    }
                }
            }
        }

        return new VTableCreator(subTableSchema, errs).create(valueList);
    }

    // 主键所在格子是否全为空
    static boolean isPkCellAllEmpty(List<DCell> row, List<Integer> pkColumnIndices) {
        for (Integer pkIndex : pkColumnIndices) {
            DCell dCell = row.get(pkIndex);
            if (!dCell.value().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static List<Integer> getPkColumnIndices(TableSchema schema) {
        List<FieldSchema> pks = schema.primaryKey().fieldSchemas();
        List<Integer> pkIndices = new ArrayList<>(pks.size());
        for (FieldSchema pk : pks) {
            int idx = 0;
            for (FieldSchema f : schema.fields()) {
                int span = Span.fieldSpan(f);
                if (f == pk) {
                    for (int i = 0; i < span; i++) {
                        pkIndices.add(idx + i);
                    }
                    break;
                }
                idx += span;
            }
        }
        return pkIndices;
    }


}

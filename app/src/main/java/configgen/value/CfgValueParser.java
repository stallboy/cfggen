package configgen.value;

import configgen.data.CfgData;
import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import static configgen.data.CfgData.DCell;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.value.CfgValue.VInterface;
import static configgen.value.CfgValue.VStruct;

public enum CfgValueParser {
    INSTANCE;

    public CfgValue parseCfgValue(CfgSchema subSchema, CfgData data, CfgSchema schema) {
        subSchema.requireResolved();
        schema.requireResolved();

        CfgValue value = new CfgValue(new TreeMap<>());
        for (TableSchema subTable : subSchema.tableMap().values()) {
            String name = subTable.name();
            CfgData.DTable dTable = data.tables().get(name);
            Objects.requireNonNull(dTable);
            TableSchema table = schema.findTable(name);
            Objects.requireNonNull(table);

            CfgValue.VTable vTable = parseTable(subTable, dTable, table);
            value.vTableMap().put(name, vTable);
        }
        return value;
    }

    private CfgValue.VTable parseTable(TableSchema subTable, CfgData.DTable dTable, TableSchema table) {
        boolean hasBlock = HasBlock.hasBlock(table);

        int rowCnt = dTable.rows().size();
        List<VStruct> valueList = new ArrayList<>(); //可能会多，无所谓
        for (int i = 0; i < rowCnt; ) {
            List<DCell> row = dTable.rows().get(i);
            VStruct vStruct = parseStructural(subTable, row, table, false, true);
            valueList.add(vStruct);
            i++;

            if (hasBlock) {
                while (i < rowCnt) {
                    List<DCell> nr = dTable.rows().get(i);
                    // 用第一列 格子是否为空来判断这行是属于上一个record的block，还是新的一格record
                    if (nr.get(0).value().isEmpty()) {
                        i++;  // 具体提取让VList，VMap，通郭parseBlock自己去提取
                    } else {
                        break;
                    }
                }
            }
        }

        return null;
    }

    private VStruct parseStructural(Nameable subNameable, List<DCell> cells, Nameable nameable, boolean pack, boolean isRootCell) {

        boolean fromEmptyRoot = false; // 支持excel里的cell为空，并且它还是个复合结构
        List<DCell> parsed;
        pack = pack || nameable.fmt() == PACK;
        if (pack) {
            require(cells.size() == 1, "pack应该只占一格");
            DCell cell = cells.get(0);

            if (isRootCell && cell.value().isEmpty()) {
                fromEmptyRoot = true;
                parsed = cells;
            } else if (nameable instanceof InterfaceSchema) {
                parsed = DCells.parseFunc(cell);
            } else {
                parsed = DCells.parseNestList(cell);
            }

        } else if (nameable.fmt() instanceof FieldFormat.Sep sep) {
            require(cells.size() == 1, "sep应该只占一格");
            DCell cell = cells.get(0);

            if (isRootCell && cell.value().isEmpty()) {
                fromEmptyRoot = true;
                parsed = cells;
            } else {
                parsed = DCells.parseList(cell, sep.sep());
            }

        } else {
            require(cells.size() == Spans.span(nameable), "列宽度应一致");
            parsed = cells;
        }

        if (fromEmptyRoot) {
            if (nameable instanceof InterfaceSchema interfaceSchema) {

                String defaultImpl = interfaceSchema.defaultImpl();
                require(!defaultImpl.isEmpty(), "当整个interface要用默认值时，里面包含的多态基类必须设置defaultImpl");
                StructSchema child = interfaceSchema.findImpl(defaultImpl);
                require(child != null, STR. "\{ defaultImpl } impl不存在" );

                InterfaceSchema subInterface = (InterfaceSchema)subNameable;
                StructSchema subChild = subInterface.findImpl(defaultImpl);
                require(subChild != null, STR. "\{ defaultImpl } impl不存在" );


                // childTBean就只能是个没有column的空子Bean
                // 这里设置packAsOne参数为true，跟下面else一致
                VStruct vChild = parseStructural(subChild, parsed, child, true, true);
                VInterface vInterface = new VInterface(subInterface, vChild, parsed);


            } else {

            }

        }

        return null;

    }

    private void require(boolean cond, String err) {
        if (!cond)
            throw new AssertionError(err);
    }


}

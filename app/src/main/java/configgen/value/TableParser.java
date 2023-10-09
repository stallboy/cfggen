package configgen.value;

import configgen.data.CfgData;
import configgen.schema.*;
import configgen.schema.EntryType.EntryBase;

import java.util.*;

import static configgen.data.CfgData.DCell;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldFormat.Block;
import static configgen.schema.FieldFormat.Sep;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.VInterface;
import static configgen.value.CfgValue.VStruct;
import static configgen.value.ValueErrs.*;

public class TableParser {
    private final TableSchema subTableSchema;
    private final CfgData.DTable dTable;
    private final TableSchema tableSchema;
    private final ValueErrs errs;
    private List<DCell> curRow;

    public TableParser(TableSchema subTableSchema, CfgData.DTable dTable, TableSchema tableSchema, ValueErrs errs) {
        this.subTableSchema = subTableSchema;
        this.dTable = dTable;
        this.tableSchema = tableSchema;
        this.errs = errs;
    }

    public CfgValue.VTable parseTable() {
        boolean hasBlock = HasBlock.hasBlock(tableSchema);

        int rowCnt = dTable.rows().size();
        List<VStruct> valueList = new ArrayList<>(); //可能会多，无所谓
        for (int curRecordRow = 0; curRecordRow < rowCnt; ) {
            curRow = dTable.rows().get(curRecordRow);
            VStruct vStruct = parseStructural(subTableSchema, curRow, tableSchema, false, true, curRecordRow);
            valueList.add(vStruct);
            curRecordRow++;

            if (hasBlock) {
                while (curRecordRow < rowCnt) {
                    List<DCell> nr = dTable.rows().get(curRecordRow);
                    // 用第一列 格子是否为空来判断这行是属于上一个record的block，还是新的一格record
                    if (nr.get(0).value().isEmpty()) {
                        curRecordRow++;  // 具体提取让VList，VMap，通郭parseBlock自己去提取
                    } else {
                        break;
                    }
                }
            }
        }

        Set<CfgValue.Value> primaryKeyValueSet = new LinkedHashSet<>();
        Map<List<String>, Set<CfgValue.Value>> uniqueKeyValueSetMap = new LinkedHashMap<>();

        // 收集主键和唯一键
        extractKeyValues(primaryKeyValueSet, valueList, subTableSchema.primaryKey());
        for (KeySchema uniqueKey : subTableSchema.uniqueKeys()) {
            Set<CfgValue.Value> res = new LinkedHashSet<>();
            extractKeyValues(res, valueList, uniqueKey);
            uniqueKeyValueSetMap.put(uniqueKey.name(), res);
        }

        // 收集枚举
        Set<String> enumNames = null;
        Map<String, Integer> enumNameToIntegerValueMap = null;
        if (subTableSchema.entry() instanceof EntryBase entry) {
            Set<String> names = new HashSet<>();
            int idx = findIndex(entry.fieldSchema());

            enumNames = new LinkedHashSet<>();

            int pkIdx = -1;
            List<FieldSchema> pk = subTableSchema.primaryKey().obj();
            if (pk.size() == 1 && pk.get(0) != entry.fieldSchema()) {
                pkIdx = findIndex(pk.get(0));
                enumNameToIntegerValueMap = new LinkedHashMap<>();
            }

            for (VStruct vStruct : valueList) {
                CfgValue.VString vStr = (CfgValue.VString) vStruct.values().get(idx);
                String e = vStr.value();
                if (e.contains(" ")) {
                    errs.addErr(new EntryContainsSpace(vStr.cell(), tableSchema.name()));
                    continue;
                }

                if (e.isEmpty()) {
                    if (entry instanceof EntryType.EEnum) {
                        errs.addErr(new EnumEmpty(vStr.cell(), tableSchema.name()));
                    }
                } else {
                    boolean add = names.add(e.toUpperCase());
                    if (!add) {
                        errs.addErr(new EntryDuplicated(vStr.cell(), tableSchema.name()));
                    } else {
                        enumNames.add(e);

                        if (pkIdx != 0) { //必须是int，这里是java生成需要
                            CfgValue.VInt vInt = (CfgValue.VInt) vStruct.values().get(pkIdx);
                            enumNameToIntegerValueMap.put(e, vInt.value());
                        }
                    }
                }
            }
        }


        return new CfgValue.VTable(subTableSchema, valueList,
                primaryKeyValueSet, uniqueKeyValueSetMap, enumNames, enumNameToIntegerValueMap);
    }

    private void extractKeyValues(Set<CfgValue.Value> keyValueSet, List<VStruct> valueList, KeySchema key) {
        int size = key.obj().size();
        int[] keyIndices = new int[size];
        int i = 0;
        for (FieldSchema k : key.obj()) {
            keyIndices[i] = findIndex(k);
            i++;
        }

        int idx = keyIndices[0];


        for (VStruct value : valueList) {
            CfgValue.Value keyValue;
            if (keyIndices.length == 1) {
                keyValue = value.values().get(idx);
            } else {
                List<CfgValue.Value> values = new ArrayList<>(size);
                for (int keyIndex : keyIndices) {
                    values.add(value.values().get(keyIndex));
                }

                keyValue = new CfgValue.VList(values);
            }

            boolean add = keyValueSet.add(keyValue);
            if (!add) {
                errs.addErr(new PrimaryOrUniqueKeyDuplicated(keyValue, tableSchema.name(), key.name()));
            }
        }
    }

    private int findIndex(FieldSchema field) {
        int i = 0;
        for (FieldSchema f : subTableSchema.fields()) {
            if (f == field) {
                return i;
            }
            i++;
        }
        return i;
    }


    VInterface parseInterface(InterfaceSchema subInterface, List<DCell> cells, InterfaceSchema sInterface,
                              boolean pack, boolean canBeEmpty, int curRowIndex) {
        List<DCell> parsed = cells;

        boolean isEmpty = false; // 支持excel里的cell为空，并且它还是个复合结构；但不支持非empty的cell里部分结构为空
        boolean isNumberOrBool = false;
        boolean isPack = pack || sInterface.fmt() == PACK;
        if (isPack) {
            require(cells.size() == 1, "pack应该只占一格");
            DCell cell = cells.get(0);
            if (canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else if (sInterface.canBeNumberOrBool()) {
                if (DCells.isFunc(cell)) {
                    parsed = DCells.parseFunc(cell);
                    canBeEmpty = false; // 只要是parse了结构的，内部就不允许为空了
                } else {
                    isNumberOrBool = true;
                }
            } else {
                parsed = DCells.parseFunc(cell);
                canBeEmpty = false;
            }

        } else if (sInterface.fmt() instanceof Sep sep) {
            require(cells.size() == 1, "sep应该只占一格");
            DCell cell = cells.get(0);
            if (canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else {
                parsed = DCells.parseList(cell, sep.sep());
                canBeEmpty = false;
            }
        } else {
            require(cells.size() == Spans.span(sInterface), "列宽度应一致");
            canBeEmpty = false;
        }

        // 内容为空的单一格子，处理方式是把这空的parsed一层层传下去
        VStruct vImpl;
        if (isEmpty) {
            StructSchema impl = sInterface.nullableDefaultImplStruct();
            StructSchema subImpl = subInterface.nullableDefaultImplStruct();
            if (impl == null) {
                errs.addErr(new InterfaceCellEmptyButHasNoDefaultImpl(parsed.get(0), sInterface.name()));
                return null;
            }
            require(subImpl != null);

            // 之后按pack为true来处理，因为反正parsed里都是单个cell，并且里面value为空
            vImpl = parseStructural(subImpl, parsed, impl, true, true, curRowIndex);

        } else {
            StructSchema impl;
            StructSchema subImpl;
            List<DCell> implCells;
            if (isNumberOrBool) {
                impl = sInterface.nullableDefaultImplStruct();
                subImpl = subInterface.nullableDefaultImplStruct();
                require(subImpl != null);
                implCells = parsed;

            } else {
                String implName = parsed.get(0).value();
                if (!implName.isEmpty()) {
                    impl = sInterface.findImpl(implName);
                    subImpl = subInterface.findImpl(implName);
                    if (impl == null) {
                        errs.addErr(new InterfaceCellImplNotFound(parsed.get(0), sInterface.name(), implName));
                        return null;
                    }
                } else {
                    impl = sInterface.nullableDefaultImplStruct();
                    subImpl = subInterface.nullableDefaultImplStruct();
                    if (impl == null) {
                        errs.addErr(new InterfaceCellEmptyButHasNoDefaultImpl(parsed.get(0), sInterface.name()));
                        return null;
                    }
                }

                require(subImpl != null);
                int expected = isPack ? 1 : Spans.span(impl);
                if (parsed.size() - 1 < expected) {
                    errs.addErr(new InterfaceCellImplSpanNotEnough(parsed, sInterface.name(), impl.name(),
                            expected, parsed.size() - 1));
                    return null;
                }
                implCells = parsed.subList(1, expected + 1);
            }
            vImpl = parseStructural(subImpl, implCells, impl, isPack, canBeEmpty, curRowIndex);
        }

        return new VInterface(subInterface, vImpl);
    }

    VStruct parseStructural(Structural subStructural, List<DCell> cells, Structural structural,
                            boolean pack, boolean canBeEmpty, int curRowIndex) {
        List<DCell> parsed = cells;
        boolean isEmpty = false; // 支持excel里的cell为空，并且它还是个复合结构；但不支持非empty的cell里部分结构为空
        boolean isPack = pack || structural.fmt() == PACK;
        if (isPack) {
            require(cells.size() == 1, "pack应该只占一格");
            DCell cell = cells.get(0);
            if (canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else {
                parsed = DCells.parseNestList(cell);
                canBeEmpty = false;
            }

        } else if (structural.fmt() instanceof Sep sep) {
            require(cells.size() == 1, "sep应该只占一格");
            DCell cell = cells.get(0);
            if (canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else {
                parsed = DCells.parseList(cell, sep.sep());
                canBeEmpty = false;
            }
        } else {
            require(cells.size() == Spans.span(structural), "列宽度应一致");
            canBeEmpty = false;
        }

        List<CfgValue.Value> values = new ArrayList<>(subStructural.fields().size());
        if (isEmpty) {
            for (FieldSchema subField : subStructural.fields()) {
                FieldSchema field = structural.findField(subField.name());
                require(field != null);
                CfgValue.Value v = parseType(subField.type(), parsed, field.type(), field.fmt(),
                        true, true, curRowIndex,
                        structural.name(), field.name());
                values.add(v);
            }

        } else {
            int startIdx = 0;
            for (FieldSchema field : structural.fields()) {
                int expected = isPack ? 1 : Spans.span(field);
                FieldSchema subField = subStructural.findField(field.name());
                if (subField != null) {
                    // 提取单个field
                    if (parsed.size() < startIdx + expected) {
                        errs.addErr(new FieldCellSpanNotEnough(parsed, structural.name(), field.name(),
                                expected, parsed.size() - startIdx));
                        return null;
                    }

                    CfgValue.Value v = parseType(subField.type(), parsed, field.type(), field.fmt(),
                            isPack || field.fmt() == PACK, canBeEmpty, curRowIndex,
                            structural.name(), field.name());
                    values.add(v);
                }

                startIdx += expected;
            }
        }

        return new VStruct(subStructural, values);
    }


    CfgValue.Value parseType(FieldType subType, List<DCell> cells, FieldType type, FieldFormat fmt,
                             boolean pack, boolean canBeEmpty, int curRowIndex,
                             String nameable, String field) {

        switch (type) {
            case Primitive primitive -> {
                require(cells.size() == 1);
                DCell cell = cells.get(0);
                String str = cell.value().trim();
                switch (primitive) {
                    case BOOL -> {
                        boolean v = str.equals("1") || str.equalsIgnoreCase("true");
                        if (!boolStrSet.contains(str)) {
                            errs.addErr(new NotMatchFieldType(cell, nameable, field, type));
                        }
                        return new CfgValue.VBool(v, cell);
                    }
                    case INT -> {
                        int v = 0;
                        try {
                            v = Integer.decode(str);
                        } catch (Exception e) {
                            errs.addErr(new NotMatchFieldType(cell, nameable, field, type));
                        }
                        return new CfgValue.VInt(v, cell);
                    }
                    case LONG -> {
                        long v = 0;
                        try {
                            v = Long.decode(str);
                        } catch (Exception e) {
                            errs.addErr(new NotMatchFieldType(cell, nameable, field, type));
                        }
                        return new CfgValue.VLong(v, cell);
                    }
                    case FLOAT -> {
                        float v = 0;
                        try {
                            v = Float.parseFloat(str);
                        } catch (Exception e) {
                            errs.addErr(new NotMatchFieldType(cell, nameable, field, type));
                        }
                        return new CfgValue.VFloat(v, cell);
                    }
                    case STRING -> {
                        return new CfgValue.VString(str, cell);
                    }
                    case TEXT -> {
                        return new CfgValue.VText(str, cell);
                    }
                }
            }

            case StructRef structRef -> {
                switch (structRef.obj()) {
                    case InterfaceSchema sInterface -> {
                        InterfaceSchema subInterface = (InterfaceSchema) (((StructRef) subType).obj());

                        return parseInterface(subInterface, cells, sInterface,
                                pack, canBeEmpty, curRowIndex);
                    }
                    case StructSchema struct -> {
                        StructSchema subStruct = (StructSchema) (((StructRef) subType).obj());
                        return parseStructural(subStruct, cells, struct,
                                pack, canBeEmpty, curRowIndex);
                    }
                }
            }
            case FList fList -> {
                return parseList((FList) subType, cells, fList, fmt, pack, curRowIndex, nameable, field);
            }

            case FMap fMap -> {
                return parseMap((FMap) subType, cells, fMap, fmt, pack, curRowIndex, nameable, field);
            }
        }
        return null;
    }

    CfgValue.Value parseMap(FMap subType, List<DCell> cells, FMap type, FieldFormat fmt,
                            boolean pack, int curRowIndex,
                            String nameable, String field) {
        List<DCell> parsed = null;
        List<CellsWithRowIndex> blocks = null;
        boolean isPack = pack || fmt == PACK;
        if (isPack) {
            require(cells.size() == 1);
            DCell cell = cells.get(0);
            parsed = DCells.parseNestList(cell);

        } else if (fmt instanceof Block _) {
//            parsed = new ArrayList<>();
            blocks = parseBlock(cells, curRowIndex);
//            for (CellsWithRowIndex block : blocks) {
//                parsed.addAll(block.cells);
//            }

        } else {
            require(cells.size() == Spans.span(type, fmt));
            parsed = cells;
        }

        if (blocks == null) {
            blocks = List.of(new CellsWithRowIndex(parsed, curRowIndex));
        }


        Map<CfgValue.Value, CfgValue.Value> valueMap = new LinkedHashMap<>();

        int kc = isPack ? 1 : Spans.span(type.key());
        int vc = isPack ? 1 : Spans.span(type.value());
        int itemSpan = kc + vc;

        for (CellsWithRowIndex block : blocks) {
            List<DCell> curLineParsed = block.cells;
            for (int startIdx = 0; startIdx < curLineParsed.size(); startIdx += itemSpan) {
                if (startIdx + itemSpan > curLineParsed.size()) {
                    errs.addErr(new FieldCellSpanNotEnough(curLineParsed.subList(startIdx, curLineParsed.size()),
                            nameable, field, itemSpan, curLineParsed.size() - startIdx));
                    continue;
                }
                List<DCell> keyCells = curLineParsed.subList(startIdx, startIdx + kc);
                List<DCell> valueCells = curLineParsed.subList(startIdx + kc, startIdx + itemSpan);

                //第一个单元作为是否有item的标记
                if (!keyCells.get(0).isCellEmpty()) {
                    CfgValue.Value key = parseType(subType.key(), keyCells, type.key(), fmt,
                            isPack, false, block.rowIndex,
                            nameable, field);
                    CfgValue.Value value = parseType(subType.value(), valueCells, type.value(), fmt,
                            isPack, false, block.rowIndex,
                            nameable, field);

                    CfgValue.Value old = valueMap.put(key, value);
                    if (old == null) {
                        errs.addErr(new MapKeyDuplicated(keyCells, nameable, field));
                    }

                } else {
                    List<DCell> itemCells = curLineParsed.subList(startIdx, startIdx + itemSpan);
                    if (itemCells.stream().anyMatch(c -> !c.isCellEmpty())) {
                        errs.addErr(new ContainerItemPartialSet(itemCells, nameable, field));
                    }
                }
            }
        }

        return new CfgValue.VMap(valueMap);
    }


    CfgValue.VList parseList(FList subType, List<DCell> cells, FList type, FieldFormat fmt,
                             boolean pack, int curRowIndex,
                             String nameable, String field) {

        List<DCell> parsed = null;
        List<CellsWithRowIndex> blocks = null;
        boolean isPack = pack || fmt == PACK;
        if (isPack) {
            require(cells.size() == 1);
            DCell cell = cells.get(0);
            parsed = DCells.parseNestList(cell);

        } else if (fmt instanceof Block _) {
//            parsed = new ArrayList<>();
            blocks = parseBlock(cells, curRowIndex);
//            for (CellsWithRowIndex block : blocks) {
//                parsed.addAll(block.cells);
//            }

        } else if (fmt instanceof Sep sep) {
            require(cells.size() == 1);
            DCell cell = cells.get(0);
            parsed = DCells.parseList(cell, sep.sep());

        } else {
            require(cells.size() == Spans.span(type, fmt));
            parsed = cells;
        }

        if (blocks == null) {
            blocks = List.of(new CellsWithRowIndex(parsed, curRowIndex));
        }

        List<CfgValue.Value> valueList = new ArrayList<>();
        int itemSpan = isPack ? 1 : Spans.span(type.item());
        for (CellsWithRowIndex block : blocks) {
            List<DCell> curLineParsed = block.cells;
            for (int startIdx = 0; startIdx < curLineParsed.size(); startIdx += itemSpan) {
                if (startIdx + itemSpan > curLineParsed.size()) {
                    errs.addErr(new FieldCellSpanNotEnough(curLineParsed.subList(startIdx, curLineParsed.size()),
                            nameable, field, itemSpan, curLineParsed.size() - startIdx));
                    continue;
                }
                List<DCell> itemCells = curLineParsed.subList(startIdx, startIdx + itemSpan);
                //第一个单元作为是否有item的标记
                if (!itemCells.get(0).isCellEmpty()) {
                    CfgValue.Value value = parseType(subType.item(), itemCells, type.item(), fmt,
                            isPack, false, block.rowIndex,
                            nameable, field);
                    valueList.add(value);
                } else {
                    if (itemCells.stream().anyMatch(c -> !c.isCellEmpty())) {
                        errs.addErr(new ContainerItemPartialSet(itemCells, nameable, field));
                    }
                }
            }
        }
        return new CfgValue.VList(valueList);

    }

    // 要允许block<bean>,bean里仍然有block，如下所示
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
    List<CellsWithRowIndex> parseBlock(List<DCell> cells, int curRowIndex) {
        DCell firstCell = cells.get(0);
        int rowSize = dTable.rows().size();
        int firstColIndex = getColumnIndex(firstCell);

        int colSize = cells.size();

        List<CellsWithRowIndex> res = new ArrayList<>();
        res.add(new CellsWithRowIndex(cells, curRowIndex));

        for (int row = curRowIndex + 1; row < rowSize; row++) {
            List<DCell> line = dTable.rows().get(row);

            // 属于上一个record的block
            if (line.get(0).isCellEmpty()) {
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

    record CellsWithRowIndex(List<DCell> cells,
                             int rowIndex) {
    }

    private int getColumnIndex(DCell cell) {
        int i = 0;
        for (DCell c : curRow) {
            if (c.col() == cell.col()) {
                return i;
            }
            i++;
        }
        return i;
    }


    private static final Set<String> boolStrSet = Set.of("false", "true", "False", "True", "1", "0", "");

    private void require(boolean cond) {
        if (!cond)
            throw new AssertionError("");
    }

    private void require(boolean cond, String err) {
        if (!cond)
            throw new AssertionError(err);
    }


}

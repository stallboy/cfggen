package configgen.write;

import configgen.schema.*;
import configgen.schema.FieldFormat.Sep;
import configgen.schema.FieldType.FMap;
import configgen.schema.FieldType.SimpleType;
import configgen.value.CfgValue.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;

/**
 * VStruct到RecordBlock的映射引擎
 * 基于mapping.md中的5种映射规则：auto、pack、sep、fix、block
 */
public class RecordBlockMapper {
    private final VStruct record;
    private final RecordBlock block;

    public static RecordBlock mapToBlock(VStruct record) {
        RecordBlockMapper mapper = new RecordBlockMapper(record);
        mapper.map();
        return mapper.block;
    }

    public RecordBlockMapper(VStruct record) {
        this.record = record;
        this.block = new RecordBlock(Span.span(record.schema()));
    }

    public void map() {
        mapStructural_auto(record, 0, 0);
    }

    /**
     * @param nextCol 下一列index
     * @param useRow 使用的行数
     */
    private record NextColAndUseRow(int nextCol,
                                    int useRow) {
    }

    /**
     * 映射auto类型struct或table的数据到block里
     */
    private NextColAndUseRow mapStructural_auto(VStruct vStruct, int startRow, int startCol) {
        int idx = 0;
        int nextCol = startCol;
        int maxUseRow = 1;
        for (FieldSchema field : vStruct.schema().fields()) {
            Value value = vStruct.values().get(idx);

            FieldFormat fmt = field.fmt();
            switch (fmt) {
                case PACK -> {
                    block.setCell(startRow, nextCol, value.packStr());
                    nextCol++;
                }
                case Sep ignored -> {
                    if (!(value instanceof VList vList)) {
                        throw new IllegalArgumentException("Value is not VList for Sep format");
                    }
                    String sepStr = ValueToSepStr.toSepStr(vList, field);

                    block.setCell(startRow, nextCol, sepStr);
                    nextCol++;
                }

                default -> {
                    switch (value) {
                        case PrimitiveValue pv -> {
                            block.setCell(startRow, nextCol, pv.toStr());
                            nextCol++;
                        }

                        case VStruct subVStruct -> { // field auto
                            NextColAndUseRow nu = mapStruct(subVStruct, startRow, nextCol);
                            nextCol = nu.nextCol;
                            if (nu.useRow > maxUseRow) {
                                maxUseRow = nu.useRow;
                            }
                        }

                        case VInterface vInterface -> { // field auto
                            NextColAndUseRow nu = mapInterface(vInterface, startRow, nextCol);
                            nextCol = nu.nextCol;
                            if (nu.useRow > maxUseRow) {
                                maxUseRow = nu.useRow;
                            }
                        }

                        case VList vList -> { // fix ，block
                            NextColAndUseRow nu = mapList_listOrBlock(vList, field, startRow, nextCol);
                            nextCol = nu.nextCol;
                            if (nu.useRow > maxUseRow) {
                                maxUseRow = nu.useRow;
                            }

                        }
                        case VMap vMap -> { // fix，block
                            NextColAndUseRow nu = mapMap_listOrBlock(vMap, field, startRow, nextCol);
                            nextCol = nu.nextCol;
                            if (nu.useRow > maxUseRow) {
                                maxUseRow = nu.useRow;
                            }
                        }
                    }

                }
            }
            idx++;
        }

        return new NextColAndUseRow(nextCol, maxUseRow);

    }


    /**
     * 映射struct数据到block里
     */
    private NextColAndUseRow mapStruct(VStruct vStruct, int startRow, int startCol) {
        Structural schema = vStruct.schema();
        int nextCol = startCol;
        int maxUseRow = 1;
        switch (schema.fmt()) {
            case PACK -> {
                block.setCell(startRow, nextCol, vStruct.packStr());
                nextCol++;
            }
            case Sep ignored -> {
                String sepStr = ValueToSepStr.toSepStr(vStruct);
                block.setCell(startRow, nextCol, sepStr);
                nextCol++;
            }
            case AUTO -> {
                NextColAndUseRow nu = mapStructural_auto(vStruct, startRow, nextCol);
                nextCol = nu.nextCol;
                if (nu.useRow > maxUseRow) {
                    maxUseRow = nu.useRow;
                }
            }
            default -> {
                throw new IllegalArgumentException("SHOULD NOT HAPPEN, Unsupported struct format: " + schema.fmt());
            }
        }

        return new NextColAndUseRow(nextCol, maxUseRow);
    }

    /**
     * 映射interface数据到block里
     */
    private NextColAndUseRow mapInterface(VInterface vInterface, int startRow, int startCol) {
        int nextCol = startCol;
        int maxUseRow = 1;
        switch (vInterface.schema().fmt()) {
            case PACK -> {
                block.setCell(startRow, nextCol, vInterface.packStr());
                nextCol++;
            }
            case AUTO -> {
                VStruct child = vInterface.child();
                block.setCell(startRow, nextCol, child.name());
                nextCol++;
                NextColAndUseRow nu = mapStruct(child, startRow, nextCol);
                nextCol = nu.nextCol;
                if (nu.useRow > maxUseRow) {
                    maxUseRow = nu.useRow;
                }

            }
            default -> {
                throw new IllegalArgumentException("SHOULD NOT HAPPEN, Unsupported interface format: "
                        + vInterface.schema().fmt());
            }
        }

        return new NextColAndUseRow(nextCol, maxUseRow);
    }

    /**
     * 映射fix或block规则的list数据到block里
     */
    private NextColAndUseRow mapList_listOrBlock(VList vList, FieldSchema field, int startRow, int startCol) {
        FixAndBlock fb = parseFixOrBlock(field, "VList");

        if (!(field.type() instanceof FieldType.FList(SimpleType item))) {
            throw new IllegalArgumentException("SHOULD NOT HAPPEN, FieldType is not FList for VList");
        }


        List<SimpleValue> values = vList.valueList();
        if (!fb.isBlock && values.size() > fb.fix) {
            throw new IllegalArgumentException("VList size exceeds fixed length, size=" + values.size() + ", fix=" + fb.fix);
        }

        int elemSpan = Span.simpleTypeSpan(item);
        return mapElements_listOrBlock(values.size(), fb.fix, elemSpan, startRow, startCol,
                (elemIdx, row, col) -> mapSimpleValue(values.get(elemIdx), row, col));
    }

    /**
     * @return useRow，使用的行数
     */
    private int mapSimpleValue(SimpleValue value, int row, int col) {
        switch (value) {
            case PrimitiveValue pv -> {
                block.setCell(row, col, pv.toStr());
                return 1;
            }
            case VStruct vStruct -> {
                NextColAndUseRow nu = mapStruct(vStruct, row, col);
                return nu.useRow;
            }
            case VInterface vInterface -> {
                NextColAndUseRow nu = mapInterface(vInterface, row, col);
                return nu.useRow;
            }
        }
    }

    private NextColAndUseRow mapMap_listOrBlock(VMap vMap, FieldSchema field, int startRow, int startCol) {
        FixAndBlock fb = parseFixOrBlock(field, "VMap");

        if (!(field.type() instanceof FMap(SimpleType key, SimpleType value))) {
            throw new IllegalArgumentException("SHOULD NOT HAPPEN, FieldType is not FMap for VMap");
        }


        Map<SimpleValue, SimpleValue> map = vMap.valueMap();
        if (!fb.isBlock && map.size() > fb.fix) {
            throw new IllegalArgumentException("VMap size exceeds fixed length, size=" + map.size() + ", fix=" + fb.fix);
        }
        var entries = new ArrayList<>(map.entrySet());
        int keySpan = Span.simpleTypeSpan(key);
        int valueSpan = Span.simpleTypeSpan(value);
        int elemSpan = keySpan + valueSpan;

        return mapElements_listOrBlock(entries.size(), fb.fix, elemSpan, startRow, startCol,
                (elemIdx, row, col) -> {
                    var entry = entries.get(elemIdx);
                    int useRow1 = mapSimpleValue(entry.getKey(), row, col);
                    int useRow2 = mapSimpleValue(entry.getValue(), row, col + keySpan);
                    return Math.max(useRow1, useRow2);
                });
    }

    /**
     * fix或block规则的fmt解析
     */
    private record FixAndBlock(int fix, boolean isBlock) {
    }

    private static FixAndBlock parseFixOrBlock(FieldSchema field, String valueKind) {
        return switch (field.fmt()) {
            case FieldFormat.Fix(int count) -> new FixAndBlock(count, false);
            case FieldFormat.Block(int count) -> new FixAndBlock(count, true);
            default -> throw new IllegalArgumentException(
                    "SHOULD NOT HAPPEN, FieldFormat is not Fix or Block for " + valueKind);
        };
    }

    /**
     * 映射一个逻辑行内的一个元素
     *
     * @return useRow，使用的行数
     */
    @FunctionalInterface
    private interface ElementMapper {
        int map(int elemIdx, int row, int col);
    }

    /**
     * fix或block规则的行列循环骨架：按逻辑行x逻辑列遍历元素，逐个映射到block里
     */
    private NextColAndUseRow mapElements_listOrBlock(int elemCount, int fix, int elemSpan,
                                                     int startRow, int startCol,
                                                     ElementMapper mapper) {
        int logicRowCount = (elemCount + fix - 1) / fix;
        int nextRow = startRow;

        for (int logicRowIdx = 0; logicRowIdx < logicRowCount; logicRowIdx++) {
            int maxUseRow = 1;

            for (int logicColIdx = 0; logicColIdx < fix; logicColIdx++) {
                int elemIdx = logicRowIdx * fix + logicColIdx;
                if (elemIdx >= elemCount) {
                    break;
                }
                int col = startCol + logicColIdx * elemSpan;
                int useRow = mapper.map(elemIdx, nextRow, col);

                if (useRow > maxUseRow) {
                    maxUseRow = useRow;
                }
            }

            nextRow += maxUseRow;
        }

        return new NextColAndUseRow(startCol + fix * elemSpan,
                Math.max(1, nextRow - startRow));
    }

}
package configgen.write;

import configgen.schema.*;
import configgen.schema.FieldFormat.Sep;
import configgen.schema.FieldType.FMap;
import configgen.schema.FieldType.SimpleType;
import configgen.value.CfgValue.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;

/**
 * VStruct到RecordBlock的映射引擎
 * 基于mapping.md中的5种映射规则：auto、pack、sep、fix、block
 */
public record RecordBlockMapper(@NotNull VStruct record,
                                @NotNull RecordBlock block) {

    public static RecordBlockMapper of(VStruct record) {
        return new RecordBlockMapper(record, new RecordBlock(Span.span(record.schema())));
    }

    public void map() {
        mapStructural_auto(record, 0, 0);
    }

    private int mapStructural_auto(VStruct vStruct, int startRow, int startCol) {
        int idx = 0;
        int nextCol = startCol;
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
                            nextCol = mapStructural(subVStruct, startRow, nextCol);
                        }

                        case VInterface vInterface -> { // field auto
                            nextCol = mapInterface(vInterface, startRow, nextCol);
                        }
                        case VList vList -> { // fix ，block
                            nextCol = mapList_listOrBlock(vList, field, startRow, nextCol);
                        }
                        case VMap vMap -> { // fix，block
                            nextCol = mapMap_listOrBlock(vMap, field, startRow, nextCol);
                        }
                    }

                }
            }
            idx++;
        }

        return nextCol;

    }


    private int mapStructural(VStruct vStruct, int startRow, int startCol) {
        Structural schema = vStruct.schema();
        int nextCol = startCol;
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
                nextCol = mapStructural_auto(vStruct, startRow, nextCol);
            }
            default -> {
                throw new IllegalArgumentException("SHOULD NOT HAPPEN, Unsupported Structural Format in mapStructural: " + schema.fmt());
            }
        }

        return nextCol;
    }

    private int mapInterface(VInterface vInterface, int startRow, int startCol) {
        InterfaceSchema schema = vInterface.schema();
        int nextCol = startCol;
        switch (schema.fmt()) {
            case PACK -> {
                block.setCell(startRow, nextCol, vInterface.packStr());
                nextCol++;
            }
            case AUTO -> {
                VStruct child = vInterface.child();
                block.setCell(startRow, nextCol, child.name());
                nextCol++;
                nextCol = mapStructural(child, startRow, nextCol);
            }
            default -> {
                throw new IllegalArgumentException("SHOULD NOT HAPPEN, Unsupported Structural Format in mapStructural: " + schema.fmt());
            }
        }

        return nextCol;
    }

    private int mapList_listOrBlock(VList vList, FieldSchema field, int startRow, int startCol) {
        FieldFormat fmt = field.fmt();
        boolean isBlock;
        int len;
        switch (fmt) {
            case FieldFormat.Fix fix -> {
                len = fix.count();
                isBlock = false;
            }
            case FieldFormat.Block b -> {
                len = b.fix();
                isBlock = true;
            }
            default -> {
                throw new IllegalArgumentException("SHOULD NOT HAPPEN, FieldFormat is not Fix or Block for VList");
            }
        }

        if (!(field.type() instanceof FieldType.FList(SimpleType item))) {
            throw new IllegalArgumentException("SHOULD NOT HAPPEN, FieldType is not FList for VList");
        }


        List<SimpleValue> values = vList.valueList();
        if (!isBlock && values.size() > len) {
            throw new IllegalArgumentException("VList size exceeds fixed length, size=" + values.size() + ", fixedLen=" + len);
        }

        int elemSpan = Span.simpleTypeSpan(item);
        int idx = 0;
        for (SimpleValue elemValue : values) {
            int rowOffset = idx / len;
            int colOffset = (idx % len) * elemSpan;
            int row = startRow + rowOffset;
            int col = startCol + colOffset;

            mapSimpleValue(elemValue, row, col);
            idx++;
        }

        return startCol + len * elemSpan;
    }

    private void mapSimpleValue(SimpleValue value, int row, int col) {
        switch (value) {
            case PrimitiveValue pv -> {
                block.setCell(row, col, pv.toStr());
            }
            case VStruct vStruct -> {
                mapStructural(vStruct, row, col);
            }
            case VInterface vInterface -> {
                mapInterface(vInterface, row, col);
            }
        }
    }

    private int mapMap_listOrBlock(VMap vMap, FieldSchema field, int startRow, int startCol) {
        FieldFormat fmt = field.fmt();
        boolean isBlock;
        int len;
        switch (fmt) {
            case FieldFormat.Fix fix -> {
                len = fix.count();
                isBlock = false;
            }
            case FieldFormat.Block b -> {
                len = b.fix();
                isBlock = true;
            }
            default -> {
                throw new IllegalArgumentException("SHOULD NOT HAPPEN, FieldFormat is not Fix or Block for VMap");
            }
        }

        if (!(field.type() instanceof FMap(SimpleType key, SimpleType value))) {
            throw new IllegalArgumentException("SHOULD NOT HAPPEN, FieldType is not FMap for VMap");
        }


        Map<SimpleValue, SimpleValue> map = vMap.valueMap();
        if (!isBlock && map.size() > len) {
            throw new IllegalArgumentException("VMap size exceeds fixed length, size=" + map.size() + ", fixedLen=" + len);
        }

        int keySpan = Span.simpleTypeSpan(key);
        int valueSpan = Span.simpleTypeSpan(value);
        int elemSpan = keySpan + valueSpan;

        int idx = 0;
        for (Map.Entry<SimpleValue, SimpleValue> e : map.entrySet()) {
            int rowOffset = idx / len;
            int colOffset = (idx % len) * elemSpan;
            int row = startRow + rowOffset;
            int col = startCol + colOffset;

            mapSimpleValue(e.getKey(), row, col);
            mapSimpleValue(e.getValue(), row, col + keySpan);

            idx++;
        }

        return startCol + len * elemSpan;
    }

}
package configgen.value;

import configgen.schema.*;

import java.util.*;

import static configgen.data.CfgData.DCell;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.value.CfgValue.*;

public class ValueParser {

    public record CellsWithRowIndex(List<DCell> cells,
                                    int rowIndex) {
    }

    public interface BlockParser {
        List<CellsWithRowIndex> parseBlock(List<DCell> cells, int curRowIndex);

        BlockParser dummy = (cells, curRowIndex) -> List.of(new CellsWithRowIndex(cells, curRowIndex));
    }


    private final ValueErrs errs;
    private final TextI18n.TableI18n nullableTableI18n;
    private final BlockParser blockParser;

    public ValueParser(ValueErrs errs, TextI18n.TableI18n nullableTableI18n, BlockParser blockParser) {
        this.errs = errs;
        this.nullableTableI18n = nullableTableI18n;
        this.blockParser = blockParser;
    }


    VInterface parseInterface(InterfaceSchema subInterface, List<DCell> cells, InterfaceSchema sInterface,
                              boolean pack, boolean canBeEmpty, int curRowIndex) {
        List<DCell> parsed = cells;

        boolean isEmpty = false; // 支持excel里的cell为空，并且它还是个复合结构；但不支持非empty的cell里部分结构为空
        boolean isNumberOrBool = false;
        boolean canChildBeEmpty = canBeEmpty;
        boolean isPack = pack || sInterface.fmt() == PACK;
        if (isPack) {
            require(cells.size() == 1, "pack应该只占一格");
            DCell cell = cells.getFirst();
            if (canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else if (sInterface.canBeNumberOrBool()) {
                if (DCells.isFunc(cell)) {
                    try {
                        parsed = DCells.parseFunc(cell);
                    } catch (Exception e) {
                        errs.addErr(new ValueErrs.ParsePackErr(cell, sInterface.name(), e.getMessage()));
                        return null;
                    }
                    canChildBeEmpty = false; // 只要是parse了结构的，内部就不允许为空了
                } else {
                    isNumberOrBool = true;
                }
            } else {
                try {
                    parsed = DCells.parseFunc(cell);
                } catch (Exception e) {
                    errs.addErr(new ValueErrs.ParsePackErr(cell, sInterface.name(), e.getMessage()));
                    return null;
                }
                canChildBeEmpty = false;
            }

        } else if (sInterface.fmt() instanceof FieldFormat.Sep sep) {
            require(cells.size() == 1, "sep应该只占一格");
            DCell cell = cells.getFirst();
            if (canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else {
                parsed = DCells.parseList(cell, sep.sep());
                canChildBeEmpty = false;
            }
        } else {
            require(cells.size() == Spans.span(sInterface), "列宽度应一致");
        }

        // 内容为空的单一格子，处理方式是把这空的parsed一层层传下去
        VStruct vImpl;
        if (isEmpty) {
            StructSchema impl = sInterface.nullableDefaultImplStruct();
            StructSchema subImpl = subInterface.nullableDefaultImplStruct();
            if (impl == null) {
                errs.addErr(new ValueErrs.InterfaceCellEmptyButHasNoDefaultImpl(parsed.getFirst(), sInterface.name()));
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
                String implName = parsed.getFirst().value();
                if (!implName.isEmpty()) {
                    impl = sInterface.findImpl(implName);
                    subImpl = subInterface.findImpl(implName);
                    if (impl == null) {
                        errs.addErr(new ValueErrs.InterfaceCellImplNotFound(parsed.getFirst(), sInterface.name(), implName));
                        return null;
                    }
                } else {
                    impl = sInterface.nullableDefaultImplStruct();
                    subImpl = subInterface.nullableDefaultImplStruct();
                    if (impl == null) {
                        errs.addErr(new ValueErrs.InterfaceCellEmptyButHasNoDefaultImpl(parsed.getFirst(), sInterface.name()));
                        return null;
                    }
                }

                require(subImpl != null);
                int expected = isPack ? 1 : Spans.span(impl);
                if (parsed.size() - 1 < expected) {
                    errs.addErr(new ValueErrs.InterfaceCellImplSpanNotEnough(parsed, sInterface.name(), impl.name(),
                            expected, parsed.size() - 1));
                    return null;
                }
                implCells = parsed.subList(1, expected + 1);
            }
            vImpl = parseStructural(subImpl, implCells, impl, isPack, canChildBeEmpty, curRowIndex);
        }

        if (vImpl == null) {
            return null;
        }

        return new VInterface(subInterface, vImpl, cells);
    }

    VStruct parseStructural(Structural subStructural, List<DCell> cells, Structural structural,
                            boolean pack, boolean canBeEmpty, int curRowIndex) {
        List<DCell> parsed = cells;
        boolean isEmpty = false; // 支持excel里的cell为空，并且它还是个复合结构；但不支持非empty的cell里部分结构为空
        boolean canChildBeEmpty = canBeEmpty;
        boolean isPack = pack || structural.fmt() == PACK;
        if (isPack) {
            require(cells.size() == 1, "pack应该只占一格");
            DCell cell = cells.getFirst();
            if (canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else {
                try {
                    parsed = DCells.parseNestList(cell);
                } catch (Exception e) {
                    errs.addErr(new ValueErrs.ParsePackErr(cell, structural.name(), e.getMessage()));
                    return null;
                }
                canChildBeEmpty = false;
            }

        } else if (structural.fmt() instanceof FieldFormat.Sep sep) {
            require(cells.size() == 1, "sep应该只占一格");
            DCell cell = cells.getFirst();
            if (canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else {
                parsed = DCells.parseList(cell, sep.sep());
                canChildBeEmpty = false;
            }
        } else {
            require(cells.size() == Spans.span(structural), "列宽度应一致");
        }

        List<Value> values = new ArrayList<>(subStructural.fields().size());
        if (isEmpty) {
            for (FieldSchema subField : subStructural.fields()) {
                FieldSchema field = structural.findField(subField.name());
                require(field != null);
                Value v = parseField(subField, parsed, field,
                        true, true, curRowIndex, structural.name());
                if (v != null) {
                    values.add(v);
                } else {
                    return null;
                }
            }

        } else {
            int startIdx = 0;
            for (FieldSchema field : structural.fields()) {
                int expected = isPack ? 1 : Spans.span(field);
                FieldSchema subField = subStructural.findField(field.name());
                if (subField != null) {
                    // 提取单个field
                    if (parsed.size() < startIdx + expected) {
                        errs.addErr(new ValueErrs.FieldCellSpanNotEnough(parsed, structural.name(), field.name(),
                                expected, parsed.size() - startIdx));
                        return null;
                    }

                    List<DCell> fieldCells = parsed.subList(startIdx, startIdx + expected);
                    Value v = parseField(subField, fieldCells, field,
                            isPack || field.fmt() == PACK, canChildBeEmpty, curRowIndex, structural.name());
                    if (v != null) {
                        values.add(v);
                    } else {
                        return null;
                    }
                }

                startIdx += expected;
            }
        }

        return new VStruct(subStructural, values, cells);
    }

    SimpleValue parseSimpleType(FieldType.SimpleType subType, List<DCell> cells, FieldType.SimpleType type,
                                boolean pack, boolean canBeEmpty, int curRowIndex,
                                String nameable, String field) {
        switch (type) {
            case FieldType.Primitive primitive -> {
                require(cells.size() == 1);
                DCell cell = cells.getFirst();
                String str = cell.value().trim();
                switch (primitive) {
                    case BOOL -> {
                        boolean v = str.equals("1") || str.equalsIgnoreCase("true");
                        if (!boolStrSet.contains(str.toLowerCase())) {
                            errs.addErr(new ValueErrs.NotMatchFieldType(cell, nameable, field, type));
                        }
                        return new VBool(v, cell);
                    }
                    case INT -> {
                        int v = 0;
                        try {
                            v = str.isEmpty() ? 0 : Integer.decode(str);
                        } catch (Exception e) {
                            errs.addErr(new ValueErrs.NotMatchFieldType(cell, nameable, field, type));
                        }
                        return new VInt(v, cell);
                    }
                    case LONG -> {
                        long v = 0;
                        try {
                            v = str.isEmpty() ? 0 : Long.decode(str);
                        } catch (Exception e) {
                            errs.addErr(new ValueErrs.NotMatchFieldType(cell, nameable, field, type));
                        }
                        return new VLong(v, cell);
                    }
                    case FLOAT -> {
                        float v = 0f;
                        try {
                            v = str.isEmpty() ? 0f : Float.parseFloat(str);
                        } catch (Exception e) {
                            errs.addErr(new ValueErrs.NotMatchFieldType(cell, nameable, field, type));
                        }
                        return new VFloat(v, cell);
                    }
                    case STRING -> {
                        return new VString(str, cell);
                    }
                    case TEXT -> {
                        String value;
                        if (nullableTableI18n != null) {
                            value = nullableTableI18n.findText(str);
                            if (value == null) {
                                value = str;
                            }
                        } else {
                            value = str;
                        }
                        return new VText(value, str, cell);
                    }
                }
            }

            case FieldType.StructRef structRef -> {
                switch (structRef.obj()) {
                    case InterfaceSchema sInterface -> {
                        InterfaceSchema subInterface = (InterfaceSchema) (((FieldType.StructRef) subType).obj());

                        return parseInterface(subInterface, cells, sInterface,
                                pack, canBeEmpty, curRowIndex);
                    }
                    case StructSchema struct -> {
                        StructSchema subStruct = (StructSchema) (((FieldType.StructRef) subType).obj());
                        return parseStructural(subStruct, cells, struct,
                                pack, canBeEmpty, curRowIndex);
                    }
                }
            }
        }
        return null;
    }

    Value parseField(FieldSchema subField, List<DCell> cells, FieldSchema field,
                     boolean pack, boolean canBeEmpty, int curRowIndex,
                     String nameable) {

        switch (field.type()) {
            case FieldType.SimpleType simple -> {
                return parseSimpleType((FieldType.SimpleType) subField.type(), cells, simple, pack, canBeEmpty, curRowIndex, nameable, field.name());
            }
            case FieldType.FList ignored -> {
                return parseList(subField, cells, field, pack, curRowIndex, nameable);
            }

            case FieldType.FMap ignored -> {
                return parseMap(subField, cells, field, pack, curRowIndex, nameable);
            }
        }
    }

    VMap parseMap(FieldSchema subField, List<DCell> cells, FieldSchema field,
                  boolean isPack, int curRowIndex,
                  String nameable) {

        FieldType.FMap subType = (FieldType.FMap) subField.type();
        FieldType.FMap type = (FieldType.FMap) field.type();

        List<DCell> parsed = null;
        List<CellsWithRowIndex> blocks = null;
        if (isPack) {
            require(cells.size() == 1);
            DCell cell = cells.getFirst();
            try {
                parsed = DCells.parseNestList(cell);
            } catch (Exception e) {
                errs.addErr(new ValueErrs.ParsePackErr(cell, type.toString(), e.getMessage()));
                return null;
            }

        } else if (field.fmt() instanceof FieldFormat.Block ignored) {
            blocks = blockParser.parseBlock(cells, curRowIndex);

        } else {
            require(cells.size() == Spans.span(field));
            parsed = cells;
        }

        if (blocks == null) {
            blocks = List.of(new CellsWithRowIndex(parsed, curRowIndex));
        }

        Map<SimpleValue, SimpleValue> valueMap = new LinkedHashMap<>();

        int kc = isPack ? 1 : Spans.span(type.key());
        int vc = isPack ? 1 : Spans.span(type.value());
        int itemSpan = kc + vc;

        for (CellsWithRowIndex block : blocks) {
            List<DCell> curLineParsed = block.cells;
            for (int startIdx = 0; startIdx < curLineParsed.size(); startIdx += itemSpan) {
                if (startIdx + itemSpan > curLineParsed.size()) {
                    errs.addErr(new ValueErrs.FieldCellSpanNotEnough(curLineParsed.subList(startIdx, curLineParsed.size()),
                            nameable, field.name(), itemSpan, curLineParsed.size() - startIdx));
                    continue;
                }
                List<DCell> keyCells = curLineParsed.subList(startIdx, startIdx + kc);
                List<DCell> valueCells = curLineParsed.subList(startIdx + kc, startIdx + itemSpan);

                //第一个单元作为是否有item的标记
                if (!keyCells.getFirst().isCellEmpty()) {
                    SimpleValue key = parseSimpleType(subType.key(), keyCells, type.key(),
                            isPack, false, block.rowIndex,
                            nameable, field.name());
                    SimpleValue value = parseSimpleType(subType.value(), valueCells, type.value(),
                            isPack, false, block.rowIndex,
                            nameable, field.name());

                    if (key != null && value != null) {
                        SimpleValue old = valueMap.put(key, value);
                        if (old != null) {
                            errs.addErr(new ValueErrs.MapKeyDuplicated(keyCells, nameable, field.name()));
                        }
                    }
                } else {
                    List<DCell> itemCells = curLineParsed.subList(startIdx, startIdx + itemSpan);
                    if (itemCells.stream().anyMatch(c -> !c.isCellEmpty())) {
                        errs.addErr(new ValueErrs.ContainerItemPartialSet(itemCells, nameable, field.name()));
                    }
                }
            }
        }

        return new VMap(valueMap, cells);
    }


    VList parseList(FieldSchema subField, List<DCell> cells, FieldSchema field,
                    boolean isPack, int curRowIndex,
                    String nameable) {

        FieldType.FList subType = (FieldType.FList) subField.type();
        FieldType.FList type = (FieldType.FList) field.type();


        List<DCell> parsed = null;
        List<CellsWithRowIndex> blocks = null;
        if (isPack) {
            require(cells.size() == 1);
            DCell cell = cells.getFirst();
            try {
                parsed = DCells.parseNestList(cell);
            } catch (Exception e) {
                errs.addErr(new ValueErrs.ParsePackErr(cell, type.toString(), e.getMessage()));
                return null;
            }

        } else if (field.fmt() instanceof FieldFormat.Block ignored) {
            blocks = blockParser.parseBlock(cells, curRowIndex);

        } else if (field.fmt() instanceof FieldFormat.Sep sep) {
            require(cells.size() == 1);
            DCell cell = cells.getFirst();
            parsed = DCells.parseList(cell, sep.sep());

        } else {
            require(cells.size() == Spans.span(field));
            parsed = cells;
        }

        if (blocks == null) {
            blocks = List.of(new CellsWithRowIndex(parsed, curRowIndex));
        }

        List<SimpleValue> valueList = new ArrayList<>();
        int itemSpan = isPack ? 1 : Spans.span(type.item());
        for (CellsWithRowIndex block : blocks) {
            List<DCell> curLineParsed = block.cells;
            for (int startIdx = 0; startIdx < curLineParsed.size(); startIdx += itemSpan) {
                if (startIdx + itemSpan > curLineParsed.size()) {
                    errs.addErr(new ValueErrs.FieldCellSpanNotEnough(curLineParsed.subList(startIdx, curLineParsed.size()),
                            nameable, field.name(), itemSpan, curLineParsed.size() - startIdx));
                    continue;
                }
                List<DCell> itemCells = curLineParsed.subList(startIdx, startIdx + itemSpan);
                //第一个单元作为是否有item的标记
                if (!itemCells.getFirst().isCellEmpty()) {
                    SimpleValue value = parseSimpleType(subType.item(), itemCells, type.item(),
                            isPack, false, block.rowIndex,
                            nameable, field.name());
                    if (value != null) {
                        valueList.add(value);
                    }
                } else {
                    if (itemCells.stream().anyMatch(c -> !c.isCellEmpty())) {
                        errs.addErr(new ValueErrs.ContainerItemPartialSet(itemCells, nameable, field.name()));
                    }
                }
            }
        }
        return new VList(valueList, cells);

    }

    private static final Set<String> boolStrSet = Set.of("false", "true", "1", "0", "");

    private void require(boolean cond) {
        if (!cond)
            throw new AssertionError("");
    }

    private void require(boolean cond, String err) {
        if (!cond)
            throw new AssertionError(err);
    }
}

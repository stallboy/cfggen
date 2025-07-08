package configgen.value;

import configgen.data.Source;
import configgen.schema.*;

import java.util.*;

import static configgen.data.CfgData.DCell;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.value.CfgValue.*;

public class ValueParser {

    public record CellsWithRowIndex(List<DCell> cells,
                                    int rowIndex) {
    }

    public interface ValueParserContext {
        List<CellsWithRowIndex> parseBlock(List<DCell> cells, int curRowIndex);

        ValueParserContext dummy = (cells, curRowIndex) -> List.of(new CellsWithRowIndex(cells, curRowIndex));
    }

    public record ParseContext(String nameable,
                               boolean pack,
                               boolean canBeEmpty,
                               int curRowIndex) {
    }

    private final CfgValueErrs errs;
    private final ValueParserContext parserContext;
    private List<DCell> currentCells;

    public ValueParser(CfgValueErrs errs, ValueParserContext parserContext) {
        Objects.requireNonNull(errs);
        Objects.requireNonNull(parserContext);
        this.errs = errs;
        this.parserContext = parserContext;
    }


    VInterface parseInterface(InterfaceSchema subInterface, List<DCell> cells, InterfaceSchema sInterface,
                              ParseContext parseContext) {
        List<DCell> parsed = cells;
        currentCells = cells;

        boolean isEmpty = false; // 支持excel里的cell为空，并且它还是个复合结构；但不支持非empty的cell里部分结构为空
        boolean isNumberOrBool = false;
        boolean canChildBeEmpty = parseContext.canBeEmpty;
        boolean isPack = parseContext.pack || sInterface.fmt() == PACK;
        if (isPack) {
            require(cells.size() == 1, "pack应该只占一格");
            DCell cell = cells.getFirst();
            cell.setModePackOrSep();
            if (parseContext.canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else if (sInterface.canBeNumberOrBool()) {
                if (DCells.isFunc(cell)) {
                    try {
                        parsed = DCells.parseFunc(cell);
                    } catch (Exception e) {
                        errs.addErr(new CfgValueErrs.ParsePackErr(cell, sInterface.name(), e.getMessage()));
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
                    errs.addErr(new CfgValueErrs.ParsePackErr(cell, sInterface.name(), e.getMessage()));
                    return null;
                }
                canChildBeEmpty = false;
            }

        } else {
            int wanted = Span.span(sInterface);
            require(cells.size() == wanted, "列宽度应一致, 结构定义宽度=" + wanted + ", 实际=" + cells.size());
        }

        // 内容为空的单一格子，处理方式是把这空的parsed一层层传下去
        VStruct vImpl;
        if (isEmpty) {
            StructSchema impl = sInterface.nullableDefaultImplStruct();
            StructSchema subImpl = subInterface.nullableDefaultImplStruct();
            if (impl == null) {
                errs.addErr(new CfgValueErrs.InterfaceCellEmptyButHasNoDefaultImpl(parsed.getFirst(), sInterface.name()));
                return null;
            }
            require(subImpl != null);

            // 之后按pack为true来处理，因为反正parsed里都是单个cell，并且里面value为空
            vImpl = parseStructural(subImpl, parsed, impl,
                    new ParseContext(parseContext.nameable, true, true, parseContext.curRowIndex));

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
                        errs.addErr(new CfgValueErrs.InterfaceCellImplNotFound(parsed.getFirst(), sInterface.name(), implName));
                        return null;
                    }
                } else {
                    impl = sInterface.nullableDefaultImplStruct();
                    subImpl = subInterface.nullableDefaultImplStruct();
                    if (impl == null) {
                        errs.addErr(new CfgValueErrs.InterfaceCellEmptyButHasNoDefaultImpl(parsed.getFirst(), sInterface.name()));
                        return null;
                    }
                }

                require(subImpl != null);
                int expected = isPack ? 1 : Span.span(impl);
                if (parsed.size() - 1 < expected) {
                    errs.addErr(new CfgValueErrs.InternalError(parsed.getFirst().toString() + " impl span not enough"));
                    return null;
                }
                implCells = parsed.subList(1, expected + 1);
            }
            vImpl = parseStructural(subImpl, implCells, impl,
                    new ParseContext(parseContext.nameable, isPack, canChildBeEmpty, parseContext.curRowIndex));
        }

        if (vImpl == null) {
            return null;
        }

        return new VInterface(subInterface, vImpl, Source.of(cells));
    }

    public VStruct parseStructural(Structural subStructural, List<DCell> cells, Structural structural,
                                   ParseContext parseContext) {
        currentCells = cells;

        List<DCell> parsed = cells;
        boolean isEmpty = false; // 支持excel里的cell为空，并且它还是个复合结构；但不支持非empty的cell里部分结构为空
        boolean canChildBeEmpty = parseContext.canBeEmpty;
        boolean isPack = parseContext.pack || structural.fmt() == PACK;
        boolean isSep = false;
        if (isPack) {
            require(cells.size() == 1, "pack应该只占一格");
            DCell cell = cells.getFirst();
            cell.setModePackOrSep();
            if (parseContext.canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else {
                try {
                    parsed = DCells.parsePack(cell);
                } catch (Exception e) {
                    errs.addErr(new CfgValueErrs.ParsePackErr(cell, structural.name(), e.getMessage()));
                    return null;
                }
                canChildBeEmpty = false;
            }

        } else if (structural.fmt() instanceof FieldFormat.Sep(char sep)) {
            require(cells.size() == 1, "sep应该只占一格");
            DCell cell = cells.getFirst();
            cell.setModePackOrSep();
            if (parseContext.canBeEmpty && cell.isCellEmpty()) {
                isEmpty = true;
            } else {
                parsed = DCells.parseList(cell, sep);
                canChildBeEmpty = false;
            }
            isSep = true;
        } else {
            int wanted = Span.span(structural);
            require(cells.size() == wanted, "列宽度应一致, 结构定义=" + wanted + ", 实际=" + cells.size());
        }

        List<Value> values = new ArrayList<>(subStructural.fields().size());
        if (isEmpty) {
            for (FieldSchema subField : subStructural.fields()) {
                FieldSchema field = structural.findField(subField.name());
                require(field != null);
                Value v = parseField(subField, parsed, field,
                        new ParseContext(structural.name(), true, true, parseContext.curRowIndex));

                if (v != null) {
                    values.add(v);
                } else {
                    return null;
                }
            }

        } else {
            int startIdx = 0;
            for (FieldSchema field : structural.fields()) {
                // 如果是pack，每个field只占1，这个特性是用于打断环嵌套导致的span无法计算问题的关键。
                // 比如数据为（a，b），c。 parsePack的时候返回2个数据a，b  和 c
                // 第一个field返回整体的a，b，在下一轮的parsePack里才分别取出a，b。
                //
                // 如果是sep，也只占1，CfgSchemaResolver里保证了
                // 而不要去查询Spans.fieldSpan，因为Spans会忽略掉struct为sep的field计算。
                int expected = isPack || isSep ? 1 : Span.fieldSpan(field);
                FieldSchema subField = subStructural.findField(field.name());
                if (subField != null) {
                    // 提取单个field
                    if (parsed.size() < startIdx + expected) {
                        errs.addErr(new CfgValueErrs.FieldCellSpanNotEnough(
                                Source.of(cells),
                                structural.name(), field.name(),
                                expected, parsed.size() - startIdx));
                        return null;
                    }

                    List<DCell> fieldCells = parsed.subList(startIdx, startIdx + expected);
                    Value v = parseField(subField, fieldCells, field,
                            new ParseContext(structural.name(), isPack || field.fmt() == PACK,
                                    canChildBeEmpty, parseContext.curRowIndex));
                    if (v != null) {
                        values.add(v);
                    } else {
                        return null;
                    }
                }

                startIdx += expected;
            }

            if (subStructural == structural) { // 说明没有filtered
                if (startIdx < parsed.size()) {
                    errs.addErr(new CfgValueErrs.FieldCellNotUsed(Source.of(cells),
                            structural.name(), parsed.subList(startIdx, parsed.size()).stream().map(DCell::value).toList()));
                }
            }
        }

        return new VStruct(subStructural, values, Source.of(cells));
    }

    SimpleValue parseSimpleType(FieldType.SimpleType subType, List<DCell> cells, FieldType.SimpleType type,
                                ParseContext parseContext, FieldSchema fieldSchema) {
        currentCells = cells;

        switch (type) {
            case FieldType.Primitive primitive -> {
                require(cells.size() == 1);
                DCell cell = cells.getFirst();
                String str = cell.value().trim();
                CfgValueErrs.NotMatchFieldType err = new CfgValueErrs.NotMatchFieldType(cell,
                        parseContext.nameable, fieldSchema.name(), type);
                switch (primitive) {
                    case BOOL -> {
                        boolean v = str.equals("1") || str.equalsIgnoreCase("true");
                        if (!boolStrSet.contains(str.toLowerCase())) {
                            errs.addErr(err);
                        }
                        return new VBool(v, cell);
                    }
                    case INT -> {
                        int v = 0;
                        try {
                            v = str.isEmpty() ? 0 : Integer.decode(str);
                        } catch (Exception e) {
                            errs.addErr(err);
                        }
                        return new VInt(v, cell);
                    }
                    case LONG -> {
                        long v = 0;
                        try {
                            v = str.isEmpty() ? 0 : Long.decode(str);
                        } catch (Exception e) {
                            errs.addErr(err);
                        }
                        return new VLong(v, cell);
                    }
                    case FLOAT -> {
                        float v = 0f;
                        try {
                            v = str.isEmpty() ? 0f : Float.parseFloat(str);
                        } catch (Exception e) {
                            errs.addErr(err);
                        }
                        return new VFloat(v, cell);
                    }
                    case STRING -> {
                        if (fieldSchema.meta().isLowercase()) {
                            str = str.toLowerCase();
                        }
                        return new VString(str, cell);
                    }
                    case TEXT -> {
                        if (fieldSchema.meta().isLowercase()) {
                            str = str.toLowerCase();
                        }
                        return new VText(str, cell);
                    }
                }
            }

            case FieldType.StructRef structRef -> {
                switch (structRef.obj()) {
                    case InterfaceSchema sInterface -> {
                        InterfaceSchema subInterface = (InterfaceSchema) (((FieldType.StructRef) subType).obj());

                        return parseInterface(subInterface, cells, sInterface,
                                parseContext);
                    }
                    case StructSchema struct -> {
                        StructSchema subStruct = (StructSchema) (((FieldType.StructRef) subType).obj());
                        return parseStructural(subStruct, cells, struct,
                                parseContext);
                    }
                }
            }
        }
        return null;
    }


    public Value parseField(FieldSchema subField, List<DCell> cells, FieldSchema field, ParseContext parseContext) {
        currentCells = cells;

        switch (field.type()) {
            case FieldType.SimpleType simple -> {
                return parseSimpleType((FieldType.SimpleType) subField.type(), cells, simple, parseContext, field);
            }
            case FieldType.FList ignored -> {
                return parseList(subField, cells, field, parseContext);
            }

            case FieldType.FMap ignored -> {
                return parseMap(subField, cells, field, parseContext);
            }
        }
    }

    VMap parseMap(FieldSchema subField, List<DCell> cells, FieldSchema field,
                  ParseContext parseContext) {
        currentCells = cells;

        FieldType.FMap subType = (FieldType.FMap) subField.type();
        FieldType.FMap type = (FieldType.FMap) field.type();

        List<DCell> parsed = null;
        List<CellsWithRowIndex> blocks = null;
        if (parseContext.pack) {
            require(cells.size() == 1);
            DCell cell = cells.getFirst();
            cell.setModePackOrSep();
            try {
                parsed = DCells.parsePack(cell);
            } catch (Exception e) {
                errs.addErr(new CfgValueErrs.ParsePackErr(cell, type.toString(), e.getMessage()));
                return null;
            }

        } else if (field.fmt() instanceof FieldFormat.Block ignored) {
            blocks = parserContext.parseBlock(cells, parseContext.curRowIndex);

        } else {
            require(cells.size() == Span.fieldSpan(field));
            parsed = cells;
        }

        if (blocks == null) {
            blocks = List.of(new CellsWithRowIndex(parsed, parseContext.curRowIndex));
        }

        Map<SimpleValue, SimpleValue> valueMap = new LinkedHashMap<>();

        int kc = parseContext.pack ? 1 : Span.simpleTypeSpan(type.key());
        int vc = parseContext.pack ? 1 : Span.simpleTypeSpan(type.value());
        int itemSpan = kc + vc;

        for (CellsWithRowIndex block : blocks) {
            List<DCell> curLineParsed = block.cells;
            for (int startIdx = 0; startIdx < curLineParsed.size(); startIdx += itemSpan) {
                if (startIdx + itemSpan > curLineParsed.size()) {
                    errs.addErr(new CfgValueErrs.FieldCellSpanNotEnough(
                            Source.of(curLineParsed.subList(startIdx, curLineParsed.size())),
                            parseContext.nameable, field.name(), itemSpan, curLineParsed.size() - startIdx));
                    continue;
                }
                List<DCell> keyCells = curLineParsed.subList(startIdx, startIdx + kc);
                List<DCell> valueCells = curLineParsed.subList(startIdx + kc, startIdx + itemSpan);

                // 可部分为空，全为空则忽略
                if (isCellNotAllEmpty(keyCells) || isCellNotAllEmpty(valueCells)) {
                    ParseContext ctx = new ParseContext(parseContext.nameable, parseContext.pack, false, block.rowIndex);
                    SimpleValue key = parseSimpleType(subType.key(), keyCells, type.key(),
                            ctx, field);
                    SimpleValue value = parseSimpleType(subType.value(), valueCells, type.value(),
                            ctx, field);

                    if (key != null && value != null) {
                        SimpleValue old = valueMap.put(key, value);
                        if (old != null) {
                            errs.addErr(new CfgValueErrs.MapKeyDuplicated(
                                    Source.of(keyCells), parseContext.nameable, field.name()));
                        }
                    }
                }
            }
        }

        return new VMap(valueMap, Source.of(cells));
    }


    VList parseList(FieldSchema subField, List<DCell> cells, FieldSchema field,
                    ParseContext parseContext) {
        currentCells = cells;

        FieldType.FList subType = (FieldType.FList) subField.type();
        FieldType.FList type = (FieldType.FList) field.type();


        List<DCell> parsed = null;
        List<CellsWithRowIndex> blocks = null;
        if (parseContext.pack) {
            require(cells.size() == 1);
            DCell cell = cells.getFirst();
            cell.setModePackOrSep();
            try {
                parsed = DCells.parsePack(cell);
            } catch (Exception e) {
                errs.addErr(new CfgValueErrs.ParsePackErr(cell, type.toString(), e.getMessage()));
                return null;
            }

        } else if (field.fmt() instanceof FieldFormat.Block ignored) {
            blocks = parserContext.parseBlock(cells, parseContext.curRowIndex);

        } else if (field.fmt() instanceof FieldFormat.Sep(char sep)) {
            require(cells.size() == 1);
            DCell cell = cells.getFirst();
            cell.setModePackOrSep();
            parsed = DCells.parseList(cell, sep);

        } else {
            require(cells.size() == Span.fieldSpan(field));
            parsed = cells;
        }

        if (blocks == null) {
            blocks = List.of(new CellsWithRowIndex(parsed, parseContext.curRowIndex));
        }

        List<SimpleValue> valueList = new ArrayList<>();
        int itemSpan = parseContext.pack ? 1 : Span.simpleTypeSpan(type.item());
        for (CellsWithRowIndex block : blocks) {
            List<DCell> curLineParsed = block.cells;
            for (int startIdx = 0; startIdx < curLineParsed.size(); startIdx += itemSpan) {
                if (startIdx + itemSpan > curLineParsed.size()) {
                    errs.addErr(new CfgValueErrs.FieldCellSpanNotEnough(
                            Source.of(curLineParsed.subList(startIdx, curLineParsed.size())),
                            parseContext.nameable, field.name(), itemSpan, curLineParsed.size() - startIdx));
                    continue;
                }
                List<DCell> itemCells = curLineParsed.subList(startIdx, startIdx + itemSpan);
                // 全为空，可忽略
                if (isCellNotAllEmpty(itemCells)) {
                    SimpleValue value = parseSimpleType(subType.item(), itemCells, type.item(),
                            new ParseContext(parseContext.nameable, parseContext.pack, false, block.rowIndex),
                            field);
                    if (value != null) {
                        valueList.add(value);
                    }
                }
            }
        }
        return new VList(valueList, Source.of(cells));

    }

    private static boolean isCellNotAllEmpty(List<DCell> cells) {
        return cells.stream().anyMatch(c -> !c.isCellEmpty());
    }

    private static final Set<String> boolStrSet = Set.of("false", "true", "1", "0", "");

    private void require(boolean cond) {
        if (!cond)
            throw new AssertionError(currentCellStr());
    }

    private void require(boolean cond, String err) {
        if (!cond)
            throw new AssertionError(err + ":" + currentCellStr());
    }

    private String currentCellStr() {
        StringBuilder err = new StringBuilder();
        if (currentCells != null) {
            for (DCell c : currentCells) {
                err.append(c.toString());
                err.append("\n");
            }
        }
        return err.toString();
    }
}

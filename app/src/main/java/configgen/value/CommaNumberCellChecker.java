package configgen.value;

import configgen.data.CfgData;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.DRawSheet;
import static configgen.data.CfgData.DTable;
import static configgen.schema.FieldFormat.*;
import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.*;
import static configgen.value.CfgValue.VTable;
import static configgen.value.ForeachFinder.*;
import static configgen.value.ValueErrs.*;

/**
 * excel的显示有个特性：当你输入12,345，excel会把12,345视为一个number：12345。
 * 而你想要的是list,int或intVector2结构，也就是12和345
 * intVector2时我们的检测系统会报少一个数的错，
 * list,int时则不会报错。此时得到了一个12345。
 * <p>
 * 这个类就是对单元格是list,int，intVector2类型，而excel中此格子又是有comma的number，这种情况提出警告
 * 策划需要在excel中把此单元格设置为文本类型（csv中无此问题）
 */
public class CommaNumberCellChecker {
    private final VTable vTable;
    private final TableSchema table;
    private final DTable dTable;
    private final ValueErrs errs;
    private List<Finder> finders;

    public CommaNumberCellChecker(VTable vTable, DTable dTable, ValueErrs errs) {
        this.vTable = vTable;
        table = vTable.schema();
        this.dTable = dTable;
        this.errs = errs;
    }

    public void check() {
        if (isCsvOnly()) {
            return;
        }

        finders = new ArrayList<>();
        collectFindersFromFields(table, Finder.of());
        if (finders.isEmpty()) {
            return;
        }

        for (Finder finder : finders) {
            ForeachFinder.foreachVTable(v -> visitValue(v, finder), vTable, finder);
        }
    }

    private void visitValue(Value value, Finder finder) {
        if (value.cells().size() != 1) {
            throw new IllegalStateException("TableNumberCellChecker value cell count != 1");
        }

        CfgData.DCell cell = value.cells().get(0);
        if (cell.isCellNumberWithComma()) {
            errs.addErr(new NumberWithCommaCellIsListIntOrIntStruct(cell, finder.type()));
        }
    }

    private boolean isCsvOnly() {
        return dTable.rawSheets().stream().allMatch(DRawSheet::isCsv);
    }

    private void collectFinders(Fieldable namable, Finder finder, FieldFormat fmt) {
        switch (namable) {
            case InterfaceSchema interfaceSchema -> {
                if (fmt == AUTO && interfaceSchema.fmt() == AUTO) {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        collectFinders(impl, finder.copyAdd(new FInterfaceImpl(impl)), AUTO);
                    }
                }
            }
            case StructSchema struct -> {
                if (isStructAllNumber(struct)) {
                    if (isFmtUseComma(fmt) || isFmtUseComma(struct.fmt())) {
                        finder.setType(struct.fullName());
                        finders.add(finder);
                    }
                } else if (fmt == AUTO) {
                    collectFindersFromFields(struct, finder);
                }
            }
        }
    }

    private void collectFindersFromFields(Structural structural, Finder finder) {
        int fieldIdx = 0;
        for (FieldSchema field : structural.fields()) {
            switch (field.type()) {
                case Primitive _ -> {
                }
                case StructRef structRef -> {
                    Finder findChain = finder.copyAdd(new FStructField(fieldIdx));
                    collectFinders(structRef.obj(), findChain, field.fmt());
                }
                case FList fList -> {
                    if (isFmtUseComma(field.fmt()) && isTypeNumber(fList.item())) {
                        Finder found = finder.copyAdd(new FStructField(fieldIdx));
                        found.setType(CfgWriter.typeStr(fList));
                        finders.add(found);
                    }

                    if (field.fmt() instanceof Fix || field.fmt() instanceof Block) {
                        switch (fList.item()) {
                            case Primitive _ -> {
                            }
                            case StructRef structRef -> {
                                Finder findChain = finder.copyAdd(new FStructField(fieldIdx), FContainerEach.LIST_ITEM);
                                collectFinders(structRef.obj(), findChain, field.fmt());
                            }
                        }
                    }
                }
                case FMap fMap -> {
                    if (field.fmt() instanceof Fix || field.fmt() instanceof Block) {
                        switch (fMap.key()) {
                            case Primitive _ -> {
                            }
                            case StructRef structRef -> {
                                Finder findChain = finder.copyAdd(new FStructField(fieldIdx), FContainerEach.MAP_KEY);
                                collectFinders(structRef.obj(), findChain, field.fmt());
                            }
                        }

                        switch (fMap.value()) {
                            case Primitive _ -> {
                            }
                            case StructRef structRef -> {
                                Finder findChain = finder.copyAdd(new FStructField(fieldIdx), FContainerEach.MAP_VALUE);
                                collectFinders(structRef.obj(), findChain, field.fmt());
                            }
                        }
                    }
                }
            }

            fieldIdx++;
        }
    }


    private boolean isStructAllNumber(StructSchema struct) {
        return struct.fields().size() > 1 && struct.fields().stream().allMatch(f -> isTypeNumber(f.type()));
    }

    private boolean isTypeNumber(FieldType t) {
        return t == INT || t == LONG || t == FLOAT || t == BOOL;
    }

    private boolean isFmtUseComma(FieldFormat fmt) {
        switch (fmt) {
            case AutoOrPack.PACK -> {
                return true;
            }
            case Sep sep -> {
                return sep.sep() == ',';
            }
            default -> {
                return false;
            }
        }
    }


}

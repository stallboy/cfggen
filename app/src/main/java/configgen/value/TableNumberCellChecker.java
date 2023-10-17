package configgen.value;

import configgen.schema.*;

import static configgen.data.CfgData.DRawSheet;
import static configgen.data.CfgData.DTable;
import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.VTable;

/**
 * excel的显示有个特性：当你输入12,345，excel会把12,345视为一个number：12345。
 * 而你想要的是list,int或intVector2结构，也就是12和345
 * intVector2时我们的检测系统会报少一个数的错，
 * list,int时则不会报错。此时得到了一个12345。
 * <p>
 * 这个类就是对单元格是list,int，intVector2类型，而excel中此格子又是number，这种情况提出警告
 * 策划需要在excel中把此单元格设置为文本类型（csv中无此问题）
 */
public class TableNumberCellChecker {
    private final VTable vTable;
    private final TableSchema table;
    private final DTable dTable;
    private final ValueErrs errs;

    public TableNumberCellChecker(VTable vTable, DTable dTable, ValueErrs errs) {
        this.vTable = vTable;
        table = vTable.schema();
        this.dTable = dTable;
        this.errs = errs;
    }

    public void check() {
        if (isCsvOnly()) {
            return;
        }


    }

    private boolean isCsvOnly() {
        return dTable.rawSheets().stream().allMatch(DRawSheet::isCsv);
    }

    private boolean needCheck(Structural structural) {
        for (FieldSchema field : structural.fields()) {
            switch (field.type()) {
                case Primitive _ -> {
                }
                case StructRef structRef -> {
                    switch (structRef.obj()) {
                        case InterfaceSchema interfaceSchema -> {
                            if (field.fmt() == AUTO && interfaceSchema.fmt() == AUTO) {
                                for (StructSchema impl : interfaceSchema.impls()) {
                                    if (needCheck(impl)) {
                                        return true;
                                    }
                                }
                            }
                        }
                        case StructSchema struct -> {
                            if (isStructAllNumber(struct)) {
                                if (isFieldFmtUseComma(field.fmt()) || isFieldFmtUseComma(struct.fmt())) {
                                    return true;
                                }
                            } else {
                                if (needCheck(struct)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                case FList fList -> {
                }
                case FMap fMap -> {
                }
            }

        }
    }

    private boolean isStructAllNumber(StructSchema struct) {
        return struct.fields().size() > 1 && struct.fields().stream().allMatch(f ->
                f.type() == INT || f.type() == LONG || f.type() == FLOAT || f.type() == BOOL);

    }

    private boolean isFieldFmtUseComma(FieldFormat fmt) {
        switch (fmt) {
            case FieldFormat.AutoOrPack.PACK -> {
                return true;
            }
            case FieldFormat.Sep sep -> {
                return sep.sep() == ',';
            }
            default -> {
                return false;
            }
        }
    }


}

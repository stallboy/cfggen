package configgen.value;

import configgen.Logger;
import configgen.schema.FieldType;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.DCell;

public record ValueErrs(List<VErr> errs) {

    public static ValueErrs of() {
        return new ValueErrs(new ArrayList<>());
    }

    void addErr(VErr err) {
        errs.add(err);
    }

    void merge(ValueErrs other) {
        errs.addAll(other.errs);
    }

    public void print() {
        if (!errs.isEmpty()) {
            Logger.log(STR. "errors \{ errs.size() }:" );
            for (VErr err : errs) {
                Logger.log("\t" + err);
            }
            throw new IllegalStateException("请修复value errors后再继续");
        }
    }

    public interface VErr {
    }

    public record ParsePackErr(DCell cell,
                               String nameable,
                               String err) implements VErr {
    }

    public record InterfaceCellEmptyButHasNoDefaultImpl(DCell cell,
                                                        String interfaceName) implements VErr {
    }

    public record InterfaceCellImplNotFound(DCell cell,
                                            String interfaceName,
                                            String notFoundImpl) implements VErr {
    }

    public record InterfaceCellImplSpanNotEnough(List<DCell> cells,
                                                 String interfaceName,
                                                 String implName,
                                                 int expected,
                                                 int notEnoughDataSpan) implements VErr {
    }

    /**
     * 需要的cell个数不匹配
     */
    public record FieldCellSpanNotEnough(List<DCell> cells,
                                         String nameable,
                                         String field,
                                         int expected,
                                         int notEnoughDataSpan) implements VErr {
    }

    /**
     * 类型不匹配
     */
    public record NotMatchFieldType(DCell cell,
                                    String nameable,
                                    String field,
                                    FieldType expectedType) implements VErr {
    }

    /**
     * list.item，或map.entry第一个为空格后，之后必须也都是空格
     */
    public record ContainerItemPartialSet(List<DCell> cells,
                                          String nameable,
                                          String field) implements VErr {
    }

    public record MapKeyDuplicated(List<DCell> cells,
                                   String nameable,
                                   String field) implements VErr {
    }

    public record PrimaryOrUniqueKeyDuplicated(List<DCell> cells,
                                               String table,
                                               List<String> keys) implements VErr {
    }

    public record EnumEmpty(DCell cell,
                            String table) implements VErr {
    }

    public record EntryContainsSpace(DCell cell,
                                     String table) implements VErr {
    }

    public record EntryDuplicated(DCell cell,
                                  String table) implements VErr {
    }


    public record RefNotNullableButCellEmpty(List<DCell> cells,
                                             String table) implements VErr {
    }

    public record ForeignValueNotFound(List<DCell> cells,
                                       String table,
                                       String foreignKey) implements VErr {

    }


}

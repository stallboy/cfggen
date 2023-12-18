package configgen.value;

import configgen.util.LocaleUtil;
import configgen.util.Logger;
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

    public void print(String prefix) {
        if (!errs.isEmpty()) {
            Logger.log("%s errors %d:", prefix, errs.size());
            for (VErr err : errs) {
                Logger.log("\t" + err);
            }
            Logger.log(LocaleUtil.getMessage("FixValueErrFirst"));
            System.exit(1);
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
        @Override
        public String toString() {
            return LocaleUtil.getMessage("InterfaceCellEmptyButHasNoDefaultImpl") + "{" +
                    "cell=" + cell +
                    ", interfaceName='" + interfaceName + '\'' +
                    '}';
        }
    }

    public record InterfaceCellImplNotFound(DCell cell,
                                            String interfaceName,
                                            String notFoundImpl) implements VErr {
        @Override
        public String toString() {
            return LocaleUtil.getMessage("InterfaceCellImplNotFound") + "{" +
                    "cell=" + cell +
                    ", interfaceName='" + interfaceName + '\'' +
                    ", notFoundImpl='" + notFoundImpl + '\'' +
                    '}';
        }
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
        @Override
        public String toString() {
            return LocaleUtil.getMessage("NotMatchFieldType") + "{" +
                    "cell=" + cell +
                    ", nameable='" + nameable + '\'' +
                    ", field='" + field + '\'' +
                    ", expectedType=" + expectedType +
                    '}';
        }
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
        @Override
        public String toString() {
            return LocaleUtil.getMessage("PrimaryOrUniqueKeyDuplicated") + "{" +
                    "cells=" + cells +
                    ", table='" + table + '\'' +
                    ", keys=" + keys +
                    '}';
        }
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
        @Override
        public String toString() {
            return LocaleUtil.getMessage("RefNotNullableButCellEmpty") + "{" +
                    "cells=" + cells +
                    ", table='" + table + '\'' +
                    '}';
        }
    }

    public record ForeignValueNotFound(List<DCell> cells,
                                       String table,
                                       String foreignKey) implements VErr {

        @Override
        public String toString() {
            return LocaleUtil.getMessage("ForeignValueNotFound") + "{" +
                    "cells=" + cells +
                    ", table='" + table + '\'' +
                    ", foreignKey='" + foreignKey + '\'' +
                    '}';
        }
    }

    public record JsonFileReadErr(String jsonFile) implements VErr {
    }

    public record JsonFileParseErr(String jsonFile, String table) implements VErr {
    }

}

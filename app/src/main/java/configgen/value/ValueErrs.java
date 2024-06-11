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

    public void checkErrors(String prefix, boolean allowErr) {
        if (!errs.isEmpty()) {
            Logger.log("%s errors %d:", prefix, errs.size());
            for (VErr err : errs) {
                Logger.log("\t" + err);
            }

            if (!allowErr){
                Logger.log(LocaleUtil.getMessage("FixValueErrFirst"));
                throw new ValueError(this);
            }
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

    public record InternalError(String internal) implements VErr {
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

    public record MapKeyDuplicated(List<DCell> cells,
                                   String nameable,
                                   String field) implements VErr {
    }

    public record PrimaryOrUniqueKeyDuplicated(CfgValue.Value value,
                                               String table,
                                               List<String> keys) implements VErr {
        @Override
        public String toString() {
            return LocaleUtil.getMessage("PrimaryOrUniqueKeyDuplicated") + "{" +
                    valueStr(value) +
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


    public record RefNotNullableButCellEmpty(CfgValue.Value value,
                                             String recordId) implements VErr {
        @Override
        public String toString() {
            return LocaleUtil.getMessage("RefNotNullableButCellEmpty") + "{" +
                    valueStr(value) +
                    ", recordId='" + recordId + '\'' +
                    '}';
        }
    }

    private static String valueStr(CfgValue.Value value) {
        String valStr;
        if (value.cells().isEmpty() || (value.cells().size() == 1 && value.cells().getFirst() == DCell.EMPTY)) {
            valStr = "value=" + value.packStr();
        } else {
            valStr = "cells=" + value.cells();
        }
        return valStr;
    }

    public record ForeignValueNotFound(CfgValue.Value value,
                                       String recordId,
                                       String foreignKey) implements VErr {

        @Override
        public String toString() {

            return LocaleUtil.getMessage("ForeignValueNotFound") + "{" +
                    valueStr(value) +
                    ", recordId='" + recordId + '\'' +
                    ", foreignKey='" + foreignKey + '\'' +
                    '}';
        }
    }

    public record JsonFileReadErr(String jsonFile, String errMsg) implements VErr {
    }

    public record JsonFileParseErr(String jsonFile, String errMsg) implements VErr {
    }

    public record JsonFileWriteErr(String jsonFile, String errMsg) implements VErr {
    }

}

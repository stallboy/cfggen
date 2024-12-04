package configgen.value;

import configgen.data.Source;
import configgen.schema.Msg;
import configgen.util.LocaleUtil;
import configgen.util.Logger;
import configgen.schema.FieldType;
import configgen.value.CfgValue.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static configgen.data.Source.*;

public record CfgValueErrs(List<VErr> errs,
                           List<VWarn> warns) {

    public static CfgValueErrs of() {
        return new CfgValueErrs(new ArrayList<>(), new ArrayList<>());
    }

    void addErr(VErr err) {
        errs.add(err);
    }

    void addWarn(VWarn warn) {
        warns.add(warn);
    }

    void merge(CfgValueErrs other) {
        errs.addAll(other.errs);
        warns.addAll(other.warns);
    }

    public void checkErrors(String prefix, boolean allowErr) {
        checkErrors(prefix, allowErr, Logger.isWarningEnabled());

    }

    public void checkErrors(String prefix, boolean allowErr, boolean logWarn) {
        if (logWarn && !warns.isEmpty()) {
            Logger.log("%s warnings %d:", prefix, warns.size());
            for (VWarn warn : warns) {
                Logger.log("\t" + warn.msg());
            }
        }

        if (!errs.isEmpty()) {
            Logger.log("%s errors %d:", prefix, errs.size());
            for (VErr err : errs) {
                Logger.log("\t" + err.msg());
            }

            if (!allowErr) {
                Logger.log(LocaleUtil.getLocaleString("FixValueErrFirst", "fix value errors first"));
                throw new CfgValueException(this);
            }
        }
    }

    public interface VErr extends Msg {
    }

    public interface VWarn extends Msg {
    }

    public record ParsePackErr(Source source,
                               String nameable,
                               String err) implements VErr {
    }

    public record InterfaceCellEmptyButHasNoDefaultImpl(Source source,
                                                        String interfaceName) implements VErr {
    }

    public record InterfaceCellImplNotFound(Source source,
                                            String interfaceName,
                                            String notFoundImpl) implements VErr {
    }

    public record InternalError(String internal) implements VErr {
    }

    /**
     * 需要的cell个数不匹配
     */
    public record FieldCellSpanNotEnough(Source source,
                                         String nameable,
                                         String field,
                                         int expected,
                                         int notEnoughDataSpan) implements VErr {
    }

    /**
     * 类型不匹配
     */
    public record NotMatchFieldType(Source source,
                                    String nameable,
                                    String field,
                                    FieldType expectedType) implements VErr {
    }

    public record MapKeyDuplicated(Source source,
                                   String nameable,
                                   String field) implements VErr {
    }

    public record PrimaryOrUniqueKeyDuplicated(Value value,
                                               String table,
                                               List<String> keys) implements VErr {
    }

    public record EnumEmpty(Source source,
                            String table) implements VErr {
    }

    public record EntryContainsSpace(Source source,
                                     String table) implements VErr {
    }

    public record EntryDuplicated(Source source,
                                  String table) implements VErr {
    }


    public record RefNotNullableButCellEmpty(Value value,
                                             String recordId) implements VErr {
    }

    public record ForeignValueNotFound(Value value,
                                       String recordId,
                                       String foreignTable,
                                       String foreignKey) implements VErr {
    }

    public record JsonFileReadErr(String jsonFile, String errMsg) implements VErr {
    }

    public record JsonFileWriteErr(String jsonFile, String errMsg) implements VErr {
    }

    public record JsonStrEmpty(DFile source) implements VErr {
    }

    public record JsonParseException(DFile source, String err) implements VErr {
    }

    public record JsonTypeNotExist(DFile source, String expected) implements VErr {
    }

    public record JsonTypeNotMatch(DFile source, String type, String expected) implements VErr {
    }

    public record JsonHasExtraFields(DFile source, String type, Set<String> extraFields) implements VWarn {
    }

    public enum EType {
        BOOL,
        INT,
        LONG,
        FLOAT,
        STR,
        ARRAY,
        MAP,
        MAP_ENTRY,
        STRUCT,
    }

    public record JsonValueNotMatchType(DFile source, String value, EType expectedType) implements VErr {
    }


}

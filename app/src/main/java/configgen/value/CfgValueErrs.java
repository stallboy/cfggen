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

    /**
     * pack格式不对
     */
    public record ParsePackErr(Source source,
                               String nameable,
                               String err) implements VErr {
    }

    /**
     * interface的格子为空，且interface没有配置defaultImpl
     */
    public record InterfaceCellEmptyButHasNoDefaultImpl(Source source,
                                                        String interfaceName) implements VErr {
    }

    /**
     * interface没有这个impl
     */
    public record InterfaceCellImplNotFound(Source source,
                                            String interfaceName,
                                            String notFoundImpl) implements VErr {
    }

    /**
     * 内部错误，不该发生，请检查程序
     */
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

    /**
     * 字典类型key重复
     */
    public record MapKeyDuplicated(Source source,
                                   String nameable,
                                   String field) implements VErr {
    }

    /**
     * 主键或唯一键重复
     */
    public record PrimaryOrUniqueKeyDuplicated(Value value,
                                               String table,
                                               List<String> keys) implements VErr {
    }

    /**
     * 枚举字符串为空
     */
    public record EnumEmpty(Source source,
                            String table) implements VErr {
    }

    /**
     * 入口或枚举字符串包含空格
     */
    public record EntryContainsSpace(Source source,
                                     String table) implements VErr {
    }

    /**
     * 入口或枚举字符串有重复
     */
    public record EntryDuplicated(Source source,
                                  String table) implements VErr {
    }


    /**
     * 有外键的字段，excel格子不能为空
     */
    public record RefNotNullableButCellEmpty(Value value,
                                             String recordId) implements VErr {
    }

    /**
     * 外键未找到
     */
    public record ForeignValueNotFound(Value value,
                                       String recordId,
                                       String foreignTable,
                                       String foreignKey) implements VErr {
    }

    /**
     * 读json文件出错
     */
    public record JsonFileReadErr(String jsonFile, String errMsg) implements VErr {
    }

    /**
     * 写json文件出错
     */
    public record JsonFileWriteErr(String jsonFile, String errMsg) implements VErr {
    }

    /**
     * json文件内容为空
     */
    public record JsonStrEmpty(DFile source) implements VErr {
    }

    /**
     * 解析json出错
     */
    public record JsonParseException(DFile source, String err) implements VErr {
    }

    /**
     * json文件中$type类型未找到
     */
    public record JsonTypeNotExist(DFile source, String expected) implements VErr {
    }

    /**
     * json文件中$type跟实际期待的不匹配
     */
    public record JsonTypeNotMatch(DFile source, String type, String expected) implements VErr {
    }

    /**
     * json文件里包含了额外的字段，可能是json结构变化了，修改导致保存后，这些信息回丢失！
     */
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

    /**
     * json文件中的值不是期待的类型
     */
    public record JsonValueNotMatchType(DFile source, String value, EType expectedType) implements VErr {
    }


}

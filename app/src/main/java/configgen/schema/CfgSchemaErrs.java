package configgen.schema;

import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.util.*;

public record CfgSchemaErrs(List<Err> errs,
                            List<Warn> warns) {
    public static CfgSchemaErrs of() {
        return new CfgSchemaErrs(new ArrayList<>(), new ArrayList<>());
    }

    public CfgSchemaErrs {
        Objects.requireNonNull(errs);
        Objects.requireNonNull(warns);
    }

    public void addErr(Err err) {
        errs.add(Objects.requireNonNull(err));
    }

    public void addWarn(Warn warn) {
        warns.add(Objects.requireNonNull(warn));
    }

    public void checkErrors() {
        checkErrors("schema");
    }

    public void checkErrors(String prefix) {
        if (Logger.isWarningEnabled() && !warns.isEmpty()) {
            Logger.log("%s warnings %d:", prefix, warns.size());
            for (Warn warn : warns) {
                Logger.log("\t" + warn.msg());
            }
        }

        if (!errs.isEmpty()) {
            Logger.log("%s errors %d:", prefix, errs.size());
            for (Err err : errs) {
                Logger.log("\t" + err.msg());
            }
            Logger.log(LocaleUtil.getLocaleString("FixSchemaErrFirst", "fix schema errors first"));
            throw new CfgSchemaException(this);
        }
    }

    public sealed interface Warn extends Msg {
    }


    /**
     * 检查局部名字空间和全局名字空间潜在的冲突
     * 1, interface的局部名字空间里，可能跟全局的冲突
     * 2, 分文件存储schema，可能导致的混乱
     */
    public record NameMayConflictByRef(String name1,
                                       String name2) implements Warn {
    }

    public record StructNotUsed(String name) implements Warn {
    }

    public record InterfaceNotUsed(String name) implements Warn {
    }

    public record FilterRefIgnoredByRefTableNotFound(String name,
                                                     String foreignKey,
                                                     String notFoundRefTable) implements Warn {
    }

    public record FilterRefIgnoredByRefKeyNotFound(String name,
                                                   String foreignKey,
                                                   String refTable,
                                                   List<String> notFoundRefKey) implements Warn {
    }

    public sealed interface Err extends Msg {
    }

    /**
     * table名称必须全小写，(因为windows文件名不分大小写，而table名可能就是文件名，这里直接约定必须都小写)
     */
    public record TableNameNotLowerCase(String tableName) implements Err {
    }

    /**
     * 在interface里的struct不能是a.b这种格式
     */
    public record ImplNamespaceNotEmpty(String sInterface,
                                        String errImplName) implements Err {
    }

    /**
     * table，struct，interface名字冲突
     */
    public record NameConflict(String name) implements Err {
    }

    /**
     * field 名字冲突
     */
    public record InnerNameConflict(String item, String name) implements Err {
    }


    /**
     * 类型未找到
     */
    public record TypeStructNotFound(String struct,
                                     String field,
                                     String notFoundStruct) implements Err {
    }

    /**
     * primitive类型字段fmt必须是auto
     */
    public record PrimitiveFieldFmtMustBeAuto(String struct,
                                              String field,
                                              String type,
                                              String errFmt) implements Err {
    }

    /**
     * struct类型字段fmt必须是auto或pack
     */
    public record StructFieldFmtMustBeAutoOrPack(String struct,
                                                 String field,
                                                 String type,
                                                 String errFmt) implements Err {
    }

    /**
     * list类型字段fmt必须是pack，sep，fix，block
     */
    public record ListFieldFmtMustBePackOrSepOrFixOrBlock(String struct,
                                                          String field,
                                                          String type,
                                                          String errFmt) implements Err {
    }


    /**
     * list类型字段fmt必须是pack，fix，block
     */
    public record MapFieldFmtMustBePackOrFixOrBlock(String struct,
                                                    String field,
                                                    String type,
                                                    String errFmt) implements Err {
    }

    /**
     * 为了简单和一致性，在interface的impl上不支持配置fmt
     * 因为如果配置了pack或sep，则这第一列就有些是impl的名字，有些不是，不一致。
     */
    public record ImplFmtNotSupport(String inInterface,
                                    String impl,
                                    String errFmt) implements Err {
    }

    /**
     * 为简单，只有field都是primitive类型的struct可以配置了sep
     */
    public record SepFmtStructHasUnPrimitiveField(String struct) implements Err {
    }

    /**
     * list,struct结构，如果list和struct的fmt都是sep，且分隔符选择相同，这也是不支持的
     */
    public record ListStructSepEqual(String structural,
                                     String field) implements Err {
    }

    /**
     * interface对应的枚举表不存在
     */
    public record EnumRefNotFound(String sInterface,
                                  String enumRef) implements Err {
    }

    /**
     * interface里无struct
     */
    public record InterfaceImplEmpty(String sInterface) implements Err {
    }

    /**
     * interface的默认实现不存在
     */
    public record DefaultImplNotFound(String sInterface,
                                      String defaultImpl) implements Err {
    }

    /**
     * table的entry或enum对应的字段不存在
     */
    public record EntryNotFound(String table,
                                String entry) implements Err {
    }

    /**
     * table的entry或enum对应的字段类型不是string
     */
    public record EntryFieldTypeNotStr(String table,
                                       String entry,
                                       String errType) implements Err {
    }

    /**
     * 用table逻辑第一列格子是否为空来判断这行是属于上一个record的block，还是新的一格record
     * 所以要保证新record的第一列必须填。
     * 这里我们约定有block的，primary key所在列必须包含第一列，primary key总是要填的吧。
     */
    public record BlockTableFirstFieldNotInPrimaryKey(String table) implements Err {
    }

    /**
     * 主键、唯一键、外键的本地健 不存在
     */
    public record KeyNotFound(String structural,
                              String key) implements Err {
    }

    /**
     * 可以做为主键或唯一键的字段，或者是基本类型int, long, bool, str
     * 或者是struct，struct里的字段类型必须为int，long，bool，str
     * 或者是多个字段，构建成隐含的struct，同样要符合struct内字段类型必须为int, long, bool, str
     */
    public record KeyTypeNotSupport(String structural,
                                    String field,
                                    String errType) implements Err {
    }

    /**
     * 外键对应的table不存在
     */
    public record RefTableNotFound(String table,
                                   String foreignKey,
                                   String errRefTable) implements Err {
    }

    /**
     * 外键到table.key，这里key不是table的唯一键
     */
    public record RefTableKeyNotUniq(String table,
                                     String foreignKey,
                                     String refTable,
                                     List<String> notUniqRefKey) implements Err {
    }

    /**
     * one to many的外键（listRef），这个local key和remote key都只支持单字段
     */
    public record ListRefMultiKeyNotSupport(String table,
                                            String foreignKey,
                                            List<String> errMultiKey) implements Err {
    }

    /**
     * 外键的local key和remote key数量不匹配
     */
    public record RefLocalKeyRemoteKeyCountNotMatch(String table,
                                                    String foreignKey) implements Err {
    }

    /**
     * 外键的local key和remote key类型不匹配
     */
    public record RefLocalKeyRemoteKeyTypeNotMatch(String structural,
                                                   String foreignKey,
                                                   String localType,
                                                   String refType) implements Err {
    }

    /**
     * list，map的ref不应该是nullable
     */
    public record RefContainerNullable(String structural,
                                       String foreignKey) implements Err {
    }

    /**
     * csv或excel的第二行名称不是标识符，没法作为程序名
     */
    public record DataHeadNameNotIdentifier(String table,
                                            String notIdentifierName) implements Err {
    }

    /**
     * 标记了json的table不能有对应的excel文件
     */
    public record JsonTableNotSupportExcel(String table,
                                           List<String> excelSheetList) implements Err {
    }

    /**
     * 标记了json的table不能有Map类型，cfgeditor不支持。
     */
    public record JsonTableNotSupportMap(String table) implements Err {
    }

    /**
     * 结构有循环而且没有用pack，导致无法映射到excel列
     */
    public record MappingToExcelLoop(SequencedCollection<String> structNameLoop) implements Err {
    }
}

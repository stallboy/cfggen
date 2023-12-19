package configgen.schema;

import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record SchemaErrs(List<Err> errs,
                         List<Warn> warns) {
    public static SchemaErrs of() {
        return new SchemaErrs(new ArrayList<>(), new ArrayList<>());
    }

    public SchemaErrs {
        Objects.requireNonNull(errs);
    }

    public void addErr(Err err) {
        errs.add(err);
    }

    public void addWarn(Warn warn) {
        warns.add(warn);
    }

    public void print(String prefix) {
        if (Logger.isWarningEnabled() && !warns.isEmpty()) {
            Logger.log("%s warnings %d:", prefix, warns.size());
            for (Warn warn : warns) {
                Logger.log("\t" + warn);
            }
        }

        if (!errs.isEmpty()) {
            Logger.log("%s errors %d:", prefix, errs.size());
            for (Err err : errs) {
                Logger.log("\t" + err);
            }
            Logger.log(LocaleUtil.getMessage("FixSchemaErrFirst"));
            System.exit(1);
        }
    }

    public sealed interface Warn {
    }

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

    public sealed interface Err {
    }

    public record TableNameNotLowerCase(String tableName) implements Err {
    }

    public record ImplNamespaceNotEmpty(String sInterface,
                                        String errImplName) implements Err {
    }

    public record NameConflict(String name) implements Err {
    }

    public record InnerNameConflict(String item, String name) implements Err {
    }


    public record TypeStructNotFound(String struct,
                                     String field,
                                     String notFoundStruct) implements Err {
    }

    public record TypeFmtNotCompatible(String struct,
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

    public record EnumRefNotFound(String sInterface,
                                  String enumRef) implements Err {
    }

    public record InterfaceImplEmpty(String sInterface) implements Err {
    }

    public record DefaultImplNotFound(String sInterface,
                                      String defaultImpl) implements Err {
    }

    public record EntryNotFound(String table,
                                String entry) implements Err {
    }

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

    public record KeyNotFound(String structural,
                              String key) implements Err {
    }

    /**
     * 可以做为主键或唯一键的字段，或者是基本类型int, long, bool, str, res
     * 或者是struct，struct里的字段类型必须为int，long，bool，str, res
     * 或者是多个字段，构建成隐含的struct，同样要符合struct内字段类型必须为int, long, bool, str, res
     */
    public record KeyTypeNotSupport(String structural,
                                    String field,
                                    String errType) implements Err {
    }

    public record RefTableNotFound(String table,
                                   String foreignKey,
                                   String errRefTable) implements Err {
    }

    public record RefTableKeyNotUniq(String table,
                                     String foreignKey,
                                     String refTable,
                                     List<String> notUniqRefKey) implements Err {
    }

    public record ListRefMultiKeyNotSupport(String table,
                                            String foreignKey,
                                            List<String> errMultiKey) implements Err {
    }

    public record RefLocalKeyRemoteKeyCountNotMatch(String table,
                                                    String foreignKey) implements Err {
    }

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

    public record JsonTableNotSupportExcel(String table,
                                           List<String> excelSheetList) implements Err {
    }


}

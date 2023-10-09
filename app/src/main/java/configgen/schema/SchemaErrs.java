package configgen.schema;

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

    void addErr(Err err) {
        errs.add(err);
    }

    void addWarn(Warn warn) {
        warns.add(warn);
    }

    public void print() {
        System.out.println(STR. "warnings \{ warns.size() }:" );
        for (Warn warn : warns) {
            System.out.println("\t" + warn);
        }
        System.out.println(STR. "errors \{ errs.size() }:" );
        for (Err err : errs) {
            System.out.println("\t" + err);
        }
    }

    public sealed interface Warn {
    }

    record NameMayConflictByRef(String name1,
                                String name2) implements Warn {
    }

    record StructNotUsed(String name) implements Warn {
    }

    record InterfaceNotUsed(String name) implements Warn {
    }

    record FilterRefIgnoredByRefTableNotFound(String name,
                                              String foreignKey,
                                              String notFoundRefTable) implements Warn {
    }

    record FilterRefIgnoredByRefKeyNotFound(String name,
                                            String foreignKey,
                                            String refTable,
                                            List<String> notFoundRefKey) implements Warn {
    }

    public sealed interface Err {
    }

    record ImplNamespaceNotEmpty(String sInterface,
                                 String errImplName) implements Err {
    }

    record NameConflict(String name) implements Err {
    }

    record InnerNameConflict(String item, String name) implements Err {
    }


    record TypeStructNotFound(String struct,
                              String field,
                              String notFoundStruct) implements Err {
    }

    record TypeFmtNotCompatible(String struct,
                                String field,
                                String type,
                                String errFmt) implements Err {
    }

    /**
     * 为了简单和一致性，在interface的impl上不支持配置fmt
     *  因为如果配置了pack或sep，则这第一列就有些是impl的名字，有些不是，不一致。
     */
    record ImplFmtNotSupport(String inInterface,
                             String impl,
                             String errFmt) implements Err {
    }

    /**
     * 为简单，只有field都是primitive类型的struct可以配置了sep
     */
    record SepFmtStructHasUnPrimitiveField(String struct) implements Err {
    }

    /**
     * list,struct结构，如果list和struct的fmt都是sep，且分隔符选择相同，这也是不支持的
     */
    record ListStructSepEqual(String structural,
                              String field) implements Err {
    }

    record EnumRefNotFound(String sInterface,
                           String enumRef) implements Err {
    }

    record DefaultImplNotFound(String sInterface,
                               String defaultImpl) implements Err {
    }

    record EntryNotFound(String table,
                         String entry) implements Err {
    }

    record EntryFieldTypeNotStr(String table,
                                String entry,
                                String errType) implements Err {
    }

    /**
     * 用table逻辑第一列格子是否为空来判断这行是属于上一个record的block，还是新的一格record
     * 所以要保证新record的第一列必须填。
     * 这里我们约定有block的，primary key所在列必须包含第一列，primary key总是要填的吧。
     */
    record BlockTableFirstFieldNotInPrimaryKey(String table) implements Err {
    }

    record KeyNotFound(String structural,
                       String key) implements Err {
    }

    /**
     * 可以做为主键或唯一键的字段，或者是基本类型int, long, bool, str, res
     * 或者是struct，struct里的字段类型必须为int，long，bool，str, res
     * 或者是多个字段，构建成隐含的struct，同样要符合struct内字段类型必须为int, long, bool, str, res
     */
    record KeyTypeNotSupport(String structural,
                             String field,
                             String errType) implements Err {
    }

    record RefTableNotFound(String table,
                            String foreignKey,
                            String errRefTable) implements Err {
    }

    record RefTableKeyNotUniq(String table,
                              String foreignKey,
                              String refTable,
                              List<String> notUniqRefKey) implements Err {
    }

    record ListRefMultiKeyNotSupport(String table,
                                     String foreignKey,
                                     List<String> errMultiKey) implements Err {
    }

    record RefLocalKeyRemoteKeyCountNotMatch(String table,
                                             String foreignKey) implements Err {
    }

    record RefLocalKeyRemoteKeyTypeNotMatch(String table,
                                            String foreignKey,
                                            String localType,
                                            String refType) implements Err {
    }

}
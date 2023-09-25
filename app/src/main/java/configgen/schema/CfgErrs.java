package configgen.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record CfgErrs(List<Err> errs, List<Warn> warns) {
    public static CfgErrs of() {
        return new CfgErrs(new ArrayList<>(), new ArrayList<>());
    }

    public CfgErrs {
        Objects.requireNonNull(errs);
    }

    void addErr(Err err) {
        errs.add(err);
    }

    void addWarn(Warn warn) {
        warns.add(warn);
    }

    public sealed interface Warn {
    }

    record NameMayConflictByRef(String name1,
                                String name2) implements Warn {
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

    record ImplFmtNotSupport(String inInterface,
                             String impl,
                             String errFmt) implements Err {
    }

    record SepFmtStructHasNoPrimitive(String struct) implements Err {
    }

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

    record KeyNotFound(String structural,
                       String key) implements Err {
    }

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
                              String notUniqRefKey) implements Err {
    }

    record ListRefMultiKeyNotSupport(String table,
                                     String foreignKey,
                                     String errMultiKey) implements Err {
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

package configgen.schema;

public sealed interface Err {

    record ImplNamespaceNotEmpty(String sInterface,
                                 String errImplName) implements Err {
    }

    record NameConflict(String name) implements Err {
    }

    record InnerNameConflict(String item, String name) implements Err {
    }

    record NameMayConflictByRef(String name1,
                                String name2) implements Err {
    }

    record TypeNotFound(String struct,
                        String field,
                        String type) implements Err {
    }

    record TypeFmtNotCompatible(String struct,
                                String field,
                                String type,
                                String errFmt) implements Err {
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

package configgen.schema;

import configgen.schema.EntryType.EntryBase;

import java.util.*;

import static configgen.schema.Err.*;
import static configgen.schema.FieldFormat.AutoOrPack;
import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.Sep;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;

/**
 * 把CfgSchema内部关系给解决了，如果有Errs，就表明有内部矛盾
 */
public final class CfgResolver {
    private final CfgSchema cfg;
    private final CfgErrs errs;
    private Nameable curNameable;
    private StructSchema curImpl;
    private boolean isInCurImpl;

    public CfgResolver(CfgSchema cfg, CfgErrs errs) {
        this.cfg = cfg;
        this.errs = errs;
    }

    public void resolve() {
        step0_checkNameConflict();
        step1_resolveAllFields();
        step2_resolveEachNameable();
        step3_resolveAllForeignKeys();
    }

    /**
     * 如果所有的cfg都配置在一个文件里，那么不需要检测
     * 如果配在每个文件夹下，虽然代码可以按从interface scope -> local scope -> global scope的顺序来解析名字。
     * 但为了清晰性，我们一开始叫避免这种可能的混乱。
     */
    private void step0_checkNameConflict() {
        Set<List<String>> nameSet = new HashSet<>();
        for (Nameable item : cfg.items()) {
            List<String> list = Arrays.asList(item.name().split("\\."));
            if (!nameSet.add(list)) {
                errs.addErr(new NameConflict(item.name()));
            }

            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        if (!impl.namespace().isEmpty()) {
                            errs.addErr(new ImplNamespaceNotEmpty(interfaceSchema.name(), impl.name()));
                        } else if (!nameSet.add(List.of(impl.name()))) {
                            errs.addErr(new NameConflict(impl.name()));
                        }
                        checkInnerNameConflict(impl);
                    }
                }
                case Structural structural -> checkInnerNameConflict(structural);
            }
        }

        for (List<String> name : nameSet) {
            if (name.size() <= 1) {
                continue;
            }
            int len = name.size();
            for (int i = 1; i < len; i++) {
                List<String> sub = name.subList(i, len);
                if (nameSet.contains(sub)) {
                    String name1 = String.join(".", name);
                    String name2 = String.join(".", sub);
                    errs.addErr(new NameMayConflictByRef(name1, name2));
                }
            }
        }

        Map<String, Fieldable> structMap = new HashMap<>();
        Map<String, TableSchema> tableMap = new HashMap<>();
        for (Nameable item : cfg.items()) {
            switch (item) {
                case Fieldable fieldable -> structMap.put(fieldable.name(), fieldable);
                case TableSchema table -> tableMap.put(table.name(), table);
            }
        }
        cfg.resolve(structMap, tableMap);
    }


    private void checkInnerNameConflict(Structural structural) {
        Set<String> innerNameSet = new HashSet<>();
        for (FieldSchema field : structural.fields()) {
            if (!innerNameSet.add(field.name())) {
                errs.addErr(new InnerNameConflict(structural.name(), field.name()));
            }
        }
        innerNameSet.clear();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            if (!innerNameSet.add(fk.name())) {
                errs.addErr(new InnerNameConflict(structural.name(), fk.name()));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////
    private void step1_resolveAllFields() {
        resolve_structural(this::resolveFields);
    }

    private interface Action<T> {
        void run(T t);
    }

    private void resolve_structural(Action<Structural> action) {
        for (Nameable value : cfg.items()) {
            curNameable = value;
            switch (value) {
                case StructSchema struct -> {
                    action.run(struct);
                }
                case InterfaceSchema sInterface -> {
                    for (StructSchema impl : sInterface.impls()) {
                        this.isInCurImpl = true;
                        this.curImpl = impl;
                        action.run(impl);
                    }
                    this.isInCurImpl = false;
                }
                case TableSchema table -> {
                    action.run(table);
                }
            }
        }
    }

    private void resolveFields(Structural structural) {
        for (FieldSchema field : structural.fields()) {
            resolveField(field);
        }
    }

    private void resolveField(FieldSchema field) {
        FieldType type = field.type();
        resolveFieldType(field, type);

        FieldFormat fmt = field.fmt();
        switch (type) {
            case Container _ -> {
                if (fmt == AUTO) {
                    errTypeFmtNotCompatible(field.name(), type.toString(), fmt.toString());
                }
            }
            case Primitive _ -> {
                if (fmt != AUTO) {
                    errTypeFmtNotCompatible(field.name(), type.toString(), fmt.toString());
                }
            }
            case StructRef _ -> {
                if (!(fmt instanceof AutoOrPack) && !(fmt instanceof Sep)) {
                    errTypeFmtNotCompatible(field.name(), type.toString(), fmt.toString());
                }
            }
        }
    }


    private void resolveFieldType(FieldSchema field, FieldType type) {
        switch (type) {
            case FList(FieldType item) -> {
                resolveFieldType(field, item);
            }
            case FMap(FieldType key, FieldType value) -> {
                resolveFieldType(field, key);
                resolveFieldType(field, value);
            }
            case Primitive _ -> {
            }
            case StructRef structRef -> {
                String name = structRef.name();
                // interface里找
                if (curNameable instanceof InterfaceSchema sInterface) {
                    StructSchema obj = sInterface.findImpl(name);
                    if (obj != null) {
                        structRef.setObj(obj);
                        return;
                    }
                }

                // 本模块找
                String namespace = curNameable.namespace();
                if (!namespace.isEmpty()) {
                    String fullName = Nameable.makeName(namespace, name);
                    Fieldable obj = cfg.findFieldable(fullName);
                    if (obj != null) {
                        structRef.setObj(obj);
                        return;
                    }
                }

                // 全局找
                Fieldable obj = cfg.findFieldable(name);
                if (obj != null) {
                    structRef.setObj(obj);
                    return;
                }

                errTypeNotFound(field.name(), name);
            }
        }
    }

    private void errTypeNotFound(String field, String type) {
        errs.addErr(new TypeNotFound(ctx(), field, type));
    }

    private void errTypeFmtNotCompatible(String field, String type, String errFmt) {
        errs.addErr(new TypeFmtNotCompatible(ctx(), field, type, errFmt));
    }

    private String ctx() {
        String ctx = curNameable.name();
        if (isInCurImpl) {
            ctx += "." + curImpl.name();
        }
        return ctx;
    }

    ////////////////////////////////////////////////////////////////////
    private void step2_resolveEachNameable() {
        for (Nameable item : cfg.items()) {
            curNameable = item;
            switch (item) {
                case StructSchema _ -> {
                }
                case InterfaceSchema sInterface -> resolveInterface(sInterface);
                case TableSchema table -> resolveTable(table);
            }
        }
    }

    private void resolveInterface(InterfaceSchema sInterface) {
        String enumRef = sInterface.enumRef();
        TableSchema enumRefTable = findTableInLocalThenGlobal(enumRef);
        if (enumRefTable != null) {
            sInterface.setEnumRefTable(enumRefTable);
        } else {
            errEnumRefNotFound(enumRef);
        }

        String def = sInterface.defaultImpl();
        if (!def.isEmpty()) {
            StructSchema defImpl = sInterface.findImpl(def);
            if (defImpl != null) {
                sInterface.setDefaultImplStruct(defImpl);
            } else {
                errDefaultImplNotFound(def);
            }
        }
    }

    private TableSchema findTableInLocalThenGlobal(String name) {
        // 本模块找
        String namespace = curNameable.namespace();
        if (!namespace.isEmpty()) {
            String fullName = Nameable.makeName(namespace, name);
            TableSchema table = cfg.findTable(fullName);
            if (table != null) {
                return table;
            }
        }

        // 全局找
        return cfg.findTable(name);
    }

    private void errEnumRefNotFound(String enumRef) {
        errs.addErr(new EnumRefNotFound(curNameable.name(), enumRef));
    }

    private void errDefaultImplNotFound(String defaultImpl) {
        errs.addErr(new DefaultImplNotFound(curNameable.name(), defaultImpl));
    }

    private void resolveTable(TableSchema table) {
        resolveEntry(table, table.entry());

        KeySchema primaryKey = table.primaryKey();
        if (resolveKey(table, primaryKey)) {
            checkPrimaryOrUniqKey(primaryKey);
        }
        for (KeySchema key : table.uniqueKeys()) {
            if (resolveKey(table, key)) {
                checkPrimaryOrUniqKey(key);
            }
        }
    }

    private void resolveEntry(TableSchema table, EntryType entry) {
        if (entry instanceof EntryBase entryBase) {
            String fn = entryBase.field();
            FieldSchema fs = table.findField(fn);
            if (fs != null) {
                if (fs.type() == Primitive.STR) {
                    entryBase.setFieldSchema(fs);
                } else {
                    errEntryFieldTypeNotStr(fn, fs.type().toString());
                }
            } else {
                errEntryNotFound(fn);
            }
        }
    }

    private void errEntryNotFound(String entry) {
        errs.addErr(new EntryNotFound(curNameable.name(), entry));
    }

    private void errEntryFieldTypeNotStr(String entry, String errType) {
        errs.addErr(new EntryFieldTypeNotStr(curNameable.name(), entry, errType));
    }

    private boolean resolveKey(Structural structural, KeySchema key) {
        List<FieldSchema> obj = new ArrayList<>();
        boolean ok = true;
        for (String name : key.name()) {
            FieldSchema field = structural.findField(name);
            obj.add(field);
            if (field == null) {
                errKeyNotFound(name);
                ok = false;
            }
        }
        if (ok) {
            key.setObj(obj);
        }
        return ok;
    }

    /**
     * 类型要符合规则：
     * <1>如果是多key，则每个都要是基本类型
     * <2>如果是一个key，除了基本类型还可以是struct，但struct里每个都要是基本类型。
     * 以上基本类型只包括bool，int，long，str
     *
     * @param key primary or unique key
     */
    private void checkPrimaryOrUniqKey(KeySchema key) {
        List<FieldSchema> fields = key.obj();
        if (fields.size() == 1) {
            FieldSchema field = fields.get(0);
            FieldType type = field.type();

            String fn = field.name();
            String tn = type.toString();

            switch (type) {
                case Container _ -> {
                    errKeyTypeNotSupport(fn, tn);
                }
                case Primitive _ -> {
                    if (checkErrFieldAsKey(field)) {
                        errKeyTypeNotSupport(fn, tn);
                    }
                }
                case StructRef structRef -> {
                    Fieldable fieldable = structRef.obj();

                    switch (fieldable) {
                        case InterfaceSchema _ -> {
                            errKeyTypeNotSupport(fn, tn);
                        }
                        case StructSchema structSchema -> {
                            if (checkErrFieldListAsKey(structSchema.fields())) {
                                errKeyTypeNotSupport(fn, tn);
                            }
                        }
                    }
                }
            }
        } else {
            for (FieldSchema field : fields) {
                String fn = field.name();
                String tn = field.type().toString();
                if (checkErrFieldAsKey(field)) {
                    errKeyTypeNotSupport(fn, tn);
                }
            }
        }

    }


    private boolean checkErrFieldListAsKey(List<FieldSchema> fields) {
        return fields.stream().anyMatch(this::checkErrFieldAsKey);
    }

    private boolean checkErrFieldAsKey(FieldSchema field) {
        FieldType type = field.type();
        return !(type == BOOL || type == INT || type == LONG || type == Primitive.STR);
    }


    private void errKeyNotFound(String key) {
        errs.addErr(new KeyNotFound(curNameable.name(), key));
    }

    private void errKeyTypeNotSupport(String field, String errType) {
        errs.addErr(new KeyTypeNotSupport(curNameable.name(), field, errType));
    }

    ////////////////////////////////////////////////////////////////////
    private void step3_resolveAllForeignKeys() {
        resolve_structural(this::resolveForeignKeys);
    }

    private void resolveForeignKeys(Structural structural) {
        for (ForeignKeySchema foreignKey : structural.foreignKeys()) {
            resolveForeignKey(structural, foreignKey);
        }
    }

    private void resolveForeignKey(Structural structural, ForeignKeySchema foreignKey) {
        // 解析映射到的table
        boolean err = false;
        String refTable = foreignKey.refTable();
        TableSchema refTableSchema = findTableInLocalThenGlobal(refTable);
        if (refTableSchema != null) {
            foreignKey.setRefTableSchema(refTableSchema);
        } else {
            errRefTableNotFound(foreignKey.name(), refTable);
            err = true;
        }

        // 解析 local key
        KeySchema localKey = foreignKey.key();
        if (!resolveKey(structural, localKey)) {
            err = true;
        }

        if (err) {
            return;
        }

        // 解析 ref key
        switch (foreignKey.refKey()) {

            // 不配具体ref到的key，则映射到主键，只检测type相符
            case RefKey.RefPrimary _ -> {
                checkLocalAndRemoteTypeMatch(foreignKey, localKey, refTableSchema.primaryKey());
            }

            // 配置ref到的key，则必须映射到唯一键，并检测type相符，如果相符则记录结果到ref key中
            case RefKey.RefUniq refUniq -> {
                KeySchema remoteKey = refUniq.key();
                KeySchema uk = refTableSchema.findUniqueKey(remoteKey);
                if (uk != null) {
                    if (checkLocalAndRemoteTypeMatch(foreignKey, localKey, remoteKey)) {
                        remoteKey.setObj(uk.obj());
                    }
                } else {
                    errRefTableKeyNotUniq(foreignKey.name(), refTable, refUniq.key().name().toString());
                }
            }

            // listRef为了简单，不支持MultiKey
            case RefKey.RefList refList -> {
                if (localKey.name().size() != 1) {
                    errListRefMultiKeyNotSupport(foreignKey.name(), localKey.name().toString());
                    return;
                }

                KeySchema remoteKey = refList.key();
                // 这里remote应该向local的type看齐，保持一致
                checkLocalAndRemoteTypeMatch(foreignKey, localKey, remoteKey);
            }
        }
    }

    private boolean checkLocalAndRemoteTypeMatch(ForeignKeySchema foreignKey, KeySchema localKey, KeySchema remoteKey) {
        List<FieldSchema> localFields = localKey.obj();
        List<FieldSchema> remoteFields = remoteKey.obj();
        if (remoteFields == null) {
            return false;
        }

        if (localFields.size() != remoteFields.size()) {
            errRefLocalKeyRemoteKeyCountNotMatch(foreignKey.toString());
            return false;
        }

        boolean ok = true;
        int len = localFields.size();
        for (int i = 0; i < len; i++) {
            FieldSchema local = localFields.get(i);
            FieldSchema remote = remoteFields.get(i);

            if (!local.type().equals(remote.type())) {
                errRefLocalKeyRemoteKeyTypeNotMatch(foreignKey.name(),
                        local.type().toString(), remote.type().toString());
                ok = false;
            }
        }
        return ok;
    }

    private void errRefTableNotFound(String foreignKey, String errRefTable) {
        errs.addErr(new RefTableNotFound(curNameable.name(), foreignKey, errRefTable));
    }

    private void errRefTableKeyNotUniq(String foreignKey, String refTable, String notUniqRefKey) {
        errs.addErr(new RefTableKeyNotUniq(curNameable.name(), foreignKey, refTable, notUniqRefKey));
    }

    private void errListRefMultiKeyNotSupport(String foreignKey, String errMultiKey) {
        errs.addErr(new ListRefMultiKeyNotSupport(curNameable.name(), foreignKey, errMultiKey));
    }

    private void errRefLocalKeyRemoteKeyCountNotMatch(String foreignKey) {
        errs.addErr(new RefLocalKeyRemoteKeyCountNotMatch(curNameable.name(), foreignKey));
    }

    private void errRefLocalKeyRemoteKeyTypeNotMatch(String foreignKey, String localType, String remoteType) {
        errs.addErr(new RefLocalKeyRemoteKeyTypeNotMatch(curNameable.name(), foreignKey, localType, remoteType));
    }
}

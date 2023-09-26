package configgen.schema;

import configgen.schema.EntryType.EntryBase;
import configgen.schema.cfg.Cfgs;

import java.nio.file.Path;
import java.util.*;

import static configgen.schema.SchemaErrs.*;
import static configgen.schema.FieldFormat.AutoOrPack;
import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldFormat.Sep;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;

/**
 * 把CfgSchema内部关系给解决了，如果有Errs，就表明有内部矛盾
 */
public final class CfgSchemaResolver {
    private final CfgSchema cfg;
    private final SchemaErrs errs;
    private Nameable curNameable;
    private StructSchema curImpl;
    private boolean isInCurImpl;

    public static SchemaErrs resolve(CfgSchema schema) {
        SchemaErrs errs = SchemaErrs.of();
        new CfgSchemaResolver(schema, errs).resolve();
        return errs;
    }

    public CfgSchemaResolver(CfgSchema cfg, SchemaErrs errs) {
        this.cfg = cfg;
        this.errs = errs;
    }

    public void resolve() {
        step0_checkNameConflict();
        step1_resolveAllFields();
        step2_resolveEachNameable();
        step3_resolveAllForeignKeys();
        step4_checkAllChainedSepFmt();
    }

    /**
     * 如果所有的cfg都配置在一个文件里，那么不需要检测
     * 如果配在每个文件夹下，虽然代码可以按从interface scope -> local scope -> global scope的顺序来解析名字。
     * 但为了清晰性，我们一开始叫避免这种可能的混乱。
     */
    private void step0_checkNameConflict() {
        Set<List<String>> nameSet = new HashSet<>();
        Set<List<String>> fieldableNameSet = new HashSet<>();
        Set<String> fieldableTopNameSet = new HashSet<>();

        // 检查全局名字空间，局部名字空间各自的冲突
        for (Nameable item : cfg.items()) {
            List<String> names = Arrays.asList(item.name().split("\\."));
            if (!nameSet.add(names)) {
                errs.addErr(new NameConflict(item.name()));
            }
            if (item instanceof Fieldable) {
                fieldableNameSet.add(names);
                fieldableTopNameSet.add(names.get(0));
            }

            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        if (!impl.namespace().isEmpty()) {
                            errs.addErr(new ImplNamespaceNotEmpty(interfaceSchema.name(), impl.name()));
                        }
                        checkInnerNameConflict(impl);
                    }
                }
                case Structural structural -> checkInnerNameConflict(structural);
            }
        }

        // 检查局部名字空间和全局名字空间潜在的冲突
        // 1, 先检查interface的局部名字空间里，可能跟全局的冲突
        for (Nameable item : cfg.items()) {
            if (item instanceof InterfaceSchema interfaceSchema) {
                for (StructSchema impl : interfaceSchema.impls()) {
                    if (fieldableTopNameSet.contains(impl.name())) {
                        errs.addWarn(new NameMayConflictByRef(interfaceSchema.name() + "." + impl.name(), impl.name()));
                    }
                }
            }
        }

        // 2，再检查分文件存储schema，可能导致的混乱
        for (List<String> name : fieldableNameSet) {
            if (name.size() <= 1) {
                continue;
            }
            int len = name.size();
            for (int i = 1; i < len; i++) {
                List<String> sub = name.subList(i, len);
                if (fieldableNameSet.contains(sub)) {
                    String name1 = String.join(".", name);
                    String name2 = String.join(".", sub);
                    errs.addWarn(new NameMayConflictByRef(name1, name2));
                }
            }
        }

        // resolve
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
            resolveFieldType(field, field.type());
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
                errs.addErr(new TypeStructNotFound(ctx(), field.name(), name));
            }
        }
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
            errs.addErr(new EnumRefNotFound(ctx(), enumRef));
        }

        String def = sInterface.defaultImpl();
        if (!def.isEmpty()) {
            StructSchema defImpl = sInterface.findImpl(def);
            if (defImpl != null) {
                sInterface.setDefaultImplStruct(defImpl);
            } else {
                errs.addErr(new DefaultImplNotFound(ctx(), def));
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
                    errs.addErr(new EntryFieldTypeNotStr(ctx(), fn, fs.type().toString()));
                }
            } else {
                errs.addErr(new EntryNotFound(ctx(), fn));
            }
        }
    }

    private boolean resolveKey(Structural structural, KeySchema key) {
        List<FieldSchema> obj = new ArrayList<>();
        boolean ok = true;
        for (String name : key.name()) {
            FieldSchema field = structural.findField(name);
            obj.add(field);
            if (field == null) {
                errs.addErr(new KeyNotFound(ctx(), name));
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
                case ContainerType _ -> {
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
            errs.addErr(new RefTableNotFound(ctx(), foreignKey.name(), refTable));
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
                    errs.addErr(new RefTableKeyNotUniq(ctx(), foreignKey.name(),
                            refTable, refUniq.key().name().toString()));
                }
            }

            // listRef为了简单，不支持MultiKey
            case RefKey.RefList refList -> {
                if (localKey.name().size() != 1) {
                    errs.addErr(new ListRefMultiKeyNotSupport(ctx(), foreignKey.name(), localKey.name().toString()));
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
            errs.addErr(new RefLocalKeyRemoteKeyCountNotMatch(ctx(), foreignKey.toString()));
            return false;
        }

        boolean ok = true;
        int len = localFields.size();
        for (int i = 0; i < len; i++) {
            FieldSchema local = localFields.get(i);
            FieldSchema remote = remoteFields.get(i);

            switch (local.type()) {
                case FList flist -> {
                    if (len != 1 || !checkSimpleTypeMatch(flist.item(), remote.type())) {
                        ok = false;
                    }
                }
                case FMap fmap -> {
                    if (len != 1 || !checkSimpleTypeMatch(fmap.value(), remote.type())) {
                        ok = false;
                    }
                }
                default -> {
                    if (!checkSimpleTypeMatch(local.type(), remote.type())) {
                        ok = false;
                    }
                }
            }
            if (!ok) {
                errs.addErr(new RefLocalKeyRemoteKeyTypeNotMatch(ctx(), foreignKey.name(),
                        local.type().toString(), remote.type().toString()));
            }
        }
        return ok;
    }

    private boolean checkSimpleTypeMatch(FieldType local, FieldType remote) {
        switch (local) {
            case Primitive primitive -> {
                return primitive.equals(remote);
            }
            case StructRef structRef -> {
                return remote instanceof StructRef ref && structRef.obj() == ref.obj();
            }
            default -> throw new IllegalStateException("Unexpected value: " + local);
        }
    }

    private void step4_checkAllChainedSepFmt() {
        for (Nameable item : cfg.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    // 为了简单和一致性，在interface的impl上不支持配置fmt
                    for (StructSchema impl : interfaceSchema.impls()) {
                        if (impl.fmt() != AUTO) {
                            errs.addErr(new ImplFmtNotSupport(interfaceSchema.name(), impl.name(), impl.fmt().toString()));
                        }
                    }
                }
                case StructSchema structSchema -> {
                    // 为简单，只有field都是简单类型的struct可以配置了sep
                    if (structSchema.fmt() instanceof Sep) {
                        boolean isAllFieldsPrimitive = true;
                        for (FieldSchema field : structSchema.fields()) {
                            if (!(field.type() instanceof Primitive)) {
                                isAllFieldsPrimitive = false;
                                break;
                            }
                        }
                        if (!isAllFieldsPrimitive) {
                            errs.addErr(new SepFmtStructHasNoPrimitive(structSchema.name()));
                        }
                    }
                }
                case TableSchema _ -> {
                }
            }
        }
        resolve_structural(this::checkFieldFmts);
    }

    private void checkFieldFmts(Structural structural) {
        for (FieldSchema field : structural.fields()) {
            checkFieldFmt(field);
        }
    }

    private void checkFieldFmt(FieldSchema field) {
        FieldType type = field.type();
        FieldFormat fmt = field.fmt();
        switch (type) {

            case Primitive _ -> {
                if (fmt != AUTO) {
                    errTypeFmtNotCompatible(field);
                }
            }
            case StructRef _ -> {
                if (!(fmt instanceof AutoOrPack) && !(fmt instanceof Sep)) {
                    errTypeFmtNotCompatible(field);
                }
            }
            case FList flist -> {
                if (fmt == AUTO) {
                    errTypeFmtNotCompatible(field);
                }
                if (fmt instanceof Sep sep && flist.item() instanceof StructRef structRef) {
                    if (structRef.obj().fmt() instanceof Sep sep2 && sep.sep() == sep2.sep() ||
                            structRef.obj().fmt() == PACK && sep.sep() == ',') {
                        errs.addErr(new ListStructSepEqual(ctx(), field.name()));
                    }
                }
            }
            case FMap _ -> {
                if (fmt == AUTO || fmt instanceof Sep) {
                    errTypeFmtNotCompatible(field);
                }
            }
        }
    }

    private void errTypeFmtNotCompatible(FieldSchema field) {
        errs.addErr(new TypeFmtNotCompatible(ctx(), field.name(), field.type().toString(), field.fmt().toString()));
    }

    public static void main(String[] args) {
        CfgSchema cfg = Cfgs.readFrom(Path.of("config.cfg"), true);
        SchemaErrs errs = CfgSchemaResolver.resolve(cfg);
        errs.print();

    }
}

package configgen.schema;

import configgen.schema.EntryType.EntryBase;

import java.util.*;

import static configgen.schema.FieldFormat.AutoOrPack;
import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldFormat.Sep;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.schema.SchemaErrs.*;

/**
 * 把CfgSchema内部关系给解决了，如果有Errs，就表明有内部矛盾
 */
public final class CfgSchemaResolver {
    private final CfgSchema cfgSchema;
    private final SchemaErrs errs;
    private Nameable curTopNameable;
    private Nameable curNameable;


    public CfgSchemaResolver(CfgSchema cfg, SchemaErrs errs) {
        this.cfgSchema = cfg;
        this.errs = errs;
    }

    public void resolve() {
        step0_setImplInterface();
        step0_checkNameConflict();
        step1_resolveAllFields();
        step2_resolveEachNameable();
        step3_resolveAllForeignKeys();
        step4_checkAllChainedSepFmt();
        step5_checkUnusedFieldable();
        if (errs.errs().isEmpty()) {
            cfgSchema.setResolved();
        }
    }

    private void step0_setImplInterface() {
        for (Nameable item : cfgSchema.items()) {
            if (item instanceof InterfaceSchema sInterface) {
                for (StructSchema impl : sInterface.impls()) {
                    impl.setNullableInterface(sInterface);
                }
            }
        }
    }


    /**
     * 如果所有的cfg都配置在一个文件里，那么不需要检测
     * 如果配在每个文件夹下，虽然代码可以按从interface scope -> local scope -> global scope的顺序来解析名字。
     * 但为了清晰性，我们一开始就避免这种可能的混乱。
     */
    private void step0_checkNameConflict() {
        Set<List<String>> nameSet = new HashSet<>();
        Set<List<String>> fieldableNameSet = new HashSet<>();
        Set<String> fieldableTopNameSet = new HashSet<>();

        // 检查全局名字空间，局部名字空间各自的冲突
        for (Nameable item : cfgSchema.items()) {
            List<String> names = Arrays.asList(item.name().split("\\."));
            if (!nameSet.add(names)) {
                errs.addErr(new NameConflict(item.name()));
            }
            if (item instanceof Fieldable) {
                fieldableNameSet.add(names);
                fieldableTopNameSet.add(names.getFirst());
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
        for (Nameable item : cfgSchema.items()) {
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
        Map<String, Nameable> itemMap = new HashMap<>();
        Map<String, Fieldable> structMap = new HashMap<>();
        Map<String, TableSchema> tableMap = new HashMap<>();
        for (Nameable item : cfgSchema.items()) {
            itemMap.put(item.name(), item);
            switch (item) {
                case Fieldable fieldable -> structMap.put(fieldable.name(), fieldable);
                case TableSchema table -> tableMap.put(table.name(), table);
            }
        }
        cfgSchema.setMap(itemMap, structMap, tableMap);
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

    private void resolve_structural(ForeachSchema.StructuralVisitor visitor) {
        ForeachSchema.foreachStructural(structural -> {
            curNameable = structural;
            curTopNameable = curNameable instanceof StructSchema s && s.nullableInterface() != null ?
                    s.nullableInterface() : curNameable;

            visitor.visit(structural);

            curNameable = null;
            curTopNameable = null;
        }, cfgSchema);
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
            case Primitive ignored -> {
            }
            case StructRef structRef -> {
                String name = structRef.name();
                // interface里找
                if (curTopNameable instanceof InterfaceSchema sInterface) {
                    StructSchema obj = sInterface.findImpl(name);
                    if (obj != null) {
                        structRef.setObj(obj);
                        return;
                    }
                }

                // 本模块找
                String namespace = curTopNameable.namespace();
                if (!namespace.isEmpty()) {
                    String fullName = Nameable.makeName(namespace, name);
                    Fieldable obj = cfgSchema.findFieldable(fullName);
                    if (obj != null) {
                        structRef.setObj(obj);
                        return;
                    }
                }

                // 全局找
                Fieldable obj = cfgSchema.findFieldable(name);
                if (obj != null) {
                    structRef.setObj(obj);
                    return;
                }
                errs.addErr(new TypeStructNotFound(ctx(), field.name(), name));
            }
        }
    }


    private String ctx() {
        return curNameable.fullName();
    }

    ////////////////////////////////////////////////////////////////////
    private void step2_resolveEachNameable() {
        for (Nameable item : cfgSchema.items()) {
            curNameable = item;
            curTopNameable = item;
            switch (item) {
                case StructSchema ignored -> {
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

        if (sInterface.impls().isEmpty()) {
            errs.addErr(new InterfaceImplEmpty(ctx()));
        }

        String def = sInterface.defaultImpl();
        if (!def.isEmpty()) {
            StructSchema defImpl = sInterface.findImpl(def);
            if (defImpl != null) {
                sInterface.setNullableDefaultImplStruct(defImpl);
            } else {
                errs.addErr(new DefaultImplNotFound(ctx(), def));
            }
        }
    }

    private TableSchema findTableInLocalThenGlobal(String name) {
        // 本模块找
        String namespace = curTopNameable.namespace();
        if (!namespace.isEmpty()) {
            String fullName = Nameable.makeName(namespace, name);
            TableSchema table = cfgSchema.findTable(fullName);
            if (table != null) {
                return table;
            }
        }

        // 全局找
        return cfgSchema.findTable(name);
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
        if (HasBlock.hasBlock(table)) {
            String firstField = table.fields().getFirst().name();
            if (!primaryKey.fields().contains(firstField)) {
                errs.addErr(new BlockTableFirstFieldNotInPrimaryKey(table.name()));
            }
        }
    }

    private void resolveEntry(TableSchema table, EntryType entry) {
        if (entry instanceof EntryBase entryBase) {
            String fn = entryBase.field();
            FieldSchema fs = table.findField(fn);
            if (fs != null) {
                if (fs.type() == STRING) {
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
        for (String name : key.fields()) {
            FieldSchema field = structural.findField(name);
            obj.add(field);
            if (field == null) {
                errs.addErr(new KeyNotFound(ctx(), name));
                ok = false;
            }
        }
        if (ok) {
            key.setFieldSchemas(obj);
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
        List<FieldSchema> fields = key.fieldSchemas();
        if (fields.size() == 1) {
            FieldSchema field = fields.getFirst();
            FieldType type = field.type();

            String fn = field.name();
            String tn = type.toString();

            switch (type) {
                case ContainerType ignored -> {
                    errKeyTypeNotSupport(fn, tn);
                }
                case Primitive ignored -> {
                    if (checkErrFieldAsKey(field)) {
                        errKeyTypeNotSupport(fn, tn);
                    }
                }
                case StructRef structRef -> {
                    Fieldable fieldable = structRef.obj();

                    switch (fieldable) {
                        case InterfaceSchema ignored -> {
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
        return !(type == BOOL || type == INT || type == LONG || type == STRING);
    }

    private void errKeyTypeNotSupport(String field, String errType) {
        errs.addErr(new KeyTypeNotSupport(ctx(), field, errType));
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

        foreignKey.setKeyIndices(FindFieldIndex.findFieldIndices(structural, foreignKey.key()));

        // 解析 ref key
        switch (foreignKey.refKey()) {

            // 不配具体ref到的key，则映射到主键，只检测type相符
            case RefKey.RefPrimary ignored -> {
                checkLocalAndRemoteTypeMatch(foreignKey, localKey, refTableSchema.primaryKey());
            }

            // 配置ref到的key，则必须映射到唯一键，并检测type相符，如果相符则记录结果到ref key中
            case RefKey.RefUniq refUniq -> {
                KeySchema remoteKey = refUniq.key();
                KeySchema uk = refTableSchema.findUniqueKey(remoteKey);
                if (uk != null) {
                    remoteKey.setFieldSchemas(uk.fieldSchemas());
                    checkLocalAndRemoteTypeMatch(foreignKey, localKey, remoteKey);

                } else {
                    errs.addErr(new RefTableKeyNotUniq(ctx(), foreignKey.name(),
                            refTable, refUniq.key().fields()));
                }
            }

            // listRef为了简单，不支持MultiKey
            case RefKey.RefList refList -> {
                if (localKey.fields().size() != 1) {
                    errs.addErr(new ListRefMultiKeyNotSupport(ctx(), foreignKey.name(), localKey.fields()));
                    return;
                }

                KeySchema remoteKey = refList.key();
                if (remoteKey.fields().size() != 1) {
                    errs.addErr(new ListRefMultiKeyNotSupport(ctx(), foreignKey.name(), remoteKey.fields()));
                    return;
                }

                FieldSchema remoteField = refTableSchema.findField(remoteKey.fields().getFirst());
                if (remoteField != null) {
                    remoteKey.setFieldSchemas(List.of(remoteField));
                    // 这里remote应该向local的type看齐，保持一致
                    checkLocalAndRemoteTypeMatch(foreignKey, localKey, remoteKey);

                } else {
                    errs.addErr(new RefTableKeyNotUniq(ctx(), foreignKey.name(),
                            refTable, refList.key().fields()));
                }
            }
        }
    }


    private void checkLocalAndRemoteTypeMatch(ForeignKeySchema foreignKey, KeySchema localKey, KeySchema remoteKey) {
        List<FieldSchema> localFields = localKey.fieldSchemas();
        List<FieldSchema> remoteFields = remoteKey.fieldSchemas();

        if (localFields.size() != remoteFields.size()) {
            errs.addErr(new RefLocalKeyRemoteKeyCountNotMatch(ctx(), foreignKey.toString()));
            return;
        }

        boolean ok = true;
        int len = localFields.size();
        for (int i = 0; i < len; i++) {
            FieldSchema local = localFields.get(i);
            FieldSchema remote = remoteFields.get(i);

            switch (local.type()) {
                case ContainerType containerType -> {
                    if (foreignKey.refKey() instanceof RefKey.RefSimple refSimple) {
                        if (refSimple.nullable()) {
                            errs.addErr(new RefContainerNullable(ctx(), foreignKey.name()));
                        }
                    }

                    switch (containerType) {
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
                    }

                }

                case SimpleType simpleType -> {
                    if (!checkSimpleTypeMatch(simpleType, remote.type())) {
                        ok = false;
                    }
                }
            }
            if (!ok) {
                errs.addErr(new RefLocalKeyRemoteKeyTypeNotMatch(ctx(), foreignKey.name(),
                        local.type().toString(), remote.type().toString()));
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkSimpleTypeMatch(SimpleType local, FieldType remote) {
        switch (local) {
            case Primitive primitive -> {
                return primitive.equals(remote);
            }
            case StructRef structRef -> {
                return remote instanceof StructRef ref && structRef.obj() == ref.obj();
            }
        }
    }

    private void step4_checkAllChainedSepFmt() {
        for (Nameable item : cfgSchema.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    // 为了简单和一致性，在interface的impl上不支持配置fmt
                    // 因为如果配置了pack或sep，则这第一列就不是impl的名字了，不一致。
                    for (StructSchema impl : interfaceSchema.impls()) {
                        if (impl.fmt() != AUTO) {
                            errs.addErr(new ImplFmtNotSupport(interfaceSchema.name(), impl.name(), impl.fmt().toString()));
                        }
                    }
                }
                case StructSchema structSchema -> {
                    // 为简单，只有field都是primitive类型的struct可以配置了sep
                    if (structSchema.fmt() instanceof Sep) {
                        boolean isAllFieldsPrimitive = true;
                        for (FieldSchema field : structSchema.fields()) {
                            if (!(field.type() instanceof Primitive)) {
                                isAllFieldsPrimitive = false;
                                break;
                            }
                        }
                        if (!isAllFieldsPrimitive) {
                            errs.addErr(new SepFmtStructHasUnPrimitiveField(structSchema.name()));
                        }
                    }
                }
                case TableSchema ignored -> {
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

            case Primitive ignored -> {
                if (fmt != AUTO) {
                    errTypeFmtNotCompatible(field);
                }
            }
            case StructRef ignored -> {
                if (!(fmt instanceof AutoOrPack)) {
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
            case FMap ignored -> {
                if (fmt == AUTO || fmt instanceof Sep) {
                    errTypeFmtNotCompatible(field);
                }
            }
        }
    }

    private void errTypeFmtNotCompatible(FieldSchema field) {
        errs.addErr(new TypeFmtNotCompatible(ctx(), field.name(), field.type().toString(), field.fmt().toString()));
    }

    private void step5_checkUnusedFieldable() {
        List<FieldSchema> needToCheck = new ArrayList<>();
        for (TableSchema table : cfgSchema.tableMap().values()) {
            needToCheck.addAll(table.fields());
        }

        Set<String> collectedFieldableSet = new HashSet<>();
        while (!needToCheck.isEmpty()) {
            Map<String, Fieldable> needToCheckFieldables = new HashMap<>();
            for (FieldSchema field : needToCheck) {
                switch (field.type()) {
                    case StructRef structRef -> {
                        if (structRef.obj() != null) {
                            needToCheckFieldables.put(structRef.obj().name(), structRef.obj());
                        }
                    }
                    case FList fList -> {
                        if (fList.item() instanceof StructRef structRef) {
                            if (structRef.obj() != null) {
                                needToCheckFieldables.put(structRef.obj().name(), structRef.obj());
                            }
                        }
                    }
                    case FMap fMap -> {
                        if (fMap.key() instanceof StructRef structRef) {
                            if (structRef.obj() != null) {
                                needToCheckFieldables.put(structRef.obj().name(), structRef.obj());
                            }
                        }
                        if (fMap.value() instanceof StructRef structRef) {
                            if (structRef.obj() != null) {
                                needToCheckFieldables.put(structRef.obj().name(), structRef.obj());
                            }
                        }
                    }
                    default -> {
                    }
                }
            }


            needToCheck.clear();
            for (Fieldable f : needToCheckFieldables.values()) {
                boolean notCheckedBefore = collectedFieldableSet.add(f.name());
                if (notCheckedBefore) {
                    switch (f) {
                        case InterfaceSchema interfaceSchema -> {
                            for (StructSchema impl : interfaceSchema.impls()) {
                                needToCheck.addAll(impl.fields());
                            }
                        }
                        case StructSchema structSchema -> {
                            needToCheck.addAll(structSchema.fields());
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Fieldable> e : cfgSchema.fieldableMap().entrySet()) {
            if (!collectedFieldableSet.contains(e.getKey())) {
                switch (e.getValue()) {
                    case InterfaceSchema ignored -> errs.addWarn(new InterfaceNotUsed(e.getKey()));
                    case StructSchema ignored -> errs.addWarn(new StructNotUsed(e.getKey()));
                }
            }
        }
    }
}

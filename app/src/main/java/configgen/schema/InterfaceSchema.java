package configgen.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldType.Primitive.*;

public final class InterfaceSchema implements Fieldable, Nameable {
    private final String name;

    /**
     * 必须索引到一个枚举表中，枚举表中要包含等价于但更策划易读的impls信息，因为策划不会去看schema
     */
    private final String enumRef;
    /**
     * 可以为空，如果设置了，则有以下两个作用
     * <1> 允许格子为空，此时会选择这个struct
     * <2> 可以不加此类型名前缀
     */
    private final String defaultImpl;

    private final FieldFormat fmt;
    private final Metadata meta;

    /**
     * 所有的实现都放在这
     */
    private final List<StructSchema> impls;

    private TableSchema enumRefTable;
    private StructSchema nullableDefaultImplStruct;

    public InterfaceSchema(String name, String enumRef, String defaultImpl,
                           FieldFormat fmt, Metadata meta,
                           List<StructSchema> impls) {
        this.name = name;
        this.enumRef = enumRef;
        this.defaultImpl = defaultImpl;
        this.fmt = fmt;
        this.meta = meta;
        this.impls = impls;

        Objects.requireNonNull(name);
        Objects.requireNonNull(enumRef);
        Objects.requireNonNull(defaultImpl);
        Objects.requireNonNull(fmt);
        Objects.requireNonNull(impls);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("interface name empty");
        }
        if (fmt != AUTO && fmt != PACK) {
            throw new IllegalArgumentException("interface fmt must be auto/pack");
        }
    }

    @Override
    public InterfaceSchema copy() {
        List<StructSchema> implsCopy = new ArrayList<>(impls.size());
        for (StructSchema impl : impls) {
            implsCopy.add(impl.copy());
        }
        return new InterfaceSchema(name, enumRef, defaultImpl, fmt, meta.copy(), implsCopy);
    }

    public String name() {
        return name;
    }

    public FieldFormat fmt() {
        return fmt;
    }

    public String enumRef() {
        return enumRef;
    }

    public String defaultImpl() {
        return defaultImpl;
    }

    public Metadata meta() {
        return meta;
    }

    public List<StructSchema> impls() {
        return impls;
    }

    public StructSchema findImpl(String name) {
        for (StructSchema impl : impls) {
            if (impl.name().equals(name)) {
                return impl;
            }
        }
        return null;
    }

    public TableSchema nullableEnumRefTable() {
        return enumRefTable;
    }

    void setNullableEnumRefTable(TableSchema enumRefTable) {
        this.enumRefTable = enumRefTable;
    }

    public StructSchema nullableDefaultImplStruct() {
        return nullableDefaultImplStruct;
    }

    void setNullableDefaultImplStruct(StructSchema defaultImplStruct) {
        this.nullableDefaultImplStruct = defaultImplStruct;
    }

    /**
     * 需求：一个格子大部分情况需要配置一个bool，或数字。那就直接写0/1,或数字就行。但有时有需要这个格子里填公式，
     * 这里通过interface来支持，公式的含义和具体计算由应用自己来定义，这里只给出具体参数
     *
     * @return 此interface，是否可做为一个数字或bool的替代出现。
     */
    public boolean canBeNumberOrBool() {
        if (fmt == PACK && nullableDefaultImplStruct != null
                && nullableDefaultImplStruct.fields().size() == 1) {
            FieldType type = nullableDefaultImplStruct.fields().getFirst().type();
            return numberOrBoolTypes.contains(type);
        }
        return false;
    }

    private static final Set<FieldType> numberOrBoolTypes = Set.of(BOOL, INT, LONG, FLOAT);

    @Override
    public String toString() {
        return "InterfaceSchema{" +
                "name='" + name + '\'' +
                ", enumRef='" + enumRef + '\'' +
                ", defaultImpl='" + defaultImpl + '\'' +
                ", fmt=" + fmt +
                ", meta=" + meta +
                ", impls=" + impls +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterfaceSchema that = (InterfaceSchema) o;
        return Objects.equals(name, that.name) && Objects.equals(enumRef, that.enumRef) && Objects.equals(defaultImpl, that.defaultImpl) && Objects.equals(fmt, that.fmt) && Objects.equals(meta, that.meta) && Objects.equals(impls, that.impls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enumRef, defaultImpl, fmt, meta, impls);
    }


}

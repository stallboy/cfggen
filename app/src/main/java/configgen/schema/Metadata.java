package configgen.schema;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;

import static configgen.schema.Metadata.MetaTag.*;


public record Metadata(SequencedMap<String, MetaValue> data) {

    public sealed interface MetaValue {
    }

    public enum MetaTag implements MetaValue {
        TAG
    }

    public record MetaInt(int value) implements MetaValue {
    }

    public record MetaFloat(float value) implements MetaValue {
    }

    public record MetaStr(String value) implements MetaValue {
    }

    public static Metadata of() {
        return new Metadata(new LinkedHashMap<>());
    }

    public Metadata copy() {
        return new Metadata(new LinkedHashMap<>(data));
    }

    public Metadata {
        Objects.requireNonNull(data);
    }

    public MetaValue get(String name) {
        return data.get(name);
    }

    public String getStr(String name, String def) {
        MetaValue metaValue = data.get(name);
        if (metaValue instanceof MetaStr str) {
            return str.value;
        }
        return def;
    }

    public boolean isJson() {
        return hasTag(JSON);
    }

    public void putTag(String tag) {
        if (reserved.contains(tag)) {
            throw new IllegalArgumentException(String.format("'%s' reserved", tag));
        }
        MetaValue old = data.putLast(tag, TAG);
        if (old != null) {
            throw new IllegalArgumentException(String.format("'%s' duplicated", tag));
        }
    }

    public boolean hasTag(String tag) {
        MetaValue value = data.get(tag);
        return value == TAG;
    }

    public void putHasRef(boolean hasRef) {
        data.putLast(HAS_REF, hasRef ? new MetaInt(1) : new MetaInt(0));
    }

    public MetaValue getHasRef() {
        return data.get(HAS_REF);
    }

    public void putHasBlock(boolean hasBlock) {
        data.putLast(HAS_BLOCK, hasBlock ? new MetaInt(1) : new MetaInt(0));
    }

    public MetaValue getHasBlock() {
        return data.get(HAS_BLOCK);
    }

    public void putSpan(int value) {
        data.putLast(SPAN, new MetaInt(value));
    }

    public MetaValue getSpan() {
        return data.get(SPAN);
    }


    // 使用下划线开头，表示这个meta数据是private的，内部用。
    private static final String COMMENT = "_comment";
    private static final String SPAN = "_span";
    private static final String HAS_REF = "_hasRef";
    private static final String HAS_BLOCK = "_hasBlock";


    private static final String JSON = "json"; // 这个表用json来分文件存
    private static final String NULLABLE = "nullable";
    private static final String ENUM_REF = "enumRef";
    private static final String DEFAULT_IMPL = "defaultImpl";
    private static final String ENTRY = "entry";
    private static final String ENUM = "enum";
    private static final String COLUMN_MODE = "columnMode";
    private static final String PACK = "pack";
    private static final String SEP = "sep";
    private static final String FIX = "fix";
    private static final String BLOCK = "block";

    private static final Set<String> reserved = Set.of(COMMENT, SPAN, HAS_REF, HAS_BLOCK,
            JSON, NULLABLE, ENUM_REF, DEFAULT_IMPL, ENTRY, ENUM, COLUMN_MODE, PACK, SEP, FIX, BLOCK);

    public String getComment() {
        if (data.get(COMMENT) instanceof MetaStr str) {
            return str.value;
        }
        return "";
    }

    public String putComment(String comment) {
        MetaValue value = data.putLast(COMMENT, new MetaStr(comment));
        if (value instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public String removeComment() {
        MetaValue obj = data.remove(COMMENT);
        if (obj instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public void putNullable() {
        data.putFirst(NULLABLE, TAG);
    }

    public boolean removeNullable() {
        return data.remove(NULLABLE) != null;
    }

    public void putEnumRef(String enumRef) {
        data.putFirst(ENUM_REF, new MetaStr(enumRef));
    }

    public String removeEnumRef() {
        MetaValue enumRef = data.remove(ENUM_REF);
        if (enumRef instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public void putDefaultImpl(String defaultImpl) {
        data.putFirst(DEFAULT_IMPL, new MetaStr(defaultImpl));
    }

    public String removeDefaultImpl() {
        MetaValue defaultImpl = data.remove(DEFAULT_IMPL);
        if (defaultImpl instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public void putEntry(EntryType entry) {
        switch (entry) {
            case EntryType.ENo.NO -> {
            }
            case EntryType.EEntry anEntry -> data.putFirst(ENTRY, new MetaStr(anEntry.field()));
            case EntryType.EEnum anEnum -> data.putFirst(ENUM, new MetaStr(anEnum.field()));
        }
    }

    public EntryType removeEntry() {
        MetaValue entry = data.remove(ENTRY);
        if (entry instanceof MetaStr ms) {
            return new EntryType.EEntry(ms.value());
        }

        MetaValue anEnum = data.remove(ENUM);
        if (anEnum instanceof MetaStr ms) {
            return new EntryType.EEnum(ms.value());
        }
        return EntryType.ENo.NO;
    }

    public void putColumnMode() {
        data.putFirst(COLUMN_MODE, TAG);
    }

    public boolean removeColumnMode() {
        return data.remove(COLUMN_MODE) != null;
    }

    public void putFmt(FieldFormat fmt) {
        switch (fmt) {
            case FieldFormat.AutoOrPack.AUTO -> {
            }
            case FieldFormat.AutoOrPack.PACK -> data.putFirst(PACK, TAG);
            case FieldFormat.Sep sep -> data.putFirst(SEP, new MetaStr(String.valueOf(sep.sep())));
            case FieldFormat.Fix fix -> data.putFirst(FIX, new MetaInt(fix.count()));
            case FieldFormat.Block block -> data.putFirst(BLOCK, new MetaInt(block.fix()));
        }
    }

    public FieldFormat removeFmt() {
        if (data.remove(PACK) != null) {
            return FieldFormat.AutoOrPack.PACK;
        }

        MetaValue sep = data.remove(SEP);
        if (sep instanceof MetaStr ms) {
            return new FieldFormat.Sep(ms.value().charAt(0));
        }

        MetaValue fix = data.remove(FIX);
        if (fix instanceof MetaInt mi) {
            return new FieldFormat.Fix(mi.value());
        }

        MetaValue block = data.remove(BLOCK);
        if (block instanceof MetaInt mi) {
            return new FieldFormat.Block(mi.value());
        }

        return FieldFormat.AutoOrPack.AUTO;
    }


}

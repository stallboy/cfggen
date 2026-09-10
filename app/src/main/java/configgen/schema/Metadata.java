package configgen.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public record MetaComment(CommentData comment) implements MetaValue {
    }

    public sealed interface MetaEnumValues extends MetaValue {
        record OfEmpty(List<EnumValueEmpty> values) implements MetaEnumValues {}
        record OfAssigned(List<EnumValueAssigned> values) implements MetaEnumValues {}
    }

    public record EnumValueEmpty(String name, String comment) {}
    public record EnumValueAssigned(String name, String comment, int number) {}

    public static Metadata of() {
        return new Metadata(new LinkedHashMap<>());
    }

    public Metadata copy() {
        return new Metadata(new LinkedHashMap<>(data));
    }

    public Metadata copyWithoutState() {
        LinkedHashMap<String, MetaValue> dataCopy = new LinkedHashMap<>(data);
        for (String stateTag : stateTags) {
            dataCopy.remove(stateTag);
        }
        return new Metadata(dataCopy);
    }


    public Metadata {
        Objects.requireNonNull(data);
    }

    public MetaValue get(String name) {
        return data.get(name);
    }

    public String getStr(String name, String def) {
        MetaValue metaValue = data.get(name);
        if (metaValue instanceof MetaStr(String value)) {
            return value;
        }
        return def;
    }

    public boolean isJson() {
        return hasTag(JSON);
    }

    public boolean isLowercase() {
        return hasTag(LOWER_CASE);
    }

    public boolean isMustFill() {
        return hasTag(MUST_FILL);
    }

    public boolean isRoot() {
        return hasTag(ROOT);
    }

    public boolean isSeq() {
        return hasTag(SEQ);
    }

    /**
     * enum声明的comment字段是否用text类型（默认str）。
     * tag保留在meta里不remove，CfgWriter还原enum声明时需要写回。
     */
    public boolean isCommentText() {
        return hasTag(COMMENT_TEXT);
    }

    public void putTag(String tag) {
        if (reservedTags.contains(tag)) {
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

    public void putHasMap(boolean hasMap) {
        data.putLast(HAS_MAP, hasMap ? new MetaInt(1) : new MetaInt(0));
    }

    public MetaValue getHasMap() {
        return data.get(HAS_MAP);
    }

    public void putHasText(boolean hasText) {
        data.putLast(HAS_TEXT, hasText ? new MetaInt(1) : new MetaInt(0));
    }

    public MetaValue getHasText() {
        return data.get(HAS_TEXT);
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
    private static final String HAS_MAP = "_hasMap";
    private static final String HAS_TEXT = "_hasText";
    private static final String ENUM_VALUES = "_enumValues";
    private static final String FROM_ENUM_TYPE = "_fromEnumType";
    private static final String FROM_CFG_FILEPATH = "_fromCfgFilePATH";

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
    private static final String LOWER_CASE = "lowercase";
    private static final String MUST_FILL = "mustFill";
    private static final String ROOT = "root";
    private static final String SEQ = "seq";
    private static final String COMMENT_TEXT = "commentText"; // enum声明的comment字段用text类型（默认str）

    private static final Set<String> stateTags = Set.of(SPAN, HAS_REF, HAS_BLOCK, HAS_MAP, HAS_TEXT);

    private static final Set<String> reservedTags = Set.of(COMMENT, SPAN, HAS_REF, HAS_BLOCK, HAS_MAP, HAS_TEXT,
            JSON, NULLABLE, ENUM_REF, DEFAULT_IMPL, ENTRY, ENUM, COLUMN_MODE, PACK, SEP, FIX, BLOCK,
            LOWER_CASE, MUST_FILL, ROOT, SEQ, COMMENT_TEXT, ENUM_VALUES, FROM_ENUM_TYPE, FROM_CFG_FILEPATH);

    /**
     * 保留tag（metadata）允许出现的位置，用于解析时校验：
     * 放错位置的保留tag直接报错，而不是静默忽略或被当成用户自定义filter tag。
     */
    public enum MetaPos {
        ENUM_DECL("enum声明"),
        TABLE("table"),
        INTERFACE("interface"),
        STRUCT("struct"),
        FIELD("字段"),
        FOREIGN_KEY("外键声明");

        public final String cn;

        MetaPos(String cn) {
            this.cn = cn;
        }

        @Override
        public String toString() {
            return cn;
        }
    }

    private static final Map<String, Set<MetaPos>> reservedTagPositions = Map.ofEntries(
            Map.entry(COMMENT_TEXT, Set.of(MetaPos.ENUM_DECL)),
            Map.entry(JSON, Set.of(MetaPos.TABLE)),
            Map.entry(ENTRY, Set.of(MetaPos.TABLE)),
            Map.entry(ENUM, Set.of(MetaPos.TABLE)),
            Map.entry(COLUMN_MODE, Set.of(MetaPos.TABLE)),
            Map.entry(ROOT, Set.of(MetaPos.TABLE)),
            Map.entry(ENUM_REF, Set.of(MetaPos.INTERFACE)),
            Map.entry(DEFAULT_IMPL, Set.of(MetaPos.INTERFACE)),
            Map.entry(PACK, Set.of(MetaPos.INTERFACE, MetaPos.STRUCT, MetaPos.FIELD)),
            Map.entry(SEP, Set.of(MetaPos.INTERFACE, MetaPos.STRUCT, MetaPos.FIELD)),
            Map.entry(FIX, Set.of(MetaPos.INTERFACE, MetaPos.STRUCT, MetaPos.FIELD)),
            Map.entry(BLOCK, Set.of(MetaPos.INTERFACE, MetaPos.STRUCT, MetaPos.FIELD)),
            Map.entry(LOWER_CASE, Set.of(MetaPos.FIELD)),
            Map.entry(MUST_FILL, Set.of(MetaPos.FIELD)),
            Map.entry(SEQ, Set.of(MetaPos.FIELD)),
            Map.entry(NULLABLE, Set.of(MetaPos.FIELD, MetaPos.FOREIGN_KEY)));

    /**
     * 只能是tag（不带值）的保留名。写成 (json='x') 这种带值形式时，hasTag为false，
     * 行为会静默不生效，必须报错。
     * 注意 nullable、columnMode、pack 是 remove()!=null 判定，带值也能工作，不在此列。
     */
    private static final Set<String> tagOnlyNames = Set.of(JSON, LOWER_CASE, MUST_FILL, ROOT, SEQ, COMMENT_TEXT);

    /**
     * 必须带值的保留名。裸写 (sep) 时 removeFmt 匹配不到值的类型，fmt静默退回auto，必须报错。
     */
    private static final Set<String> valueRequiredNames = Set.of(ENTRY, ENUM, ENUM_REF, DEFAULT_IMPL, SEP, FIX, BLOCK);

    public static boolean isReservedTagNotAllowedAt(String name, MetaPos pos) {
        Set<MetaPos> allowed = reservedTagPositions.get(name);
        return allowed != null && !allowed.contains(pos);
    }

    public static String allowedPositionsText(String name) {
        Set<MetaPos> allowed = reservedTagPositions.get(name);
        if (allowed == null) {
            return "";
        }
        List<String> parts = new ArrayList<>(allowed.size());
        for (MetaPos pos : allowed) {
            parts.add(pos.cn);
        }
        return String.join("、", parts);
    }

    public static boolean isTagOnlyName(String name) {
        return tagOnlyNames.contains(name);
    }

    public static boolean isValueRequiredName(String name) {
        return valueRequiredNames.contains(name);
    }

    public CommentData getComment() {
        if (data.get(COMMENT) instanceof MetaComment(CommentData cd)) {
            return cd;
        }
        return null;
    }

    public void putComment(CommentData comment) {
        data.putLast(COMMENT, new MetaComment(comment));
    }

    public CommentData removeComment() {
        MetaValue obj = data.remove(COMMENT);
        if (obj instanceof MetaComment(CommentData cd)) {
            return cd;
        }
        return null;
    }

    // enum table 的值列表
    public void putEnumValues(MetaEnumValues values) {
        data.put(ENUM_VALUES, values);
    }

    public void removeEnumValues() {
        data.remove(ENUM_VALUES);
    }

    public MetaEnumValues getEnumValues() {
        MetaValue v = data.get(ENUM_VALUES);
        return v instanceof MetaEnumValues e ? e : null;
    }

    public boolean hasEnumValues() {
        return data.get(ENUM_VALUES) instanceof MetaEnumValues;
    }

    // 来自 enum 类型（用于 CfgWriter 还原）
    public void putFromEnumType(String enumType) {
        data.put(FROM_ENUM_TYPE, new MetaStr(enumType));
    }

    public boolean isFromEnumType() {
        return data.containsKey(FROM_ENUM_TYPE);
    }

    public String getFromEnumType() {
        MetaValue v = data.get(FROM_ENUM_TYPE);
        return v instanceof MetaStr(String value) ? value : null;
    }

    public void putFromCfgFilepath(String filepath) {
        data.put(FROM_CFG_FILEPATH, new MetaStr(filepath));
    }

    public String getFromCfgFilepath() {
        MetaValue v = data.get(FROM_CFG_FILEPATH);
        return v instanceof MetaStr(String value) ? value : null;
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
        if (enumRef instanceof MetaStr(String value)) {
            return value;
        }
        return "";
    }

    public void putDefaultImpl(String defaultImpl) {
        data.putFirst(DEFAULT_IMPL, new MetaStr(defaultImpl));
    }

    public String removeDefaultImpl() {
        MetaValue defaultImpl = data.remove(DEFAULT_IMPL);
        if (defaultImpl instanceof MetaStr(String value)) {
            return value;
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
        if (entry instanceof MetaStr(String value)) {
            return new EntryType.EEntry(value);
        }

        MetaValue anEnum = data.remove(ENUM);
        if (anEnum instanceof MetaStr(String value)) {
            return new EntryType.EEnum(value);
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
        if (sep instanceof MetaStr(String value)) {
            return new FieldFormat.Sep(value.charAt(0));
        }

        MetaValue fix = data.remove(FIX);
        if (fix instanceof MetaInt(int value)) {
            return new FieldFormat.Fix(value);
        }

        MetaValue block = data.remove(BLOCK);
        if (block instanceof MetaInt(int value)) {
            return new FieldFormat.Block(value);
        }

        return FieldFormat.AutoOrPack.AUTO;
    }


}

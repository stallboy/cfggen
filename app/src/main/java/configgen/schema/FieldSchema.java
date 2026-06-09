package configgen.schema;

import java.util.Objects;

public record FieldSchema(String name,
                          FieldType type,
                          FieldFormat fmt,
                          Metadata meta) {
    public FieldSchema {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        Objects.requireNonNull(fmt);
        Objects.requireNonNull(meta);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("field name empty");
        }
    }

    /**
     * @return empty when no comment
     */
    public String comment() {
        CommentData cd = meta.getComment();
        return cd != null ? cd.encode() : "";
    }

    public FieldSchema copy() {
        return new FieldSchema(name, type.copy(), fmt, meta.copy());
    }

    public boolean isLowercase() {
        return meta.isLowercase();
    }

    public boolean isMustFill() {
        return meta.isMustFill();
    }

    public boolean isSeq() {
        return meta.isSeq();
    }
}

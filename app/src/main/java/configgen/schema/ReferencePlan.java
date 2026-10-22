package configgen.schema;

import java.util.List;

/** Validated navigation shared by value validation and code generation. */
public record ReferencePlan(TableSchema root, List<Source> sources, Start target,
                            KeySchema recordKey, List<Step> steps, Nameable resultType) {
    public record Start(ReferencePath.Start kind, Structural type) {}
    public record Source(Start start, List<FieldSchema> fields, List<Integer> indices) {}
    public record Step(FieldSchema field, int fieldIndex, KeySchema listKey,
                       List<FieldType> keyTypes) {}
}

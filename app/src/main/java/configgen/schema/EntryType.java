package configgen.schema;

/**
 * 设计目标：
 * 程序入口，整个策划配表应该由这个做为初始入口，
 * 通过它的遍历。理论上能遍历到所有的配表行 但也不用执着，总有一些约定，比如初始level是1，下一level是+1，这些约定提供了额外的入口
 */
public sealed interface EntryType {

    /**
     * 大多table在内部，不用开放程序入口
     */
    enum ENo implements EntryType {
        NO
    }

    sealed abstract class EntryBase implements EntryType {
        private final String field;
        private FieldSchema fieldSchema;

        EntryBase(String field) {
            this.field = field;
        }

        public String field() {
            return field;
        }

        public FieldSchema fieldSchema() {
            return fieldSchema;
        }

        void setFieldSchema(FieldSchema fieldSchema) {
            this.fieldSchema = fieldSchema;
        }
    }

    /**
     * 代码中避免魔术，使用Entry，Enum来访问特定行
     */
    final class EEntry extends EntryBase {
        public EEntry(String field) {
            super(field);
        }
    }

    /**
     * 生成枚举，代码可以switch
     */
    final class EEnum extends EntryBase {
        public EEnum(String field) {
            super(field);
        }
    }

}

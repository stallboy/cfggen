package configgen.schema;

import java.util.OptionalInt;

import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldType.*;

public class Spans {

    public static int span(Nameable nameable) {
        if (nameable.fmt() == PACK || nameable.fmt() instanceof FieldFormat.Sep) {
            return 1;
        }

        switch (nameable) {
            case InterfaceSchema interfaceSchema -> {
                OptionalInt max = interfaceSchema.impls().stream().mapToInt(Spans::span).max();
                if (max.isPresent()) {
                    return max.getAsInt() + 1;
                } else {
                    return 1;
                }
            }
            case Structural structural -> {
                return structural.fields().stream().mapToInt(Spans::span).sum();
            }
        }
    }

    public static int span(FieldSchema field) {
        return span(field.type(), field.fmt());
    }

    public static int span(FieldType type, FieldFormat fmt) {
        switch (fmt) {
            case PACK:
            case FieldFormat.Sep _:
                return 1;
            default:
                break;
        }

        switch (type) {
            case Primitive _ -> {
                return 1;
            }

            case StructRef structRef -> {
                return span(structRef.obj());
            }

            case FList flist -> {
                switch (fmt) {
                    case FieldFormat.Block block -> {
                        return span(flist.item()) * block.fix();

                    }
                    case FieldFormat.Fix fix -> {
                        return span(flist.item()) * fix.count();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + fmt);
                }

            }
            case FMap fmap -> {
                switch (fmt) {
                    case FieldFormat.Block block -> {
                        return (span(fmap.key()) + span(fmap.value())) *
                                block.fix();

                    }
                    case FieldFormat.Fix fix -> {
                        return (span(fmap.key()) + span(fmap.value())) *
                                fix.count();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + fmt);
                }
            }
        }
    }

    public static int span(SimpleType type) {
        switch (type) {
            case Primitive _ -> {
                return 1;
            }
            case StructRef structRef -> {
                return span(structRef.obj());
            }
        }
    }

}

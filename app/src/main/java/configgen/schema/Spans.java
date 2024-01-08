package configgen.schema;

import java.util.*;

import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldType.*;
import static configgen.schema.Metadata.*;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class Spans {

    public static int span(Nameable nameable) {
        synchronized (nameable) {
            Metadata meta = nameable.meta();
            if (meta.getSpan() instanceof MetaInt vi) {
                return vi.value();
            }

            int s = calcSpan(nameable);
            meta.putSpan(s);
            return s;
        }
    }

    public static int calcSpan(Nameable nameable) {
        return calcSpanCheckLoop(nameable, new LinkedHashSet<>());
    }

    private static int calcSpanCheckLoop(Nameable nameable, SequencedSet<String> stack) {
        synchronized (nameable) {
            Metadata meta = nameable.meta();
            MetaValue value = meta.getSpan();
            if (value instanceof MetaInt vi) {
                return vi.value();
            }
        }

        if (nameable.fmt() == PACK || nameable.fmt() instanceof FieldFormat.Sep) {
            return 1;
        }

        if (!stack.add(nameable.fullName())) {
            throw new IllegalStateException("Calculate span loop: " + String.join(",", stack));
        }

        int resultSpan;
        switch (nameable) {
            case InterfaceSchema interfaceSchema -> {
                OptionalInt max = interfaceSchema.impls().stream()
                        .mapToInt(impl -> calcSpanCheckLoop(impl, stack))
                        .max();
                if (max.isPresent()) {
                    resultSpan = max.getAsInt() + 1;
                } else {
                    resultSpan = 1;
                }
            }
            case Structural structural -> {
                resultSpan = structural.fields().stream()
                        .mapToInt(field -> calcFieldSpanCheckLoop(field, stack))
                        .sum();
            }
        }
        stack.remove(nameable.fullName());
        return resultSpan;

    }

    public static int fieldSpan(FieldSchema field) {
        synchronized (field) {
            Metadata meta = field.meta();
            if (meta.getSpan() instanceof MetaInt vi) {
                return vi.value();
            }

            int s = calcFieldSpan(field);
            meta.putSpan(s);
            return s;
        }
    }

    public static int calcFieldSpan(FieldSchema field) {
        return calcFieldSpanCheckLoop(field, new LinkedHashSet<>());
    }

    public static int calcFieldSpanCheckLoop(FieldSchema field, SequencedSet<String> stack) {
        FieldFormat fmt = field.fmt();

        switch (fmt) {
            case PACK:
                return 1;
            case FieldFormat.Sep ignored:
                return 1;
            default:
                break;
        }

        switch (field.type()) {
            case Primitive ignored -> {
                return 1;
            }

            case StructRef structRef -> {
                return calcSpanCheckLoop(structRef.obj(), stack);
            }

            case FList flist -> {
                switch (fmt) {
                    case FieldFormat.Block block -> {
                        return calcSimpleTypeSpanCheckLoop(flist.item(), stack) * block.fix();

                    }
                    case FieldFormat.Fix fix -> {
                        return calcSimpleTypeSpanCheckLoop(flist.item(), stack) * fix.count();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + fmt);
                }

            }
            case FMap fmap -> {
                switch (fmt) {
                    case FieldFormat.Block block -> {
                        return (calcSimpleTypeSpanCheckLoop(fmap.key(), stack)
                                + calcSimpleTypeSpanCheckLoop(fmap.value(), stack))
                                * block.fix();

                    }
                    case FieldFormat.Fix fix -> {
                        return (calcSimpleTypeSpanCheckLoop(fmap.key(), stack)
                                + calcSimpleTypeSpanCheckLoop(fmap.value(), stack))
                                * fix.count();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + fmt);
                }
            }
        }
    }

    public static int simpleTypeSpan(SimpleType type) {
        switch (type) {
            case Primitive ignored -> {
                return 1;
            }
            case StructRef structRef -> {
                return span(structRef.obj());
            }
        }
    }

    public static int calcSimpleTypeSpan(SimpleType type) {
        return calcSimpleTypeSpanCheckLoop(type, new LinkedHashSet<>());
    }

    public static int calcSimpleTypeSpanCheckLoop(SimpleType type, SequencedSet<String> stack) {
        switch (type) {
            case Primitive ignored -> {
                return 1;
            }
            case StructRef structRef -> {
                return calcSpanCheckLoop(structRef.obj(), stack);
            }
        }
    }

}

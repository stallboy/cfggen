package configgen.schema;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldType.*;

public class Spans {
    public static int span(FieldSchema field) {
        switch (field.fmt()) {
            case PACK:
            case FieldFormat.Sep _:
                return 1;
            default:
                break;
        }

        switch (field.type()) {
            case Primitive _ -> {
                return 1;
            }

            case StructRef structRef -> {
                return span(structRef.obj());
            }

            case FList flist -> {
                switch (field.fmt()) {
                    case FieldFormat.Block block -> {
                        return span(flist.item()) * block.fix();

                    }
                    case FieldFormat.Fix fix -> {
                        return span(flist.item()) * fix.count();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + field.fmt());
                }

            }
            case FMap fmap -> {
                switch (field.fmt()) {
                    case FieldFormat.Block block -> {
                        return (span(fmap.key()) + span(fmap.value())) *
                                block.fix();

                    }
                    case FieldFormat.Fix fix -> {
                        return (span(fmap.key()) + span(fmap.value())) *
                                fix.count();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + field.fmt());
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

    public static int span(Fieldable fieldable) {
        switch (fieldable.fmt()) {
            case AUTO -> {
                switch (fieldable) {
                    case InterfaceSchema interfaceSchema -> {
                        int max = 0;
                        for (StructSchema impl : interfaceSchema.impls()) {
                            int s = span(impl);
                            if (s > max) {
                                max = s;
                            }
                        }
                        return max + 1;
                    }
                    case StructSchema structSchema -> {
                        int sum = 0;
                        for (FieldSchema field : structSchema.fields()) {
                            sum += span(field);
                        }
                        return sum;
                    }
                }
            }

            case PACK -> {
                return 1;
            }
            case FieldFormat.Sep _ -> {
                return 1;
            }
            default -> throw new IllegalStateException("Unexpected value: " + fieldable.fmt());
        }
    }
}

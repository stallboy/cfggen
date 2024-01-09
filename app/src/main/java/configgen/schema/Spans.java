package configgen.schema;

import java.util.*;

import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldType.*;
import static configgen.schema.Metadata.MetaInt;

public class Spans {

    public static void preCalculateAllNeededSpans(CfgSchema cfgSchema) {
        SequencedMap<String, Nameable> needSpans = collectNeededCalculateSpans(cfgSchema);
        for (Nameable nameable : needSpans.reversed().values()) {
            calcSpanCheckLoop(nameable, new LinkedHashSet<>());
            if (nameable instanceof Structural structural && nameable.fmt() instanceof FieldFormat.Sep){
                // 内部field的span会被略去，这里计算
                for (FieldSchema field : structural.fields()) {
                    calcFieldSpanCheckLoop(field, new LinkedHashSet<>());
                }
            }
        }
    }

    private static SequencedMap<String, Nameable> collectNeededCalculateSpans(CfgSchema cfgSchema) {
        SequencedMap<String, Nameable> collectedNeedSpans = new LinkedHashMap<>();

        List<FieldSchema> fieldFrontiers = new ArrayList<>();
        for (TableSchema table : cfgSchema.tableMap().values()) {
            if (!table.meta().isJson()) {
                collectedNeedSpans.put(table.name(), table);
                fieldFrontiers.addAll(table.fields());
            }
        }

        while (!fieldFrontiers.isEmpty()) {
            Map<String, Fieldable> needChecks = new HashMap<>();
            for (FieldSchema field : fieldFrontiers) {
                if (field.fmt() != PACK) {
                    switch (field.type()) {
                        case StructRef structRef -> {
                            // 这里只去除pack，不去除sep，因为sep是不能允许递归，所以需要检测
                            addIfNotPack(needChecks, structRef.obj());
                        }
                        case FList fList -> {
                            if (fList.item() instanceof StructRef structRef) {
                                addIfNotPack(needChecks, structRef.obj());
                            }
                        }
                        case FMap fMap -> {
                            if (fMap.key() instanceof StructRef structRef) {
                                addIfNotPack(needChecks, structRef.obj());
                            }
                            if (fMap.value() instanceof StructRef structRef) {
                                addIfNotPack(needChecks, structRef.obj());
                            }
                        }
                        default -> {
                        }
                    }
                }
            }

            fieldFrontiers.clear();
            for (Nameable nameable : needChecks.values()) {
                Nameable old = collectedNeedSpans.put(nameable.fullName(), nameable);
                boolean notCheckedBefore = (old == null);
                if (notCheckedBefore) {
                    switch (nameable) {
                        case InterfaceSchema interfaceSchema -> {
                            for (StructSchema impl : interfaceSchema.impls()) {
                                fieldFrontiers.addAll(impl.fields());
                            }
                        }
                        case StructSchema structSchema -> {
                            fieldFrontiers.addAll(structSchema.fields());
                        }
                        case TableSchema ignored -> {
                        }
                    }
                }
            }
        }

        return collectedNeedSpans;
    }

    private static void addIfNotPack(Map<String, Fieldable> needChecks, Fieldable fieldable) {
        if (fieldable != null && fieldable.fmt() != PACK) {
            needChecks.put(fieldable.name(), fieldable);
        }
    }


    private static int calcSpanCheckLoop(Nameable nameable, SequencedSet<String> stack) {
        Metadata meta = nameable.meta();
        if (meta.getSpan() instanceof MetaInt vi) {
            return vi.value();
        }

        FieldFormat fmt = nameable.fmt();
        if (fmt == PACK || fmt instanceof FieldFormat.Sep) {
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

        meta.putSpan(resultSpan);
        return resultSpan;

    }


    private static int calcFieldSpanCheckLoop(FieldSchema field, SequencedSet<String> stack) {
        FieldFormat fmt = field.fmt();
        if (fmt == PACK || fmt instanceof FieldFormat.Sep || field.type() instanceof Primitive) {
            return 1;
        }

        Metadata meta = field.meta();
        if (meta.getSpan() instanceof MetaInt vi) {
            return vi.value();
        }

        int resultSpan;
        switch (field.type()) {
            case StructRef structRef -> {
                resultSpan = calcSpanCheckLoop(structRef.obj(), stack);
            }

            case FList flist -> {
                switch (fmt) {
                    case FieldFormat.Block block -> {
                        resultSpan = calcSimpleTypeSpanCheckLoop(flist.item(), stack) * block.fix();

                    }
                    case FieldFormat.Fix fix -> {
                        resultSpan = calcSimpleTypeSpanCheckLoop(flist.item(), stack) * fix.count();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + fmt);
                }

            }
            case FMap fmap -> {
                switch (fmt) {
                    case FieldFormat.Block block -> {
                        resultSpan = (calcSimpleTypeSpanCheckLoop(fmap.key(), stack)
                                + calcSimpleTypeSpanCheckLoop(fmap.value(), stack))
                                * block.fix();

                    }
                    case FieldFormat.Fix fix -> {
                        resultSpan = (calcSimpleTypeSpanCheckLoop(fmap.key(), stack)
                                + calcSimpleTypeSpanCheckLoop(fmap.value(), stack))
                                * fix.count();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + fmt);
                }
            }
            case Primitive ignore -> {
                throw new IllegalStateException();
            }
        }

        meta.putSpan(resultSpan);
        return resultSpan;
    }

    private static int calcSimpleTypeSpanCheckLoop(SimpleType type, SequencedSet<String> stack) {
        switch (type) {
            case Primitive ignored -> {
                return 1;
            }
            case StructRef structRef -> {
                return calcSpanCheckLoop(structRef.obj(), stack);
            }
        }
    }


    public static int span(Nameable nameable) {
        FieldFormat fmt = nameable.fmt();
        if (fmt == PACK || fmt instanceof FieldFormat.Sep) {
            return 1;
        }

        if (nameable.meta().getSpan() instanceof MetaInt vi) {
            return vi.value();
        }

        throw new IllegalStateException(nameable.fullName() + "'s span not pre calculated");
    }


    public static int fieldSpan(FieldSchema field) {
        FieldFormat fmt = field.fmt();
        if (fmt == PACK || fmt instanceof FieldFormat.Sep || field.type() instanceof Primitive) {
            return 1;
        }

        if (field.meta().getSpan() instanceof MetaInt vi) {
            return vi.value();
        }

        throw new IllegalStateException(field.name() + "'s span not pre calculated");
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


}

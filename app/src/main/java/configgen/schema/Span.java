package configgen.schema;

import configgen.schema.cfg.CfgWriter;

import java.util.*;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldType.*;
import static configgen.schema.Metadata.MetaInt;
import static configgen.schema.CfgSchemaErrs.*;

/**
 * 在resolved前 预先计算好每个结构占用的excel的列数
 * 查询
 */
public class Span {

    static void preCalculateAllNeededSpans(CfgSchema cfgSchema, CfgSchemaErrs errs) {
        SequencedMap<String, Nameable> needSpans = collectNeededCalculateSpans(cfgSchema);
        // 因为needSpans是广度优先拓展得到的，先从需要映射excel的table开始。
        // reverse下，先计算table依赖的struct的span，感觉更好点
        Collection<Nameable> reversedNeedSpans = needSpans.reversed().values();
        for (Nameable nameable : reversedNeedSpans) {
            checkNameableFmt(nameable, errs);
        }

        if (errs.errs().isEmpty()) { // 如果fmt有问题就直接返回，要不然可能会抛出异常
            try {
                for (Nameable nameable : reversedNeedSpans) {
                    calcSpanCheckLoop(nameable, new LinkedHashSet<>());
                }
            } catch (StructNestLoop loop) {
                errs.addErr(new MappingToExcelLoop(loop.stack));
            }
        }
    }


    private static class StructNestLoop extends RuntimeException {
        final SequencedCollection<String> stack;

        StructNestLoop(SequencedCollection<String> stack) {
            this.stack = stack;
        }
    }

    private static void checkNameableFmt(Nameable nameable, CfgSchemaErrs errs) {
        switch (nameable) {
            case InterfaceSchema interfaceSchema -> {
                for (StructSchema impl : interfaceSchema.impls()) {
                    for (FieldSchema field : impl.fields()) {
                        checkFieldFmt(field, errs, impl.fullName());
                    }
                }
            }

            case Structural structural -> {
                for (FieldSchema field : structural.fields()) {
                    checkFieldFmt(field, errs, structural.fullName());
                }
            }
        }
    }

    private static void checkFieldFmt(FieldSchema field, CfgSchemaErrs errs, String ctx) {
        FieldType type = field.type();
        FieldFormat fmt = field.fmt();
        switch (type) {

            case Primitive ignored -> {
                if (fmt != AUTO) {
                    errs.addErr(new PrimitiveFieldFmtMustBeAuto(ctx,
                            field.name(),
                            CfgWriter.typeStr(field.type()),
                            CfgWriter.fmtStr(field.fmt())));
                }
            }
            case StructRef ignored -> {
                if (!(fmt instanceof FieldFormat.AutoOrPack)) {
                    errs.addErr(new StructFieldFmtMustBeAutoOrPack(ctx,
                            field.name(),
                            CfgWriter.typeStr(field.type()),
                            CfgWriter.fmtStr(field.fmt())));
                }
            }
            case FList flist -> {
                if (fmt == AUTO) {
                    errs.addErr(new ListFieldFmtMustBePackOrSepOrFixOrBlock(ctx,
                            field.name(),
                            CfgWriter.typeStr(field.type()),
                            CfgWriter.fmtStr(field.fmt())));
                }
                if (fmt instanceof FieldFormat.Sep(char sep) && flist.item() instanceof StructRef structRef) {
                    if (structRef.obj().fmt() instanceof FieldFormat.Sep(char sep1) && sep == sep1 ||
                        structRef.obj().fmt() == PACK && sep == ',') {
                        errs.addErr(new ListStructSepEqual(ctx, field.name()));
                    }
                }
            }
            case FMap ignored -> {
                if (fmt == AUTO || fmt instanceof FieldFormat.Sep) {
                    errs.addErr(new MapFieldFmtMustBePackOrFixOrBlock(ctx,
                            field.name(),
                            CfgWriter.typeStr(field.type()),
                            CfgWriter.fmtStr(field.fmt())));
                }
            }
        }
    }

    /**
     * @return 广度优先搜索得到所有需要计算span的结构
     */
    private static SequencedMap<String, Nameable> collectNeededCalculateSpans(CfgSchema cfgSchema) {
        SequencedMap<String, Nameable> collectedNeedSpans = new LinkedHashMap<>();

        List<FieldSchema> fieldFrontiers = new ArrayList<>();
        for (TableSchema table : cfgSchema.tableMap().values()) {
            // json存储的不需要计算span
            if (!table.isJson()) {
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


    /**
     * @param stack 用它来检验是否循环嵌套，导致无法计算span
     * @return 返回nameable所占列数
     */
    private static int calcSpanCheckLoop(Nameable nameable, SequencedSet<String> stack) {
        Metadata meta = nameable.meta();
        // 如果已经计算过了，直接返回，用来避免重复计算。
        if (meta.getSpan() instanceof MetaInt(int value)) {
            return value;
        }

        FieldFormat fmt = nameable.fmt();
        if (fmt == PACK || fmt instanceof FieldFormat.Sep) {
            return 1;
        }

        if (!stack.add(nameable.fullName())) {
            throw new StructNestLoop(stack);
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
        if (meta.getSpan() instanceof MetaInt(int value)) {
            return value;
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

        if (nameable.meta().getSpan() instanceof MetaInt(int value)) {
            return value;
        }

        throw new IllegalStateException(nameable.fullName() + " has no _span meta value, schema may not resolved");
    }


    public static int fieldSpan(FieldSchema field) {
        FieldFormat fmt = field.fmt();
        if (fmt == PACK || fmt instanceof FieldFormat.Sep || field.type() instanceof Primitive) {
            return 1;
        }

        if (field.meta().getSpan() instanceof MetaInt(int value)) {
            return value;
        }

        throw new IllegalStateException(field.name() + " has no _span meta value, schema may not resolved");
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

package configgen.schema.cfg;

import configgen.schema.*;
import configgen.schema.cfg.CommentUtils.CommentData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.*;
import static configgen.schema.Metadata.*;

public class CfgWriter {
    private final StringBuilder destination;
    private final boolean useLastName;
    private final boolean includeMetaStartWith_;

    public static String stringify(CfgSchema cfg) {
        return stringify(cfg, false, false);
    }

    public static String stringify(CfgSchema cfg, boolean useLastName, boolean includeMetaStartWith_) {
        StringBuilder sb = new StringBuilder(4 * 1024);
        CfgWriter cfgWriter = new CfgWriter(sb, useLastName, includeMetaStartWith_);
        cfgWriter.writeCfg(cfg, "");
        return sb.toString();
    }

    public CfgWriter(StringBuilder destination, boolean useLastName, boolean includeMetaStartWith_) {
        this.destination = destination;
        this.useLastName = useLastName;
        this.includeMetaStartWith_ = includeMetaStartWith_;
    }

    public void writeCfg(CfgSchema cfg, String prefix) {
        for (Nameable item : cfg.items()) {
            writeNamable(item, prefix);
        }

        // 写回文件末尾注释（默认包名 ""）
        String endComment = cfg.getFileEndComment("");
        if (!endComment.isEmpty()) {
            String[] lines = endComment.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    println("%s// %s", prefix, line);
                }
            }
        }
    }

    public void writeNamable(Nameable item, String prefix) {
        switch (item) {
            case StructSchema structSchema -> writeStruct(structSchema, prefix, false);
            case InterfaceSchema interfaceSchema -> writeInterface(interfaceSchema, prefix);
            case TableSchema tableSchema -> {
                MetaEnumValues enumValues = tableSchema.meta().getEnumValues();
                if (enumValues != null) {
                    writeEnum(tableSchema, enumValues, prefix);
                } else {
                    writeTable(tableSchema, prefix);
                }
            }
        }

    }

    public void writeTable(TableSchema table, String prefix) {
        Metadata meta = table.meta().copy();
        if (table.isColumnMode()) {
            meta.putColumnMode();
        }
        meta.putEntry(table.entry());

        CommentData comment = CommentUtils.decode(meta.removeComment());
        writeLeadingComment(comment, prefix);

        String name = useLastName ? table.lastName() : table.name();
        println("%stable %s%s%s {%s", prefix, name,
                keyStr(table.primaryKey()), metadataStr(meta), comment.formatTrailing());
        for (KeySchema keySchema : table.uniqueKeys()) {
            println("%s\t%s;", prefix, keyStr(keySchema));
        }
        writeStructural(table, prefix);

        writeSuffixComment(comment, prefix);

        println("%s}", prefix);
        println();
    }

    public void writeEnum(TableSchema table, MetaEnumValues enumValues, String prefix) {
        Metadata meta = table.meta().copy();
        meta.removeEnumValues();  // 不写出 enumValues，因为会还原为 enum 格式

        CommentData comment = CommentUtils.decode(meta.removeComment());
        writeLeadingComment(comment, prefix);

        String name = useLastName ? table.lastName() : table.name();
        println("%senum %s%s {%s", prefix, name, metadataStr(meta), comment.formatTrailing());
        for (MetaEnumValues.EnumValue ev : enumValues.values()) {
            String valueComment = ev.comment().isEmpty() ? "" : " // " + ev.comment();
            println("%s\t%s;%s", prefix, ev.name(), valueComment);
        }

        writeSuffixComment(comment, prefix);

        println("%s}", prefix);
        println();
    }

    public void writeInterface(InterfaceSchema sInterface, String prefix) {
        Metadata meta = sInterface.meta().copy();
        meta.putFmt(sInterface.fmt());
        if (!sInterface.defaultImpl().isEmpty()) {
            meta.putDefaultImpl(sInterface.defaultImpl());
        }
        if (!sInterface.enumRef().isEmpty()) {
            meta.putEnumRef(sInterface.enumRef());
        }

        CommentData comment = CommentUtils.decode(meta.removeComment());
        writeLeadingComment(comment, prefix);

        String name = useLastName ? sInterface.lastName() : sInterface.name();
        println("%sinterface %s%s {%s", prefix, name, metadataStr(meta), comment.formatTrailing());
        int i = 0;
        for (StructSchema value : sInterface.impls()) {
            i++;
            boolean noLineSeparator = sInterface.impls().size() == i;
            writeStruct(value, prefix + "\t", noLineSeparator);
        }

        writeSuffixComment(comment, prefix);

        println("%s}", prefix);
        println();
    }

    public void writeStruct(StructSchema struct, String prefix, boolean noLineSeparator) {
        Metadata meta = struct.meta().copy();
        meta.putFmt(struct.fmt());

        CommentData comment = CommentUtils.decode(meta.removeComment());
        writeLeadingComment(comment, prefix);

        String name = useLastName ? struct.lastName() : struct.name();
        println("%sstruct %s%s {%s", prefix, name, metadataStr(meta), comment.formatTrailing());
        writeStructural(struct, prefix);

        writeSuffixComment(comment, prefix);

        println("%s}", prefix);
        if (!noLineSeparator) {
            println();
        }
    }

    private void writeStructural(Structural structural, String prefix) {
        for (FieldSchema f : structural.fields()) {
            Metadata meta = f.meta().copy();
            meta.putFmt(f.fmt());

            ForeignKeySchema fk = structural.findForeignKey(f.name());

            // 检查是否是 enum 类型的字段
            String typeStr;
            String fkStr;
            if (fk != null && fk.meta().isFromEnumType()) {
                // enum 字段：还原为 enum 类型名，不写外键符号
                typeStr = fk.refTable();  // 如 "ArgCaptureMode"
                fkStr = "";
            } else {
                typeStr = typeStr(f.type());
                fkStr = fk == null ? "" : foreignStr(fk);
            }

            if (fk != null) {
                foreignToMeta(fk, meta);
            }

            CommentData comment = CommentUtils.decode(meta.removeComment());
            writeLeadingComment(comment, prefix + "\t");
            println("%s\t%s:%s%s%s;%s",
                    prefix, f.name(), typeStr, fkStr,
                    metadataStr(meta), comment.formatTrailing());
        }

        for (ForeignKeySchema fk : structural.foreignKeys()) {
            // 跳过 fromEnumType 的外键（已在字段中处理）和字段同名的外键
            if (fk.meta().isFromEnumType()) {
                continue;
            }
            if (structural.findField(fk.name()) == null) {
                Metadata meta = fk.meta().copy();
                foreignToMeta(fk, meta);

                CommentData comment = CommentUtils.decode(meta.removeComment());
                writeLeadingComment(comment, prefix + "\t");
                println("%s\t->%s:%s%s%s;%s",
                        prefix, fk.name(), keyStr(fk.key()), foreignStr(fk),
                        metadataStr(meta), comment.formatTrailing());
            }
        }
    }

    private void println(String fmt, Object... args) {
        if (args.length == 0) {
            destination.append(fmt);
        } else {
            destination.append(String.format(fmt, args));
        }

        destination.append("\r\n");
    }

    private void println() {
        destination.append("\r\n");
    }

    private void writeLeadingComment(CommentData cd, String prefix) {
        String leadingComment = cd.formatLeading(prefix);
        if (!leadingComment.isEmpty()) {
            destination.append(leadingComment);
        }
    }

    private void writeSuffixComment(CommentData cd, String prefix) {
        String suffixComment = cd.formatSuffix(prefix);
        if (!suffixComment.isEmpty()) {
            destination.append(suffixComment);
        }
    }

    public static String typeStr(FieldType t) {
        return switch (t) {
            case Primitive.STRING -> "str";
            case Primitive primitive -> primitive.name().toLowerCase();
            case StructRef structRef -> structRef.name();
            case FList fList -> String.format("list<%s>", typeStr(fList.item()));
            case FMap fMap -> String.format("map<%s,%s>", typeStr(fMap.key()), typeStr(fMap.value()));
        };
    }

    public static String typeStrWithFullName(FieldType t) {
        return switch (t) {
            case Primitive.STRING -> "str";
            case Primitive primitive -> primitive.name().toLowerCase();
            case StructRef structRef -> structRef.obj().fullName();
            case FList fList -> String.format("list<%s>", typeStrWithFullName(fList.item()));
            case FMap fMap ->
                    String.format("map<%s,%s>", typeStrWithFullName(fMap.key()), typeStrWithFullName(fMap.value()));
        };
    }

    public static String fmtStr(FieldFormat fmt) {
        Metadata meta = Metadata.of();
        meta.putFmt(fmt);
        Map.Entry<String, MetaValue> entry = meta.data().firstEntry();
        if (entry != null) {
            return metaEntryStr(entry);
        }
        return "";
    }


    static String keyStr(KeySchema key) {
        return String.format("[%s]", String.join(",", key.fields()));
    }

    static void foreignToMeta(ForeignKeySchema fk, Metadata meta) {
        switch (fk.refKey()) {
            case RefKey.RefSimple refSimple -> {
                if (refSimple.nullable()) {
                    meta.putNullable();
                }
            }
            case RefKey.RefList ignored -> {
            }
        }
    }

    static String foreignStr(ForeignKeySchema fk) {
        return switch (fk.refKey()) {
            case RefKey.RefPrimary ignored -> String.format(" ->%s", fk.refTable());
            case RefKey.RefUniq refUniq -> String.format(" ->%s%s", fk.refTable(), keyStr(refUniq.key()));
            case RefKey.RefList refList -> String.format(" =>%s%s", fk.refTable(), keyStr(refList.key()));
        };
    }

    String metadataStr(Metadata meta) {
        if (meta.data().isEmpty()) {
            return "";
        }

        Metadata m;
        if (includeMetaStartWith_) {
            m = meta;
        } else {
            m = of();
            for (Map.Entry<String, MetaValue> e : meta.data().entrySet()) {
                if (!e.getKey().startsWith("_")) {
                    m.data().put(e.getKey(), e.getValue());
                }
            }

            if (m.data().isEmpty()) {
                return "";
            }
        }
        List<String> list = m.data().entrySet().stream().map(CfgWriter::metaEntryStr).collect(Collectors.toList());
        return String.format(" (%s)", String.join(", ", list));
    }

    private static String metaEntryStr(Map.Entry<String, MetaValue> entry) {
        String k = entry.getKey();
        return switch (entry.getValue()) {
            case MetaTag.TAG -> k;
            case MetaFloat metaFloat -> String.format("%s=%f", k, metaFloat.value());
            case MetaInt metaInt -> String.format("%s=%d", k, metaInt.value());
            case MetaStr metaStr -> String.format("%s='%s'", k, metaStr.value());
            case MetaEnumValues ignored -> "";  // enumValues 不会写出
        };
    }

}

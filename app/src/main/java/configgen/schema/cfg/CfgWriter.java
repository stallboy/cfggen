package configgen.schema.cfg;

import configgen.schema.*;

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
        cfgWriter.writeCfg(cfg);
        return sb.toString();
    }

    public CfgWriter(StringBuilder destination, boolean useLastName, boolean includeMetaStartWith_) {
        this.destination = destination;
        this.useLastName = useLastName;
        this.includeMetaStartWith_ = includeMetaStartWith_;
    }

    public void writeCfg(CfgSchema cfg) {
        for (Nameable item : cfg.items()) {
            switch (item) {
                case StructSchema structSchema -> writeStruct(structSchema, "");
                case InterfaceSchema interfaceSchema -> writeInterface(interfaceSchema);
                case TableSchema tableSchema -> writeTable(tableSchema);
            }
        }
    }

    public void writeTable(TableSchema table) {
        Metadata meta = table.meta().copy();
        if (table.isColumnMode()) {
            meta.putColumnMode();
        }
        meta.putEntry(table.entry());
        String comment = meta.removeComment();

        String name = useLastName ? table.lastName() : table.name();
        println("table %s%s%s {%s", name, keyStr(table.primaryKey()), metadataStr(meta), commentStr(comment));
        for (KeySchema keySchema : table.uniqueKeys()) {
            println("\t%s;", keyStr(keySchema));
        }
        writeStructural(table, "");
        println("}");
        println();
    }

    public void writeInterface(InterfaceSchema sInterface) {
        Metadata meta = sInterface.meta().copy();
        meta.putFmt(sInterface.fmt());
        if (!sInterface.defaultImpl().isEmpty()) {
            meta.putDefaultImpl(sInterface.defaultImpl());
        }
        if (!sInterface.enumRef().isEmpty()) {
            meta.putEnumRef(sInterface.enumRef());
        }
        String comment = meta.removeComment();

        String name = useLastName ? sInterface.lastName() : sInterface.name();
        println("interface %s%s {%s", name, metadataStr(meta), commentStr(comment));
        for (StructSchema value : sInterface.impls()) {
            writeStruct(value, "\t");
        }
        println("}");
        println();
    }

    public void writeStruct(StructSchema struct, String prefix) {
        Metadata meta = struct.meta().copy();
        meta.putFmt(struct.fmt());
        String comment = meta.removeComment();
        String name = useLastName ? struct.lastName() : struct.name();
        println("%sstruct %s%s {%s", prefix, name, metadataStr(meta), commentStr(comment));
        writeStructural(struct, prefix);
        println("%s}", prefix);
        println();
    }

    private void writeStructural(Structural structural, String prefix) {
        for (FieldSchema f : structural.fields()) {
            Metadata meta = f.meta().copy();
            meta.putFmt(f.fmt());
            String comment = meta.removeComment();

            ForeignKeySchema fk = structural.findForeignKey(f.name());
            String fkStr = fk == null ? "" : foreignStr(fk);
            if (fk != null) {
                foreignToMeta(fk, meta);
            }
            println("%s\t%s:%s%s%s;%s",
                    prefix, f.name(), typeStr(f.type()), fkStr, metadataStr(meta), commentStr(comment));
        }

        for (ForeignKeySchema fk : structural.foreignKeys()) {
            if (structural.findField(fk.name()) == null) {
                Metadata meta = fk.meta().copy();
                String comment = meta.removeComment();
                foreignToMeta(fk, meta);
                println("%s\t->%s:%s%s%s;%s",
                        prefix, fk.name(), keyStr(fk.key()), foreignStr(fk), metadataStr(meta), commentStr(comment));
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
        };
    }

    static String commentStr(String comment) {
        if (comment.isEmpty()) {
            return "";
        } else {
            return String.format(" // %s", comment);
        }
    }
}

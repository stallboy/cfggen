package configgen.schema.cfg;

import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.schema.FieldType.*;
import static configgen.schema.Metadata.*;

public class CfgWriter {
    private final StringBuilder destination;
    private final boolean useLastName;

    public static String stringify(CfgSchema cfg) {
        return stringify(cfg, false);
    }

    public static String stringify(CfgSchema cfg, boolean useLastName) {
        StringBuilder sb = new StringBuilder(4 * 1024);
        CfgWriter cfgWriter = new CfgWriter(sb, useLastName);
        cfgWriter.writeCfg(cfg);
        return sb.toString();
    }

    public CfgWriter(StringBuilder destination, boolean useLastName) {
        this.destination = destination;
        this.useLastName = useLastName;
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
        println(STR. "table \{ name }\{ keyStr(table.primaryKey()) }\{ metadataStr(meta) } {\{ commentStr(comment) }" );
        for (KeySchema keySchema : table.uniqueKeys()) {
            println(STR. "\t\{ keyStr(keySchema) };" );
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
        meta.putEnumRef(sInterface.enumRef());
        String comment = meta.removeComment();

        String name = useLastName ? sInterface.lastName() : sInterface.name();
        println(STR. "interface \{ name }\{ metadataStr(meta) } {\{ commentStr(comment) }" );
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
        println(STR. "\{ prefix }struct \{ name }\{ metadataStr(meta) } {\{ commentStr(comment) }" );
        writeStructural(struct, prefix);
        println(STR. "\{ prefix }}" );
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
            println(STR. "\{ prefix }\t\{ f.name() }:\{ typeStr(f.type()) }\{ fkStr }\{ metadataStr(meta) };\{ commentStr(comment) }" );
        }

        for (ForeignKeySchema fk : structural.foreignKeys()) {
            if (structural.findField(fk.name()) == null) {
                Metadata meta = fk.meta().copy();
                String comment = meta.removeComment();
                foreignToMeta(fk, meta);
                println(STR. "\{ prefix }\t->\{ fk.name() }:\{ keyStr(fk.key()) }\{ foreignStr(fk) }\{ metadataStr(meta) };\{ commentStr(comment) }" );
            }
        }
    }

    private void println(String s) {
        destination.append(s).append("\r\n");
    }

    private void println() {
        destination.append("\r\n");
    }

    static String typeStr(FieldType t) {
        return switch (t) {
            case Primitive.STRING -> "str";
            case Primitive primitive -> primitive.name().toLowerCase();
            case StructRef structRef -> structRef.name();
            case FList fList -> STR. "list<\{ typeStr(fList.item()) }>" ;
            case FMap fMap -> STR. "map<\{ typeStr(fMap.key()) },\{ typeStr(fMap.value()) }>" ;
        };
    }

    static String keyStr(KeySchema key) {
        return STR. "[\{ String.join(",", key.name()) }]" ;
    }

    static void foreignToMeta(ForeignKeySchema fk, Metadata meta) {
        switch (fk.refKey()) {
            case RefKey.RefSimple refSimple -> {
                if (refSimple.nullable()) {
                    meta.putNullable();
                }
            }
            case RefKey.RefList _ -> {
            }
        }
    }

    static String foreignStr(ForeignKeySchema fk) {
        return switch (fk.refKey()) {
            case RefKey.RefPrimary _ -> STR. " ->\{ fk.refTable() }" ;
            case RefKey.RefUniq refUniq -> STR. " ->\{ fk.refTable() }\{ keyStr(refUniq.key()) }" ;
            case RefKey.RefList refList -> STR. " =>\{ fk.refTable() }\{ keyStr(refList.key()) }" ;
        };
    }

    static String metadataStr(Metadata meta) {
        if (meta.data().isEmpty()) {
            return "";
        }

        List<String> list = new ArrayList<>();
        for (Map.Entry<String, MetaValue> entry : meta.data().entrySet()) {
            String k = entry.getKey();
            MetaValue v = entry.getValue();
            String str = switch (v) {
                case MetaTag.TAG -> k;
                case MetaFloat metaFloat -> STR. "\{ k }=\{ metaFloat.value() }" ;
                case MetaInt metaInt -> STR. "\{ k }=\{ metaInt.value() }" ;
                case MetaStr metaStr -> STR. "\{ k }='\{ metaStr.value() }'" ;
            };
            list.add(str);
        }
        return STR. " (\{ String.join(", ", list) })" ;
    }

    static String commentStr(String comment) {
        if (comment.isEmpty()) {
            return "";
        } else {
            return STR. " // \{ comment }" ;
        }
    }
}

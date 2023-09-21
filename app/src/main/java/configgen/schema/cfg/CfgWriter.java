package configgen.schema.cfg;

import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.schema.FieldType.*;
import static configgen.schema.Metadata.*;

public class CfgWriter {
    private final StringBuilder sb;
    private final boolean useLastName;

    public CfgWriter(StringBuilder ps) {
        this(ps, false);
    }

    public CfgWriter(StringBuilder sb, boolean useLastName) {
        this.sb = sb;
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
        if (table.isColumnMode()) {
            table.meta().data().putFirst("isColumnMode", MetaTag.TAG);
        }
        Metas.putEntry(table.meta(), table.entry());
        String comment = Metas.removeComment(table.meta());

        String name = useLastName ? table.lastName() : table.name();
        println(STR. "table \{ name }\{ keyStr(table.primaryKey()) }\{ metadataStr(table.meta()) } {\{ commentStr(comment) }" );
        for (KeySchema keySchema : table.uniqueKeys()) {
            println(STR. "\t\{ keyStr(keySchema) };" );
        }
        writeStructural(table, "");
        println("}");
        println();
    }

    public void writeInterface(InterfaceSchema sInterface) {
        Metas.putFmt(sInterface.meta(), sInterface.fmt());
        if (!sInterface.defaultImpl().isEmpty()) {
            sInterface.meta().data().putFirst("defaultImpl", new MetaStr(sInterface.defaultImpl()));
        }
        sInterface.meta().data().putFirst("enumRef", new MetaStr(sInterface.enumRef()));
        String comment = Metas.removeComment(sInterface.meta());

        String name = useLastName ? sInterface.lastName() : sInterface.name();
        println(STR. "interface \{ name }\{ metadataStr(sInterface.meta()) } {\{ commentStr(comment) }" );
        for (StructSchema value : sInterface.impls()) {
            writeStruct(value, "\t");
        }
        println("}");
        println();
    }

    public void writeStruct(StructSchema struct, String prefix) {
        Metas.putFmt(struct.meta(), struct.fmt());
        String comment = Metas.removeComment(struct.meta());
        String name = useLastName ? struct.lastName() : struct.name();
        println(STR. "\{ prefix }struct \{ name }\{ metadataStr(struct.meta()) } {\{ commentStr(comment) }" );
        writeStructural(struct, prefix);
        println(STR. "\{ prefix }}" );
        println();
    }

    private void writeStructural(Structural structural, String prefix) {
        for (FieldSchema f : structural.fields()) {
            Metas.putFmt(f.meta(), f.fmt());
            String comment = Metas.removeComment(f.meta());

            ForeignKeySchema fk = structural.findForeignKey(f.name());
            String fkStr = fk == null ? "" : foreignStr(fk);
            if (fk != null) {
                foreignToMeta(fk, f.meta());
            }
            println(STR. "\{ prefix }\t\{ f.name() }:\{ typeStr(f.type()) }\{ fkStr }\{ metadataStr(f.meta()) };\{ commentStr(comment) }" );
        }

        for (ForeignKeySchema fk : structural.foreignKeys()) {
            if (structural.findField(fk.name()) == null) {
                String comment = Metas.removeComment(fk.meta());
                foreignToMeta(fk, fk.meta());
                println(STR. "\{ prefix }\t->\{ fk.name() }:\{ keyStr(fk.key()) }\{ foreignStr(fk) }\{ metadataStr(fk.meta()) };\{ commentStr(comment) }" );
            }
        }
    }

    private void println(String s) {
        sb.append(s).append("\r\n");
    }

    private void println() {
        sb.append("\r\n");
    }

    static String typeStr(FieldType t) {
        return switch (t) {
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
            case RefKey.RefPrimary refPrimary -> {
                if (refPrimary.nullable()) {
                    Metas.putNullable(meta);
                }
            }
            case RefKey.RefUniq refUniq -> {
                if (refUniq.nullable()) {
                    Metas.putNullable(meta);
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

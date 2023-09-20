package configgen.schema.cfg;

import configgen.schema.*;

import static configgen.schema.Metadata.*;

import configgen.schema.xml.XmlParser;

import java.nio.file.Path;
import java.util.*;

import static configgen.schema.FieldType.*;

public class CfgSaver {

    private final CfgSchema cfg;

    public CfgSaver(CfgSchema cfg) {
        this.cfg = cfg;
    }

    public void save() {
        for (Fieldable value : cfg.structs().values()) {
            switch (value) {
                case StructSchema structSchema -> printStruct(structSchema, "");
                case InterfaceSchema interfaceSchema -> printInterface(interfaceSchema);
            }
        }
        for (TableSchema value : cfg.tables().values()) {
            printTable(value);
        }

        TableSchema tableSchema = cfg.tables().get("item.commonitem");
        println(tableSchema.toString());
    }

    private void printTable(TableSchema table) {
        if (table.isColumnMode()) {
            table.meta().data().putFirst("isColumnMode", MetaTag.TAG);
        }
        Metas.putEntry(table.meta(), table.entry());

        println(STR. "table \{ table.name() }\{ keyStr(table.primaryKey()) }\{ metadataStr(table.meta()) } {" );
        printStructural(table, "");

        for (KeySchema keySchema : table.uniqueKeys()) {
            println(STR. "\t\{ keyStr(keySchema) }" );
        }

        println("}");
        println();
    }

    private void printInterface(InterfaceSchema sInterface) {
        Metas.putFmt(sInterface.meta(), sInterface.fmt());
        if (!sInterface.defaultImpl().isEmpty()) {
            sInterface.meta().data().putFirst("defaultImpl", new MetaStr(sInterface.defaultImpl()));
        }
        sInterface.meta().data().putFirst("enumRef", new MetaStr(sInterface.enumRef()));

        println(STR. "interface \{ sInterface.name() }\{ metadataStr(sInterface.meta()) } {" );

        for (StructSchema value : sInterface.impls().values()) {
            printStruct(value, "\t");
        }
        println("}");
        println();
    }

    private void printStruct(StructSchema struct, String prefix) {
        Metas.putFmt(struct.meta(), struct.fmt());
        println(STR. "\{ prefix }struct \{ struct.name() }\{ metadataStr(struct.meta()) } {" );
        printStructural(struct, prefix);
        println(STR. "\{ prefix }}" );
        println();
    }

    private void printStructural(Structural structural, String prefix) {
        for (FieldSchema f : structural.fields()) {
            Metas.putFmt(f.meta(), f.fmt());
            println(STR. "\{ prefix }\t\{ f.name() }:\{ typeStr(f.type()) }\{ foreignStr(f, structural) }\{ metadataStr(f.meta()) };" );
        }

        for (ForeignKeySchema fk : structural.foreignKeys()) {
            if (structural.findField(fk.name()) == null) {
                println(STR. "\{ prefix }\t->\{ fk.name() }:\{ keyStr(fk.key()) }\{ foreignStr(fk) }\{ metadataStr(fk.meta()) };" );
            }
        }
    }

    private void println(String s) {
        System.out.println(s);
    }

    private void println() {
        System.out.println();
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

    static String foreignStr(FieldSchema field, Structural struct) {
        ForeignKeySchema fk = struct.findForeignKey(field.name());
        if (fk == null) {
            return "";
        }
        return foreignStr(fk);
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
                case MetaStr metaStr -> STR. "\{ k }=\{ metaStr.value() }" ;
            };
            list.add(str);
        }
        return STR. " (\{ String.join(", ", list) })" ;
    }


    public static void main(String[] args) {
        CfgSchema cfg = new CfgSchema(new TreeMap<>(), new TreeMap<>());
        XmlParser parser = new XmlParser(cfg);
        parser.parse(Path.of("config.xml"), true);
        new CfgSaver(cfg).save();
    }


}

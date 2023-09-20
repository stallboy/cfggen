package configgen.schema.cfg;

import java.nio.file.Path;
import java.util.*;

import configgen.schema.*;
import configgen.util.CachedIndentPrinter;

import static configgen.schema.Metadata.*;
import static configgen.schema.FieldType.*;

public enum CfgSaver {
    INSTANCE;

    private CachedIndentPrinter printer;
    private boolean useLastName = false;

    public void saveInAllSubDirectory(CfgSchema root, Path dst) {
        dst = dst.toAbsolutePath().normalize();
        Map<String, CfgSchema> cfgs = Cfgs.separate(root);
        for (Map.Entry<String, CfgSchema> entry : cfgs.entrySet()) {
            String ns = entry.getKey();
            CfgSchema cfg = entry.getValue();

            if (ns.isEmpty()) {
                saveInOneFile(cfg, dst, true);
            } else {
                Path d = dst.getParent();
                String[] split = ns.split("\\.");
                String lastSub = "config";
                for (String sub : split) {
                    d = d.resolve(sub);
                    lastSub = sub;
                }
                d = d.resolve(lastSub + ".cfg");
                saveInOneFile(cfg, d, true);
            }
        }
    }

    public void saveInOneFile(CfgSchema cfg, Path dst) {
        saveInOneFile(cfg, dst, false);
    }

    private void saveInOneFile(CfgSchema cfg, Path dst, boolean useLastName) {
        try (CachedIndentPrinter print = new CachedIndentPrinter(dst)) {
            printer = print;
            this.useLastName = useLastName;

            for (Fieldable value : cfg.structs().values()) {
                switch (value) {
                    case StructSchema structSchema -> printStruct(structSchema, "");
                    case InterfaceSchema interfaceSchema -> printInterface(interfaceSchema);
                }
            }
            for (TableSchema value : cfg.tables().values()) {
                printTable(value);
            }
        }
    }

    private void printTable(TableSchema table) {
        if (table.isColumnMode()) {
            table.meta().data().putFirst("isColumnMode", MetaTag.TAG);
        }
        Metas.putEntry(table.meta(), table.entry());
        String comment = Metas.removeComment(table.meta());

        String name = useLastName ? table.lastName() : table.name();
        println(STR. "table \{ name }\{ keyStr(table.primaryKey()) }\{ metadataStr(table.meta()) } {\{ commentStr(comment) }" );
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
        String comment = Metas.removeComment(sInterface.meta());

        String name = useLastName ? sInterface.lastName() : sInterface.name();
        println(STR. "interface \{ name }\{ metadataStr(sInterface.meta()) } {\{ commentStr(comment) }" );
        for (StructSchema value : sInterface.impls().values()) {
            printStruct(value, "\t");
        }
        println("}");
        println();
    }

    private void printStruct(StructSchema struct, String prefix) {
        Metas.putFmt(struct.meta(), struct.fmt());
        String comment = Metas.removeComment(struct.meta());
        String name = useLastName ? struct.lastName() : struct.name();
        println(STR. "\{ prefix }struct \{ name }\{ metadataStr(struct.meta()) } {\{ commentStr(comment) }" );
        printStructural(struct, prefix);
        println(STR. "\{ prefix }}" );
        println();
    }

    private void printStructural(Structural structural, String prefix) {
        for (FieldSchema f : structural.fields()) {
            Metas.putFmt(f.meta(), f.fmt());
            String comment = Metas.removeComment(f.meta());
            println(STR. "\{ prefix }\t\{ f.name() }:\{ typeStr(f.type()) }\{ foreignStr(f, structural) }\{ metadataStr(f.meta()) };\{ commentStr(comment) }" );
        }

        for (ForeignKeySchema fk : structural.foreignKeys()) {
            if (structural.findField(fk.name()) == null) {
                String comment = Metas.removeComment(fk.meta());
                println(STR. "\{ prefix }\t->\{ fk.name() }:\{ keyStr(fk.key()) }\{ foreignStr(fk) }\{ metadataStr(fk.meta()) };\{ commentStr(comment) }" );
            }
        }
    }

    private void println(String s) {
        printer.println(s);
    }

    private void println() {
        printer.println();
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

    public static void main(String[] args) {
        CfgSchema cfg = CfgSchema.of();
        XmlParser parser = new XmlParser(cfg);
        parser.parse(Path.of("config.xml"), true);
        CfgSaver.INSTANCE.saveInAllSubDirectory(cfg, Path.of("config.cfg"));
    }

}

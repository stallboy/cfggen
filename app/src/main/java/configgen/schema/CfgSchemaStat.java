package configgen.schema;

import java.util.List;

import static configgen.schema.FieldFormat.*;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;

public class CfgSchemaStat implements Stat {
    private int structCount;
    private int interfaceCount;
    private int implCount;
    private int tableCount;
    private int fieldCount;

    private int tBoolCount;
    private int tIntCount;
    private int tLongCount;
    private int tFloatCount;
    private int tStrCount;
    private int tTextCount;
    private int tListCount;
    private int tMapCount;
    private int tStructRefCount;

    private int fPackCount;
    private int fSepCount;
    private int fSepListCount;
    private int fSepListStructCount;

    private int fFixCount;
    private int fBlockCount;

    private int eEntryCount;
    private int eEnumCount;

    private int refCount;
    private int refUniqKeyCount;
    private int refListCount;
    private int refByStructCount;
    private int refByListItemCount;
    private int refByMapValueCount;

    private int uniqKeyCount;
    private int multi2KeyCount;
    private int multi3KeyCount;
    private int multiGt3KeyCount;


    public CfgSchemaStat(CfgSchema cfg) {
        for (Nameable item : cfg.items()) {
            switch (item) {
                case InterfaceSchema sInterface -> {
                    interfaceCount++;
                    parseInterface(sInterface);

                }
                case StructSchema struct -> {
                    structCount++;
                    parseStruct(struct);
                }
                case TableSchema table -> {
                    tableCount++;
                    parseTable(table);
                }
            }
        }
    }

    private void parseTable(TableSchema table) {
        parseStructural(table);
        switch (table.entry()) {
            case EntryType.ENo.NO -> {
            }
            case EntryType.EEntry ignored -> eEntryCount++;
            case EntryType.EEnum ignored -> eEnumCount++;
        }

        parseKey(table.primaryKey());
        for (KeySchema uk : table.uniqueKeys()) {
            uniqKeyCount++;
            parseKey(uk);
        }
    }

    private void parseKey(KeySchema key) {
        switch (key.fields().size()) {
            case 1 -> {
            }
            case 2 -> multi2KeyCount++;
            case 3 -> multi3KeyCount++;
            default -> multiGt3KeyCount++;
        }
    }

    private void parseStruct(StructSchema struct) {
        parseStructural(struct);
        switch (struct.fmt()) {
            case AutoOrPack.PACK -> fPackCount++;
            case Sep ignored -> fSepCount++;
            default -> {
            }
        }
    }

    private void parseInterface(InterfaceSchema sInterface) {
        for (StructSchema impl : sInterface.impls()) {
            implCount++;
            parseStruct(impl);
        }
    }

    private void parseStructural(Structural s) {
        for (FieldSchema field : s.fields()) {
            fieldCount++;
            switch (field.type()) {
                case BOOL -> tBoolCount++;
                case INT -> tIntCount++;
                case LONG -> tLongCount++;
                case FLOAT -> tFloatCount++;
                case STRING -> tStrCount++;
                case TEXT -> tTextCount++;
                case StructRef ignored -> tStructRefCount++;
                case FList ignored -> tListCount++;
                case FMap ignored -> tMapCount++;
            }
            switch (field.fmt()) {
                case AutoOrPack.PACK -> fPackCount++;
                case Sep ignored -> {
                    fSepCount++;
                    if (field.type() instanceof FList flist) {
                        fSepListCount++;
                        if (flist.item() instanceof StructRef) {
//                            System.out.println(field);
                            fSepListStructCount++;
                        }
                    }
                }
                case Fix ignored -> fFixCount++;
                case Block ignored -> fBlockCount++;
                case AutoOrPack.AUTO -> {
                }
            }
        }

        for (ForeignKeySchema fk : s.foreignKeys()) {
            refCount++;
            switch (fk.refKey()) {
                case RefKey.RefPrimary ignored -> {
                }
                case RefKey.RefUniq ignored -> refUniqKeyCount++;
                case RefKey.RefList ignored -> refListCount++;
            }

            List<FieldSchema> fs = fk.key().fieldSchemas();
            if (fs.size() == 1) {
                FieldSchema f = fs.getFirst();
                switch (f.type()) {
                    case StructRef ignored -> refByStructCount++;
                    case FList ignored -> refByListItemCount++;
                    case FMap ignored -> refByMapValueCount++;
                    default -> {
                    }
                }
            }
        }
    }

}

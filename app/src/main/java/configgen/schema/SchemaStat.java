package configgen.schema;

import java.util.List;

import static configgen.schema.FieldFormat.*;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;

public class SchemaStat implements Stat {
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


    public SchemaStat(CfgSchema cfg) {
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
            case EntryType.EEntry _ -> eEntryCount++;
            case EntryType.EEnum _ -> eEnumCount++;
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
            case Sep _ -> fSepCount++;
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
                case StructRef _ -> tStructRefCount++;
                case FList _ -> tListCount++;
                case FMap _ -> tMapCount++;
            }
            switch (field.fmt()) {
                case AutoOrPack.PACK -> fPackCount++;
                case Sep _ -> fSepCount++;
                case Fix _ -> fFixCount++;
                case Block _ -> fBlockCount++;
                case AutoOrPack.AUTO -> {
                }
            }
        }

        for (ForeignKeySchema fk : s.foreignKeys()) {
            refCount++;
            switch (fk.refKey()) {
                case RefKey.RefPrimary _ -> {
                }
                case RefKey.RefUniq _ -> refUniqKeyCount++;
                case RefKey.RefList _ -> refListCount++;
            }

            List<FieldSchema> fs = fk.key().fieldSchemas();
            if (fs.size() == 1) {
                FieldSchema f = fs.get(0);
                switch (f.type()) {
                    case StructRef _ -> refByStructCount++;
                    case FList _ -> refByListItemCount++;
                    case FMap _ -> refByMapValueCount++;
                    default -> {
                    }
                }
            }
        }
    }

    public int structCount() {
        return structCount;
    }

    public int interfaceCount() {
        return interfaceCount;
    }

    public int implCount() {
        return implCount;
    }

    public int tableCount() {
        return tableCount;
    }

    public int fieldCount() {
        return fieldCount;
    }

    public int tBoolCount() {
        return tBoolCount;
    }

    public int tIntCount() {
        return tIntCount;
    }

    public int tLongCount() {
        return tLongCount;
    }

    public int tFloatCount() {
        return tFloatCount;
    }

    public int tStrCount() {
        return tStrCount;
    }

    public int tTextCount() {
        return tTextCount;
    }

    public int tListCount() {
        return tListCount;
    }

    public int tMapCount() {
        return tMapCount;
    }

    public int tStructRefCount() {
        return tStructRefCount;
    }

    public int fPackCount() {
        return fPackCount;
    }

    public int fSepCount() {
        return fSepCount;
    }

    public int fFixCount() {
        return fFixCount;
    }

    public int fBlockCount() {
        return fBlockCount;
    }

    public int eEntryCount() {
        return eEntryCount;
    }

    public int eEnumCount() {
        return eEnumCount;
    }

    public int refCount() {
        return refCount;
    }

    public int refUniqKeyCount() {
        return refUniqKeyCount;
    }

    public int refListCount() {
        return refListCount;
    }

    public int refByStructCount() {
        return refByStructCount;
    }

    public int refByListItemCount() {
        return refByListItemCount;
    }

    public int refByMapValueCount() {
        return refByMapValueCount;
    }

    public int uniqKeyCount() {
        return uniqKeyCount;
    }

    public int multi2KeyCount() {
        return multi2KeyCount;
    }

    public int multi3KeyCount() {
        return multi3KeyCount;
    }

    public int multiGt3KeyCount() {
        return multiGt3KeyCount;
    }

}

package configgen.schema.cfg;

import configgen.schema.EntryType;
import configgen.schema.EntryType.EEntry;
import configgen.schema.EntryType.EEnum;
import configgen.schema.EntryType.ENo;
import configgen.schema.FieldFormat;
import configgen.schema.Metadata;
import configgen.schema.Metadata.MetaInt;

import java.util.SequencedMap;
import java.util.Set;

import static configgen.schema.FieldFormat.*;
import static configgen.schema.Metadata.MetaStr;
import static configgen.schema.Metadata.MetaTag.TAG;
import static configgen.schema.Metadata.MetaValue;

public class Metas {

    private static final String COMMENT = "__comment";
    private static final String NULLABLE = "nullable";
    private static final String ENUM_REF = "enumRef";
    private static final String DEFAULT_IMPL = "defaultImpl";
    private static final String ENTRY = "entry";
    private static final String ENUM = "enum";
    private static final String COLUMN_MODE = "columnMode";
    private static final String PACK = "pack";
    private static final String SEP = "sep";
    private static final String FIX = "fix";
    private static final String BLOCK = "block";

    private static final Set<String> reserved = Set.of(COMMENT, NULLABLE,
            ENUM_REF, DEFAULT_IMPL, ENTRY, ENUM, COLUMN_MODE, PACK, SEP, FIX, BLOCK);

    public static void addTag(Metadata meta, String tag) {
        if (reserved.contains(tag)) {
            throw new IllegalArgumentException(STR. "'\{ tag }' reserved" );
        }
        MetaValue old = meta.data().putLast(tag, TAG);
        if (old != null) {
            throw new IllegalArgumentException(STR. "'\{ tag }' duplicated" );
        }
    }

    public static String putComment(Metadata meta, String comment) {
        MetaValue value = meta.data().putLast(COMMENT, new MetaStr(comment));
        if (value instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public static String removeComment(Metadata meta) {
        MetaValue obj = meta.data().remove(COMMENT);
        if (obj instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public static void putNullable(Metadata meta) {
        meta.data().putFirst(NULLABLE, TAG);
    }

    public static boolean removeNullable(Metadata meta) {
        return meta.data().remove(NULLABLE) != null;
    }

    public static void putEnumRef(Metadata meta, String enumRef) {
        meta.data().putFirst(ENUM_REF, new MetaStr(enumRef));
    }

    public static String removeEnumRef(Metadata meta) {
        MetaValue enumRef = meta.data().remove(ENUM_REF);
        if (enumRef instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public static void putDefaultImpl(Metadata meta, String defaultImpl) {
        meta.data().putFirst(DEFAULT_IMPL, new MetaStr(defaultImpl));
    }

    public static String removeDefaultImpl(Metadata meta) {
        MetaValue defaultImpl = meta.data().remove(DEFAULT_IMPL);
        if (defaultImpl instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public static void putEntry(Metadata meta, EntryType entry) {
        switch (entry) {
            case ENo.NO -> {
            }
            case EEntry anEntry -> meta.data().putFirst(ENTRY, new MetaStr(anEntry.field()));
            case EEnum anEnum -> meta.data().putFirst(ENUM, new MetaStr(anEnum.field()));
        }
    }

    public static EntryType removeEntry(Metadata meta) {
        SequencedMap<String, MetaValue> data = meta.data();
        MetaValue entry = data.remove(ENTRY);
        if (entry instanceof MetaStr ms) {
            return new EEntry(ms.value());
        }

        MetaValue anEnum = data.remove(ENUM);
        if (anEnum instanceof MetaStr ms) {
            return new EEnum(ms.value());
        }
        return ENo.NO;
    }

    public static void putColumnMode(Metadata meta) {
        meta.data().putFirst(COLUMN_MODE, TAG);
    }

    public static boolean removeColumnMode(Metadata meta) {
        return meta.data().remove(COLUMN_MODE) != null;
    }

    public static void putFmt(Metadata meta, FieldFormat fmt) {
        SequencedMap<String, MetaValue> data = meta.data();
        switch (fmt) {
            case AutoOrPack.AUTO -> {
            }
            case AutoOrPack.PACK -> data.putFirst(PACK, TAG);
            case Sep sep -> data.putFirst(SEP, new MetaStr(String.valueOf(sep.sep())));
            case Fix fix -> data.putFirst(FIX, new MetaInt(fix.count()));
            case Block block -> data.putFirst(BLOCK, new MetaInt(block.fix()));
        }
    }

    public static FieldFormat removeFmt(Metadata meta) {
        SequencedMap<String, MetaValue> data = meta.data();
        if (data.remove(PACK) != null) {
            return AutoOrPack.PACK;
        }

        MetaValue sep = data.remove(SEP);
        if (sep instanceof MetaStr ms) {
            return new Sep(ms.value().charAt(0));
        }

        MetaValue fix = data.remove(FIX);
        if (fix instanceof MetaInt mi) {
            return new Fix(mi.value());
        }

        MetaValue block = data.remove(BLOCK);
        if (block instanceof MetaInt mi) {
            return new Block(mi.value());
        }

        return AutoOrPack.AUTO;
    }
}

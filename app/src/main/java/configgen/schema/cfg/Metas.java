package configgen.schema.cfg;

import configgen.schema.EntryType;
import configgen.schema.EntryType.EEntry;
import configgen.schema.EntryType.EEnum;
import configgen.schema.EntryType.ENo;
import configgen.schema.FieldFormat;
import configgen.schema.Metadata;
import configgen.schema.Metadata.MetaInt;

import java.util.SequencedMap;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.Metadata.*;

import static configgen.schema.FieldFormat.*;
import static configgen.schema.Metadata.MetaTag.TAG;

public class Metas {

    public static void putComment(Metadata meta, String comment) {
        meta.data().putLast("__comment", new Metadata.MetaStr(comment));
    }

    public static String removeComment(Metadata meta) {
        MetaValue obj = meta.data().remove("__comment");
        if (obj instanceof MetaStr ms) {
            return ms.value();
        }
        return "";
    }

    public static void putNullable(Metadata meta) {
        meta.data().putFirst("nullable", TAG);
    }

    public static boolean removeNullable(Metadata meta) {
        return meta.data().remove("nullable") != null;
    }

    public static void putEntry(Metadata meta, EntryType entry) {
        switch (entry) {
            case ENo.NO -> {
            }
            case EEntry anEntry -> meta.data().putFirst("entry", new MetaStr(anEntry.field()));
            case EEnum anEnum -> meta.data().putFirst("enum", new MetaStr(anEnum.field()));
        }
    }

    public static EntryType removeEntry(Metadata meta) {
        SequencedMap<String, MetaValue> data = meta.data();
        MetaValue entry = data.remove("entry");
        if (entry instanceof MetaStr ms) {
            return new EEntry(ms.value());
        }

        MetaValue anEnum = data.remove("enum");
        if (anEnum instanceof MetaStr ms) {
            return new EEnum(ms.value());
        }
        return ENo.NO;
    }

    public static void putFmt(Metadata meta, FieldFormat fmt) {
        SequencedMap<String, MetaValue> data = meta.data();
        switch (fmt) {
            case AUTO -> {
            }
            case PACK -> data.putFirst("pack", TAG);
            case Sep sep -> data.putFirst("sep", new MetaStr(String.valueOf(sep.sep())));
            case Fix fix -> data.putFirst("fix", new MetaInt(fix.count()));
            case Block block -> data.putFirst("block", new MetaInt(block.fix()));
        }
    }

    public static FieldFormat removeFmt(Metadata meta) {
        SequencedMap<String, MetaValue> data = meta.data();
        if (data.remove("pack") != null) {
            return PACK;
        }

        MetaValue sep = data.remove("sep");
        if (sep instanceof MetaStr ms) {
            return new Sep(ms.value().charAt(0));
        }

        MetaValue fix = data.remove("fix");
        if (fix instanceof MetaInt mi) {
            return new Fix(mi.value());
        }

        MetaValue block = data.remove("block");
        if (block instanceof MetaInt mi) {
            return new Block(mi.value());
        }

        data.remove("auto");
        return AUTO;
    }
}

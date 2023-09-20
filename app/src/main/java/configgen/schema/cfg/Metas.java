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

    static void putEntry(Metadata meta, EntryType entry) {
        switch (entry) {
            case ENo.NO -> {
            }
            case EEntry anEntry -> meta.data().putFirst("entry", new MetaStr(anEntry.field()));
            case EEnum anEnum -> meta.data().putFirst("enum", new MetaStr(anEnum.field()));
        }
    }

    static EntryType removeEntry(Metadata meta) {
        SequencedMap<String, MetaValue> data = meta.data();
        MetaValue entry = data.remove("entry");
        if (entry != null) {
            return new EEntry(((MetaStr) entry).value());
        }

        MetaValue anEnum = data.remove("enum");
        if (anEnum != null) {
            return new EEnum(((MetaStr) anEnum).value());
        }
        return ENo.NO;
    }

    static void putFmt(Metadata meta, FieldFormat fmt) {
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

    static FieldFormat removeFmt(Metadata meta) {
        SequencedMap<String, MetaValue> data = meta.data();
        if (data.remove("pack") != null) {
            return PACK;
        }

        MetaValue sep = data.remove("sep");
        if (sep != null) {
            return new Sep(((MetaStr) sep).value().charAt(0));
        }

        MetaValue fix = data.remove("fix");
        if (fix != null) {
            return new Fix(((MetaInt) fix).value());
        }

        MetaValue block = data.remove("block");
        if (block != null) {
            return new Block(((MetaInt) block).value());
        }

        data.remove("auto");
        return AUTO;
    }
}

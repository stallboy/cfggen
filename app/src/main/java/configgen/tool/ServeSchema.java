package configgen.tool;

import com.alibaba.fastjson2.annotation.JSONField;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;

import java.util.ArrayList;
import java.util.List;


public class ServeSchema {

    public interface SNameable {
        String name();

    }

    public record SField(String name,
                         String type,
                         String comment) {
    }

    public enum SRefType {
        rPrimary,
        rUniq,
        rList
    }

    public record SForeignKey(String name,
                              List<String> keys,
                              String refTable,
                              SRefType refType,
                              List<String> refKeys) {
    }

    public record SStruct(String name,
                          List<SField> fields,
                          List<SForeignKey> foreignKeys) implements SNameable {

        @JSONField
        public String type() {
            return "struct";
        }

    }

    public enum SEntryType {
        eNo,
        eEnum,
        eEntry,
    }

    public record SInterface(String name,
                             String enumRef,
                             String defaultImpl,
                             List<SStruct> impls) implements SNameable {

        @JSONField
        public String type() {
            return "interface";
        }
    }

    public record STable(String name,
                         List<String> pk,
                         List<List<String>> uks,
                         SEntryType entryType,
                         String entryField,
                         List<SField> fields,
                         List<SForeignKey> foreignKeys) implements SNameable {
        @JSONField
        public String type() {
            return "table";
        }
    }

    public record Schema(List<SNameable> items) {
    }


    public static Schema fromCfgSchema(CfgSchema cfgSchema) {
        return new Schema(cfgSchema.items().stream().map(ServeSchema::fromNameable).toList());
    }


    public static SNameable fromNameable(Nameable n) {
        return switch (n) {
            case InterfaceSchema is -> fromInterface(is);
            case StructSchema ss -> fromStruct(ss);
            case TableSchema ts -> fromTable(ts);
        };
    }

    public static SInterface fromInterface(InterfaceSchema is) {
        return new SInterface(is.name(),
                is.enumRef(),
                is.defaultImpl(),
                is.impls().stream().map(ServeSchema::fromStruct).toList());
    }

    public static SStruct fromStruct(StructSchema ss) {
        return new SStruct(ss.name(),
                fromFields(ss.fields()),
                fromFks(ss.foreignKeys()));
    }

    public static STable fromTable(TableSchema ts) {
        SEntryType entryType;
        String entryField;
        switch (ts.entry()) {
            case EntryType.ENo _ -> {
                entryType = SEntryType.eNo;
                entryField = null;
            }
            case EntryType.EEntry eEntry -> {
                entryType = SEntryType.eEntry;
                entryField = eEntry.field();
            }
            case EntryType.EEnum eEnum -> {
                entryType = SEntryType.eEnum;
                entryField = eEnum.field();
            }
        }
        return new STable(ts.name(),
                ts.primaryKey().fields(),
                ts.uniqueKeys().stream().map(KeySchema::fields).toList(),
                entryType,
                entryField,
                fromFields(ts.fields()),
                fromFks(ts.foreignKeys()));
    }

    private static List<SField> fromFields(List<FieldSchema> fields) {
        return fields.stream().map(f -> new SField(f.name(), CfgWriter.typeStr(f.type()), f.comment())).toList();
    }

    private static List<SForeignKey> fromFks(List<ForeignKeySchema> fks) {
        List<SForeignKey> res = new ArrayList<>(fks.size());
        for (ForeignKeySchema f : fks) {
            SRefType ref;
            List<String> refKeys = null;
            switch (f.refKey()) {
                case RefKey.RefPrimary _ -> {
                    ref = SRefType.rPrimary;
                }
                case RefKey.RefUniq refUniq -> {
                    ref = SRefType.rUniq;
                    refKeys = refUniq.keyNames();
                }
                case RefKey.RefList refList -> {
                    ref = SRefType.rList;
                    refKeys = refList.keyNames();
                }
            }
            res.add(new SForeignKey(f.name(), f.key().fields(), f.refTable(), ref, refKeys));
        }
        return res;
    }
}

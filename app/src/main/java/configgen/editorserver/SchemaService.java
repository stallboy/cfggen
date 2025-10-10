package configgen.editorserver;

import com.alibaba.fastjson2.annotation.JSONField;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;


public class SchemaService {

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
        rList,
        rNullablePrimary,
        rNullableUniq
    }

    public record SForeignKey(String name,
                              List<String> keys,
                              String refTable,
                              SRefType refType,
                              List<String> refKeys) {
    }

    public record SStruct(String name,
                          String comment,
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
                             String comment,
                             String enumRef,
                             String defaultImpl,
                             List<SStruct> impls) implements SNameable {

        @JSONField
        public String type() {
            return "interface";
        }
    }

    public record RecordId(String id,
                           String title) {
    }

    public record STable(String name,
                         String comment,
                         List<String> pk,
                         List<List<String>> uks,
                         SEntryType entryType,
                         String entryField,
                         List<SField> fields,
                         List<SForeignKey> foreignKeys,
                         List<RecordId> recordIds,
                         boolean isEditable) implements SNameable {
        @JSONField
        public String type() {
            return "table";
        }
    }

    /**
     * 结构信息
     * @param isEditable 因为可能是partial，此时不能被编辑
     */
    public record Schema(boolean isEditable,
                         List<SNameable> items,
                         Map<String, Map<String, Long>> lastModifiedMap) {
    }


    public static Schema fromCfgSchema(CfgSchema cfgSchema) {
        return new Schema(!cfgSchema.isPartial(),
                cfgSchema.items().stream().map(n -> fromNameable(n, null)).toList(),
                Map.of());
    }

    public static Schema fromCfgValue(CfgValue cfgValue) {
        return new Schema(!cfgValue.schema().isPartial(),
                cfgValue.schema().items().stream().map(n -> fromNameable(n, cfgValue)).toList(),
                cfgValue.valueStat().getLastModifiedMap());
    }

    public static SNameable fromNameable(Nameable n, CfgValue cfgValue) {
        return switch (n) {
            case InterfaceSchema is -> fromInterface(is);
            case StructSchema ss -> fromStruct(ss);
            case TableSchema ts -> fromTable(ts, cfgValue);
        };
    }

    public static SInterface fromInterface(InterfaceSchema is) {
        return new SInterface(
                is.name(),
                is.comment(),
                is.nullableEnumRefTable() != null ? is.nullableEnumRefTable().name() : "", // 全局名字空间
                is.defaultImpl(),
                is.impls().stream().map(SchemaService::fromStruct).toList());
    }

    public static SStruct fromStruct(StructSchema ss) {
        return new SStruct(
                ss.name(),
                ss.comment(),
                fromFields(ss.fields()),
                fromFks(ss.foreignKeys()));
    }

    public static STable fromTable(TableSchema ts, CfgValue cfgValue) {
        SEntryType entryType;
        String entryField;
        switch (ts.entry()) {
            case EntryType.ENo ignored -> {
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

        VTable vTable = cfgValue != null ? cfgValue.vTableMap().get(ts.name()) : null;
        List<RecordId> recordIds = getRecordIds(vTable);

        return new STable(
                ts.name(),
                ts.comment(),
                ts.primaryKey().fields(),
                ts.uniqueKeys().stream().map(KeySchema::fields).toList(),
                entryType,
                entryField,
                fromFields(ts.fields()),
                fromFks(ts.foreignKeys()),
                recordIds,
                ts.isJson());
    }

    public static List<RecordId> getRecordIds(VTable vTable) {
        if (vTable == null) {
            return List.of();
        }
        List<RecordId> recordIds = new ArrayList<>(vTable.primaryKeyMap().size());
        for (Map.Entry<Value, VStruct> e : vTable.primaryKeyMap().sequencedEntrySet()) {
            Value pk = e.getKey();
            VStruct vStruct = e.getValue();
            recordIds.add(new RecordId(pk.packStr(), RecordService.getBriefTitle(vStruct)));
        }
        return recordIds;
    }

    private static List<SField> fromFields(List<FieldSchema> fields) {
        return fields.stream().map(f -> new SField(f.name(), CfgWriter.typeStrWithFullName(f.type()), f.comment())).toList();
    }

    private static List<SForeignKey> fromFks(List<ForeignKeySchema> fks) {
        List<SForeignKey> res = new ArrayList<>(fks.size());
        for (ForeignKeySchema f : fks) {
            SRefType ref;
            List<String> refKeys = null;
            switch (f.refKey()) {
                case RefKey.RefPrimary refPrimary -> {
                    ref = refPrimary.nullable() ? SRefType.rNullablePrimary : SRefType.rPrimary;
                }
                case RefKey.RefUniq refUniq -> {
                    ref = refUniq.nullable() ? SRefType.rNullableUniq : SRefType.rUniq;
                    refKeys = refUniq.keyNames();
                }
                case RefKey.RefList refList -> {
                    ref = SRefType.rList;
                    refKeys = refList.keyNames();
                }
            }
            res.add(new SForeignKey(f.name(),
                    f.key().fields(),
                    f.refTableSchema().fullName(),  //用full name，避免让client去先local后global的去解析名字空间。
                    ref,
                    refKeys));
        }
        return res;
    }
}

package configgen.data;

import configgen.Logger;
import configgen.schema.*;
import configgen.schema.EntryType.EEntry;
import configgen.schema.EntryType.EEnum;
import configgen.schema.FieldType.Primitive;
import configgen.schema.cfg.CfgUtil;
import configgen.schema.cfg.Metas;

import java.util.*;

import static configgen.data.CfgData.DField;
import static configgen.schema.EntryType.ENo.NO;
import static configgen.schema.FieldFormat.AutoOrPack;

public class CfgSchemaAlignToData {
    private final CfgSchema cfgSchema;
    private final CfgData cfgData;
    private final SchemaErrs errs;

    public CfgSchemaAlignToData(CfgSchema cfgSchema, CfgData cfgData, SchemaErrs errs) {
        this.cfgSchema = cfgSchema;
        this.cfgData = cfgData;
        this.errs = errs;
    }

    /**
     * @return align到cfgData后的cfgSchema，未resolve
     */
    public CfgSchema align() {
        TreeMap<String, CfgData.DTable> dataHeaders = new TreeMap<>(cfgData.tables());
        CfgSchema alignedCfg = CfgSchema.of();
        for (Nameable item : cfgSchema.items()) {
            switch (item) {
                case Fieldable fieldable -> {
                    alignedCfg.add(fieldable.copy());
                }
                case TableSchema table -> {
                    CfgData.DTable th = dataHeaders.remove(table.name());
                    if (th != null) {
                        TableSchema alignedTable = alignTable(table, th.fields());
                        alignedCfg.add(alignedTable);
                    }
                }
            }
        }

        for (CfgData.DTable th : dataHeaders.values()) {
            TableSchema newTable = newTable(th);
            alignedCfg.add(newTable);
        }
        return alignedCfg;
    }

    TableSchema newTable(CfgData.DTable th) {
        if (th.fields().isEmpty()) {
            Logger.log(STR. "\{ th.tableName() } header empty, ignored!" );
            return null;
        }

        List<FieldSchema> fields = new ArrayList<>(th.fields().size());
        for (DField hf : th.fields()) {
            Metadata meta = Metadata.of();
            if (!hf.comment().isEmpty()) {
                Metas.putComment(meta, hf.comment());
            }

            FieldSchema field = new FieldSchema(hf.name(), Primitive.STRING, AutoOrPack.AUTO, meta);
            fields.add(field);
        }

        String first = fields.iterator().next().name();
        KeySchema primaryKey = new KeySchema(List.of(first));

        return new TableSchema(th.tableName(), primaryKey, NO, false,
                Metadata.of(), fields, List.of(), List.of());
    }

    TableSchema alignTable(TableSchema table, List<DField> header) {
        String name = table.name();
        Map<String, FieldSchema> fieldSchemas = alignFields(table, header);
        if (fieldSchemas.isEmpty()) {
            return null;
        }

        KeySchema primaryKey;
        if (!isKeyInSchemaList(table.primaryKey(), fieldSchemas)) {
            String first = fieldSchemas.keySet().iterator().next();
            primaryKey = new KeySchema(List.of(first));
        } else {
            primaryKey = table.primaryKey().copy();
        }

        EntryType entry = NO;
        switch (table.entry()) {
            case NO -> {
            }
            case EEntry ee -> {
                if (fieldSchemas.containsKey(ee.field())) {
                    entry = new EEntry(ee.field());
                }
            }
            case EEnum ee -> {
                if (fieldSchemas.containsKey(ee.field())) {
                    entry = new EEnum(ee.field());
                }
            }
        }

        boolean isColumnMode = table.isColumnMode();
        Metadata meta = table.meta().copy();
        List<FieldSchema> fields = new ArrayList<>(fieldSchemas.values());

        List<ForeignKeySchema> fks = new ArrayList<>();
        for (ForeignKeySchema fk : table.foreignKeys()) {
            if (isKeyInSchemaList(fk.key(), fieldSchemas)) {
                fks.add(fk.copy());
            }
        }

        List<KeySchema> uks = new ArrayList<>();
        for (KeySchema uk : table.uniqueKeys()) {
            if (isKeyInSchemaList(uk, fieldSchemas)) {
                uks.add(uk.copy());
            }
        }

        return new TableSchema(name, primaryKey, entry, isColumnMode, meta, fields, fks, uks);
    }

    private boolean isKeyInSchemaList(KeySchema key, Map<String, FieldSchema> fieldSchemas) {
        for (String k : key.name()) {
            if (!fieldSchemas.containsKey(k)) {
                return false;
            }
        }
        return true;
    }


    private Map<String, FieldSchema> alignFields(TableSchema table, List<DField> header) {
        Map<String, FieldSchema> curFields = new LinkedHashMap<>();
        for (FieldSchema field : table.fields()) {
            curFields.put(field.name(), field);
        }

        Map<String, FieldSchema> alignedFields = new LinkedHashMap<>();
        int size = header.size();
        for (int idx = 0; idx < size; ) {
            DField hf = header.get(idx);
            String name = hf.name();
            String comment = hf.comment();

            FieldSchema newField;
            FieldSchema curField = findAndRemove(header, idx, curFields);
            if (curField != null) {
                int span = Spans.span(curField);
                idx += span;
                String fieldName = curField.name();
                Metadata meta = curField.meta().copy();
                if (!comment.isEmpty() && !comment.equalsIgnoreCase(fieldName)) {
                    String old = Metas.putComment(meta, comment);
                    if (!old.equals(comment)) {
                        Logger.log(STR. "\{ table.name() }[\{ fieldName }] set comment: \{ old } -> \{ comment }" );
                    }
                } else {
                    String old = Metas.removeComment(meta);
                    if (!old.isEmpty()) {
                        Logger.log(STR. "\{ table.name() }[\{ fieldName }] remove old comment: \{ old }" );
                    }
                }
                newField = new FieldSchema(fieldName, curField.type().copy(), curField.fmt(), meta);
                alignedFields.put(newField.name(), newField);

            } else {
                idx++;

                if (CfgUtil.isIdentifier(name)) {
                    Metadata meta = Metadata.of();
                    if (!comment.isEmpty()) {
                        Metas.putComment(meta, comment);
                    }
                    newField = new FieldSchema(name, Primitive.STRING, AutoOrPack.AUTO, meta);
                    Logger.log(STR. "\{ table.name() } new field: \{ name }" );
                    alignedFields.put(newField.name(), newField);
                } else {
                    errs.addErr(new SchemaErrs.DataHeadNameNotIdentifier(table.name(), name));
                }
            }
        }

        for (FieldSchema remove : curFields.values()) {
            Logger.log(STR. "\{ table.name() } delete field: \{ remove.name() }" );
        }
        return alignedFields;
    }


    private FieldSchema findAndRemove(List<DField> headers, int index, Map<String, FieldSchema> curFields) {
        String name = headers.get(index).name();
        FieldSchema fs = curFields.remove(name);
        if (fs != null) {
            return fs;
        }

        //// 以下是为兼容之前的做法
        if (!name.endsWith("1")) {
            return null;
        }

        String nam = name.substring(0, name.length() - 1);
        String listName = STR. "\{ nam }List" ;
        FieldSchema listField = curFields.get(listName);
        if (listField != null
                && listField.type() instanceof FieldType.FList fList && Spans.span(fList.item()) == 1
                && listField.fmt() instanceof FieldFormat.Fix fix && headers.size() > index + fix.count() - 1) {

            boolean ok = true;
            for (int i = 2; i <= fix.count(); i++) {
                if (!headers.get(index + i - 1).name().equals(STR. "\{ nam }\{ i }" )) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                curFields.remove(listName);
                return listField;
            }
        }

        if (headers.size() <= index + 1) {
            return null;
        }
        String name2 = headers.get(index + 1).name();
        if (!name2.endsWith("1")) {
            return null;
        }
        String nam2 = name2.substring(0, name2.length() - 1);
        String mapName = STR. "\{ nam }2\{ nam2 }Map" ;
        FieldSchema mapField = curFields.get(mapName);
        if (mapField != null
                && mapField.type() instanceof FieldType.FMap fMap
                && Spans.span(fMap.key()) == 1 && Spans.span(fMap.value()) == 1
                && mapField.fmt() instanceof FieldFormat.Fix fix
                && headers.size() > index + fix.count() * 2 - 1) {

            boolean ok = true;
            for (int i = 2; i <= fix.count(); i++) {
                if (!headers.get(index + (i - 1) * 2).name().equals(STR. "\{ nam }\{ i }" )) {
                    ok = false;
                    break;
                }

                if (!headers.get(index + (i - 1) * 2 + 1).name().equals(STR. "\{ nam2 }\{ i }" )) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                curFields.remove(mapName);
                return mapField;
            }
        }
        return null;
    }

}

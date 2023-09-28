package configgen.data;

import configgen.Logger;
import configgen.schema.*;
import configgen.schema.EntryType.EEntry;
import configgen.schema.EntryType.EEnum;
import configgen.schema.FieldType.Primitive;
import configgen.schema.cfg.Cfgs;
import configgen.schema.cfg.Metas;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static configgen.data.CfgDataHeader.*;
import static configgen.schema.EntryType.ENo.NO;
import static configgen.schema.FieldFormat.AutoOrPack;

public enum CfgSchemaAlignToDataHeader {
    INSTANCE;

    public CfgSchema align(CfgSchema cfgSchema, CfgData cfgData) {
        CfgDataHeader header = CfgDataHeader.of(cfgData, cfgSchema);
        return align(cfgSchema, header);
    }

    public CfgSchema align(CfgSchema cfg, CfgDataHeader header) {
        Map<String, TableDataHeader> dataHeaders = new TreeMap<>(header.tables());
        CfgSchema alignedCfg = CfgSchema.of();
        for (Nameable item : cfg.items()) {
            switch (item) {
                case Fieldable fieldable -> {
                    alignedCfg.add(fieldable.copy());
                }
                case TableSchema table -> {
                    TableDataHeader th = dataHeaders.remove(table.name());
                    if (th != null) {
                        TableSchema alignedTable = alignTable(table, th);
                        alignedCfg.add(alignedTable);
                    }
                }
            }
        }

        for (TableDataHeader th : dataHeaders.values()) {
            TableSchema newTable = newTable(th);
            alignedCfg.add(newTable);
        }
        return alignedCfg;
    }

    private TableSchema newTable(TableDataHeader th) {
        List<FieldSchema> fields = new ArrayList<>(th.fields().size());
        for (HeaderField hf : th.fields()) {
            Metadata meta = Metadata.of();
            if (!hf.comment().isEmpty()) {
                Metas.putComment(meta, hf.comment());
            }

            FieldSchema field = new FieldSchema(hf.name(), Primitive.STR, AutoOrPack.AUTO, meta);
            fields.add(field);
        }

        if (fields.isEmpty()) {
            Logger.log(STR. "\{ th.name() } header empty, ignored!" );
            return null;
        }

        String first = fields.iterator().next().name();
        KeySchema primaryKey = new KeySchema(List.of(first));

        return new TableSchema(th.name(), primaryKey, NO, false, Metadata.of(), fields, List.of(), List.of());
    }

    private TableSchema alignTable(TableSchema table, TableDataHeader header) {
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


    private Map<String, FieldSchema> alignFields(TableSchema table, TableDataHeader header) {
        Map<String, FieldSchema> curFields = new LinkedHashMap<>();
        for (FieldSchema field : table.fields()) {
            curFields.put(field.name(), field);
        }

        Map<String, FieldSchema> alignedFields = new LinkedHashMap<>();
        int size = header.fields().size();
        for (int idx = 0; idx < size; ) {
            HeaderField hf = header.fields().get(idx);
            String name = hf.name();
            String comment = hf.comment();

            FieldSchema newField;
            FieldSchema curField = findAndRemove(header.fields(), idx, curFields);
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

                Pattern pattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
                if (!pattern.matcher(name).matches()) {
                    Logger.log(STR. "\{ table.name() }[\{ name }] not identifier, ignore!" );
                } else {
                    Metadata meta = Metadata.of();
                    if (!comment.isEmpty()) {
                        Metas.putComment(meta, comment);
                    }
                    newField = new FieldSchema(name, Primitive.STR, AutoOrPack.AUTO, meta);
                    Logger.log(STR. "\{ table.name() } new field: \{ name }" );
                    alignedFields.put(newField.name(), newField);
                }
            }
        }

        for (FieldSchema remove : curFields.values()) {
            Logger.log(STR. "\{ table.name() } delete field: \{ remove.name() }" );
        }
        return alignedFields;
    }


    private FieldSchema findAndRemove(List<HeaderField> headers, int index, Map<String, FieldSchema> curFields) {
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


    public static void main(String[] args) {
        Logger.enableVerbose();
        Logger.mm("start readCfgData");
        CfgData cfgData = CfgDataReader.INSTANCE.readCfgData(Path.of("."));
        Logger.mm("end readCfgData");

        cfgData.stat().print();
        System.out.println("table\t" + cfgData.tables().size());

        CfgSchema cfgSchema = Cfgs.readFrom(Path.of("config.cfg"), true);
        SchemaErrs errs = cfgSchema.resolve();
        errs.print();
        CfgSchema alignedSchema = CfgSchemaAlignToDataHeader.INSTANCE.align(cfgSchema, cfgData);

        System.out.println(cfgSchema.equals(alignedSchema));
        cfgSchema.printDiff(alignedSchema);

        SchemaErrs errs2 = alignedSchema.resolve();
        errs2.print();
    }

}

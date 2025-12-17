package configgen.data;

import configgen.ctx.HeadRow;
import configgen.util.Logger;
import configgen.schema.*;
import configgen.schema.EntryType.EEntry;
import configgen.schema.EntryType.EEnum;
import configgen.schema.FieldType.Primitive;
import configgen.schema.cfg.CfgUtil;

import java.util.*;

import static configgen.data.CfgData.DField;
import static configgen.schema.EntryType.ENo.NO;
import static configgen.schema.FieldFormat.AutoOrPack.*;

public record CfgSchemaAlignToData(HeadRow headRow) {

    public CfgSchemaAlignToData {
        Objects.requireNonNull(headRow);
    }

    /**
     * @return align到cfgData后的cfgSchema，未resolve
     */
    public CfgSchema align(CfgSchema cfgSchema, CfgData cfgData, CfgSchemaErrs errs) {
        Objects.requireNonNull(cfgSchema);
        Objects.requireNonNull(cfgData);
        Objects.requireNonNull(errs);

        TreeMap<String, CfgData.DTable> dataHeaders = new TreeMap<>(cfgData.tables());
        CfgSchema alignedCfg = CfgSchema.of();
        for (Nameable item : cfgSchema.items()) {
            switch (item) {
                case Fieldable fieldable -> {
                    alignedCfg.add(fieldable.copy());
                }
                case TableSchema table -> {
                    CfgData.DTable th = dataHeaders.remove(table.name());

                    if (table.isJson()) {
                        alignedCfg.add(table.copy());
                        if (th != null) {
                            // 用json，就不能用excel
                            List<String> sheets = new ArrayList<>();
                            for (CfgData.DRawSheet rawSheet : th.rawSheets()) {
                                sheets.add(String.format("%s[%s]", rawSheet.relativeFilePath(), rawSheet.sheetName()));
                            }
                            errs.addErr(new CfgSchemaErrs.JsonTableNotSupportExcel(table.name(), sheets));
                        }
                    } else {
                        if (th != null) {
                            TableSchema alignedTable = alignTable(table, th.fields(), errs);
                            alignedCfg.add(alignedTable);
                        }
                    }
                }
            }
        }

        for (CfgData.DTable th : dataHeaders.values()) {
            TableSchema newTable = newTableSchema(th, errs);
            alignedCfg.add(newTable);
        }
        return alignedCfg;
    }

    TableSchema newTableSchema(CfgData.DTable th, CfgSchemaErrs errs) {
        List<FieldSchema> fields = new ArrayList<>(th.fields().size());
        for (DField hf : th.fields()) {
            if (CfgUtil.isIdentifier(hf.name())) {
                FieldSchema field = newFieldSchema(hf, th.tableName(), errs);
                fields.add(field);
            } else {
                errs.addErr(new CfgSchemaErrs.DataHeadNameNotIdentifier(th.tableName(), hf.name()));
            }
        }

        if (fields.isEmpty()) {
            Logger.log("%s header empty, ignored!", th.tableName());
            return null;
        }

        String first = fields.getFirst().name();
        KeySchema primaryKey = new KeySchema(List.of(first));

        Metadata metadata = Metadata.of();
        String tag = th.nullableAddTag();
        if (tag != null && !tag.isEmpty()) {
            metadata.putTag(tag);
        }

        return new TableSchema(th.tableName(), primaryKey, NO, false,
                metadata, fields, List.of(), List.of());
    }

    private FieldSchema newFieldSchema(DField hf, String tableName, CfgSchemaErrs errs) {
        Metadata meta = Metadata.of();
        if (!hf.comment().isEmpty()) {
            meta.putComment(hf.comment());
        }
        FieldType type;
        String typeStr = hf.suggestedType();
        if (typeStr != null && !typeStr.isEmpty()) {
            type = headRow.parseType(typeStr);
            if (type == null) {
                errs.addWarn(new CfgSchemaErrs.SuggestTypeUnknown(tableName, hf.name(), typeStr));
                type = Primitive.STRING;
            }
        } else {
            type = Primitive.STRING;
        }
        return new FieldSchema(hf.name(), type, AUTO, meta);
    }

    TableSchema alignTable(TableSchema table, List<DField> header, CfgSchemaErrs errs) {
        String name = table.name();
        Map<String, FieldSchema> fieldSchemas = alignFields(table, header, errs);
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

        List<ForeignKeySchema> fks = new ArrayList<>(table.foreignKeys().size());
        for (ForeignKeySchema fk : table.foreignKeys()) {
            if (isKeyInSchemaList(fk.key(), fieldSchemas)) {
                fks.add(fk.copy());
            }
        }

        List<KeySchema> uks = new ArrayList<>(table.uniqueKeys().size());
        for (KeySchema uk : table.uniqueKeys()) {
            if (isKeyInSchemaList(uk, fieldSchemas)) {
                uks.add(uk.copy());
            }
        }

        return new TableSchema(name, primaryKey, entry, isColumnMode, meta, fields, fks, uks);
    }

    private boolean isKeyInSchemaList(KeySchema key, Map<String, FieldSchema> fieldSchemas) {
        for (String k : key.fields()) {
            if (!fieldSchemas.containsKey(k)) {
                return false;
            }
        }
        return true;
    }


    private Map<String, FieldSchema> alignFields(TableSchema table, List<DField> header, CfgSchemaErrs errs) {
        Map<String, FieldSchema> curFields = new LinkedHashMap<>(table.fields().size());
        for (FieldSchema field : table.fields()) {
            curFields.put(field.name(), field);
        }

        Map<String, FieldSchema> alignedFields = new LinkedHashMap<>(table.fields().size());
        int size = header.size();
        for (int idx = 0; idx < size; ) {
            DField hf = header.get(idx);
            String comment = hf.comment();

            FieldSchema newField;
            FieldSchema curField = findAndRemove(header, idx, curFields);
            if (curField != null) {
                int span = Span.fieldSpan(curField);
                idx += span;
                String fieldName = curField.name();
                Metadata meta = curField.meta().copy();
                if (!comment.isEmpty() && !comment.equalsIgnoreCase(fieldName)) {
                    String old = meta.putComment(comment);
                    if (!old.equals(comment)) {
                        Logger.log("%s[%s] set comment: %s -> %s", table.name(), fieldName, old, comment);
                    }
                } else {
                    String old = meta.removeComment();
                    if (!old.isEmpty()) {
                        Logger.log("%s[%s] remove old comment: %s", table.name(), fieldName, old);
                    }
                }
                newField = new FieldSchema(fieldName, curField.type().copy(), curField.fmt(), meta);
                FieldSchema old = alignedFields.put(newField.name(), newField);
                if (old != null) {
                    errs.addErr(new CfgSchemaErrs.DataHeadNameDuplicated(table.name(), fieldName));
                }

            } else {
                idx++;

                if (CfgUtil.isIdentifier(hf.name())) {
                    newField = newFieldSchema(hf, table.fullName(), errs);
                    Logger.log("%s new field: %s", table.name(), hf.name());
                    FieldSchema old = alignedFields.put(newField.name(), newField);
                    if (old != null) {
                        errs.addErr(new CfgSchemaErrs.DataHeadNameDuplicated(table.name(), newField.name()));
                    }

                } else {
                    errs.addErr(new CfgSchemaErrs.DataHeadNameNotIdentifier(table.name(), hf.name()));
                }
            }
        }

        for (FieldSchema remove : curFields.values()) {
            Logger.log("%s delete field: %s", table.name(), remove.name());
        }
        return alignedFields;
    }


    private FieldSchema findAndRemove(List<DField> headers, int index, Map<String, FieldSchema> curFields) {
        String name = headers.get(index).name();
        FieldSchema fs = curFields.remove(name);
        if (fs != null) {
            return fs;
        }

        //// 以下是为兼容之前的做法,
        //// - 允许a1,a2,a3..代表aList
        //// - 允许a1,b1, a2, b2,..代表a2bMap
        if (!name.endsWith("1")) {
            return null;
        }

        String nam = name.substring(0, name.length() - 1);
        String listName = String.format("%sList", nam);
        FieldSchema listField = curFields.get(listName);
        if (listField != null
                && listField.type() instanceof FieldType.FList(
                FieldType.SimpleType item
        ) && Span.simpleTypeSpan(item) == 1
                && listField.fmt() instanceof Fix(int count) && headers.size() > index + count - 1) {

            boolean ok = true;
            for (int i = 2; i <= count; i++) {
                if (!headers.get(index + i - 1).name().equals(String.format("%s%d", nam, i))) {
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
        String mapName = String.format("%s2%sMap", nam, nam2);
        FieldSchema mapField = curFields.get(mapName);
        if (mapField != null
                && mapField.type() instanceof FieldType.FMap(FieldType.SimpleType key, FieldType.SimpleType value)
                && Span.simpleTypeSpan(key) == 1 && Span.simpleTypeSpan(value) == 1
                && mapField.fmt() instanceof Fix(int count)
                && headers.size() > index + count * 2 - 1) {

            boolean ok = true;
            for (int i = 2; i <= count; i++) {
                if (!headers.get(index + (i - 1) * 2).name().equals(String.format("%s%d", nam, i))) {
                    ok = false;
                    break;
                }

                if (!headers.get(index + (i - 1) * 2 + 1).name().equals(String.format("%s%d", nam2, i))) {
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

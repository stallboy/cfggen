package configgen.i18n;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.HasText;
import configgen.util.CachedFileOutputStream;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;
import configgen.value.ValueUtil;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.*;

import static configgen.value.CfgValue.*;


public final class GenI18nByPkAndFieldChain extends Generator {
    record OneText(String fieldChain,
                   String original,
                   String translated) {
    }

    /**
     * @param description：oneTable.nullableDescriptionFieldName != null 时有效
     */
    record OneRecord(String pk,
                     String description,
                     List<OneText> texts) {
    }

    /**
     * @param nullableDescriptionFieldName：如果table schema设置了lang，则最后输出文件里加上描述字段列，此列不翻译，仅用于参考
     */
    record OneTable(String table,
                    String nullableDescriptionFieldName,
                    List<OneRecord> records) {
    }

    private final String outputDir;
    private List<OneTable> textTables;
    private OneTable curTable;
    private OneRecord curRecord;
    private final I18nStat stat = new I18nStat();

    public GenI18nByPkAndFieldChain(Parameter parameter) {
        super(parameter);
        outputDir = parameter.get("dir", "../i18n/en");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);

        textTables = new ArrayList<>(16);
        for (VTable vTable : cfgValue.sortedTables()) {
            if (HasText.hasText(vTable.schema())) {
                String descriptionFieldName = vTable.schema().meta().getStr("lang", null);
                curTable = new OneTable(vTable.name(), descriptionFieldName, new ArrayList<>());
                curRecord = null;
                for (Map.Entry<Value, VStruct> e : vTable.primaryKeyMap().entrySet()) {
                    Value pk = e.getKey();
                    VStruct vStruct = e.getValue();
                    String description = descriptionFieldName != null ?
                            ValueUtil.extractFieldValueStr(vStruct, descriptionFieldName) : null;

                    ForeachValue.foreachValue(new TextValueVisitor(description), vStruct, pk, List.of());
                }

                textTables.add(curTable);
            }
        }

        for (Map.Entry<String, List<OneTable>> e : getTopModuleToTextTables().entrySet()) {
            String topModule = e.getKey() + ".xlsx";
            List<OneTable> tables = e.getValue();
            try (OutputStream os = new CachedFileOutputStream(Path.of(outputDir, topModule).toFile());
                 Workbook wb = new Workbook(os, "cfg", "1.0")) {
                for (OneTable ot : tables) {
                    Worksheet ws = wb.newWorksheet(ot.table);
                    new GenOneTable().gen(ot, ws, stat);
                }
            }
        }

        stat.dump();
    }

    private class TextValueVisitor extends ForeachValue.ValueVisitorForPrimitive {

        private final String description;

        public TextValueVisitor(String description) {
            this.description = description;
        }

        @Override
        public void visitPrimitive(PrimitiveValue primitiveValue, Value pk, List<String> fieldChain) {
            if (primitiveValue instanceof VText vText) {
                String original = vText.original().trim();
                String translated = vText.translated();
                if (original.isEmpty() && translated.isEmpty()) {
                    return;
                }

                String pkStr = pk.packStr();
                String fieldChainStr = TextFinderByPkAndFieldChain.fieldChainStr(fieldChain);
                OneText oneText = new OneText(fieldChainStr, original, translated);

                if (curRecord != null && curRecord.pk.equals(pkStr)) {
                    curRecord.texts.add(oneText);
                } else {
                    curRecord = new OneRecord(pkStr, description, new ArrayList<>());
                    curRecord.texts.add(oneText);
                    curTable.records.add(curRecord);
                }
            }
        }


    }

    private Map<String, List<OneTable>> getTopModuleToTextTables() {
        Map<String, List<OneTable>> res = new LinkedHashMap<>();
        for (OneTable ot : textTables) {
            int idx = ot.table.indexOf('.');
            String topModule = "_top";
            if (idx != -1) {
                topModule = ot.table.substring(0, idx);
            }

            List<OneTable> tables = res.computeIfAbsent(topModule, k -> new ArrayList<>());
            tables.add(ot);
        }
        return res;
    }


    static class GenOneTable {
        private final List<String> fields = new ArrayList<>();


        void gen(OneTable textTable, Worksheet ws, I18nStat stat) {
            int r = 1;
            boolean hasDescriptionColumn = textTable.nullableDescriptionFieldName != null;
            int offset = hasDescriptionColumn ? 2 : 1;

            for (OneRecord record : textTable.records) {
                for (OneText text : record.texts) {
                    stat.addOneTranslate(textTable.table, text.original, text.translated);
                    int idx = findFieldIndex(text);
                    int c = idx * 2 + offset;


                    ws.inlineString(r, c, text.original);
                    ws.inlineString(r, c + 1, text.translated);

                    if (text.translated.isEmpty()) {
                        stat.incNotTranslate(textTable.table);
                        ws.style(r, c + 1).fillColor("FF8800").set();
                    }
                }

                boolean hasValue = !record.texts.isEmpty();
                if (hasValue) {
                    ws.inlineString(r, 0, record.pk);
                    if (hasDescriptionColumn) {
                        ws.inlineString(r, 1, record.description);
                    }
                    r++;
                }
            }

            // 开始填第一行
            if (r > 1) {
                ws.inlineString(0, 0, "id");
                if (hasDescriptionColumn) {
                    ws.inlineString(0, 1, textTable.nullableDescriptionFieldName);
                }

                int idx = 0;
                for (String field : fields) {
                    int c = idx * 2 + offset;
                    ws.inlineString(0, c, field);
                    ws.inlineString(0, c + 1, "t(" + field + ")");
                    idx++;
                }
            }
        }

        int findFieldIndex(OneText text) {
            int i = 0;
            for (String field : fields) {
                if (text.fieldChain.equals(field)) {
                    return i;
                }
                i++;
            }

            fields.add(text.fieldChain);
            return i;
        }

    }

    static class I18nStat {
        record OneT(String table,
                    String translatedText) {
        }

        private int notTranslateCount = 0;
        private int sameOriginalCount = 0;
        private int textCount = 0;

        private final Map<String, OneT> accumulate = new HashMap<>();
        private final Map<String, List<OneT>> different = new HashMap<>();
        private final Set<String> hasNotTranslateTables = new HashSet<>();

        void addOneTranslate(String table, String orig, String translated) {
            OneT newT = new OneT(table, translated);
            OneT old = accumulate.putIfAbsent(orig, newT);
            textCount++;

            if (old != null) {
                sameOriginalCount++;
                if (!newT.translatedText.trim().equals(old.translatedText.trim())) {
                    List<OneT> diffs = different.get(orig);
                    if (diffs == null) {
                        diffs = new ArrayList<>();
                        diffs.add(old);
                        different.put(orig, diffs);
                    }
                    diffs.add(newT);
                }
            }
        }

        void incNotTranslate(String table) {
            notTranslateCount++;
            hasNotTranslateTables.add(table);
        }

        void dump() {
            System.out.printf("textCount         : %d%n", textCount);
            System.out.printf("notTranslateCount : %d%n", notTranslateCount);
            System.out.printf("sameOriginalCount : %d%n", sameOriginalCount);
            System.out.printf("differentTranslate: %d%n", different.size());

            System.out.println("------------ has not translate text: ------------ ");
            for (String table : hasNotTranslateTables) {
                System.out.println(table);
            }

            System.out.println("------------ different translate: ------------ ");
            for (Map.Entry<String, List<OneT>> e : different.entrySet()) {
                String orig = e.getKey();
                System.out.println(orig);
                for (OneT oneT : e.getValue()) {
                    System.out.printf("    %s \t (%s)%n", oneT.translatedText, oneT.table);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // 结论：没办法保证2次存储的文件相同
        try (OutputStream os = new FileOutputStream("test.xlsx");
                Workbook wb = new Workbook(os, "MyApplication", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet 1");
            ws.value(0, 0, "This is a string in A1");
            ws.value(0, 2, 1234);
            ws.value(0, 3, 123456L);
            ws.value(0, 4, 1.234);
        }
    }

}

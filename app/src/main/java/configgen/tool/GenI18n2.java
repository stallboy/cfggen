package configgen.tool;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.HasText;
import configgen.util.CachedFileOutputStream;
import configgen.value.CfgValue;
import configgen.value.ForeachPrimitiveValue;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.*;

import static configgen.value.CfgValue.*;

/**
 * 重要假设：每个表中的原始中文一样的话，英文也一样
 */
public final class GenI18n2 extends Generator {

    record OneText(String fieldChain,
                   String originalText,
                   String translatedText) {
    }

    record OneRecord(String pk,
                     List<OneText> texts) {
    }

    record OneTable(String table,
                    List<OneRecord> records) {
    }

    private final String outputDir;
    private List<OneTable> textTables;
    private OneTable curTable;
    private OneRecord curRecord;
    private final I18nStat stat = new I18nStat();

    public GenI18n2(Parameter parameter) {
        super(parameter);
        outputDir = parameter.get("dir", "../i18n/en");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);

        textTables = new ArrayList<>(16);
        for (VTable vTable : cfgValue.sortedTables()) {
            if (HasText.hasText(vTable.schema())) {
                curTable = new OneTable(vTable.name(), new ArrayList<>());
                curRecord = null;
                ForeachPrimitiveValue.foreachVTable(this::visit, vTable);
                textTables.add(curTable);
            }
        }

        for (Map.Entry<String, List<OneTable>> e : getTopModuleToTextTables().entrySet()) {
            String topModule = e.getKey() + ".xlsx";
            List<OneTable> tables = e.getValue();
            try (OutputStream os = new CachedFileOutputStream(Path.of(outputDir, topModule).toFile());
                 Workbook wb = new Workbook(os, "cfg", "1.0");) {
                for (OneTable ot : tables) {
                    Worksheet ws = wb.newWorksheet(ot.table);

                    new GenOneTable().gen(ot, ws, stat);

                }
            }
        }

        stat.dump();
    }

    private void visit(PrimitiveValue primitiveValue, String table, Value pk, List<String> fieldChain) {
        if (primitiveValue instanceof VText vText) {
            String original = vText.original().trim();
            String nullableI18n = vText.nullableI18n();
            if (original.isEmpty() && nullableI18n == null) {
                return;
            }

            String pkStr = pk.packStr();
            String translatedText = nullableI18n != null ? nullableI18n.trim() : "";
            String fieldChainStr = fieldChain.size() == 1 ? fieldChain.get(0) : String.join("-", fieldChain);
            OneText oneText = new OneText(fieldChainStr, original, translatedText);

            if (curRecord != null && curRecord.pk.equals(pkStr)) {
                curRecord.texts.add(oneText);
            } else {
                curRecord = new OneRecord(pkStr, new ArrayList<>());
                curRecord.texts.add(oneText);
                curTable.records.add(curRecord);
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
            Map<String, String> originalToTranslated = new LinkedHashMap<>();
            for (OneRecord record : textTable.records) {
                boolean hasValue = false;
                for (OneText text : record.texts) {
                    String old = originalToTranslated.putIfAbsent(text.originalText, text.translatedText);
                    if (old == null) {
                        int idx = findFieldIndex(text);
                        int c = idx * 2 + 1;

                        ws.inlineString(r, c, text.originalText);
                        ws.inlineString(r, c + 1, text.translatedText);

                        if (text.translatedText.isEmpty()) {
                            stat.incNotTranslate(textTable.table);
                            ws.style(r, c+1).fillColor("FF8800").set();
                        }
                        hasValue = true;
                    } //else: ignore 根据假设忽略
                }

                if (hasValue) {
                    ws.inlineString(r, 0, record.pk);
                    r++;
                }
            }

            // 开始填第一行
            if (r > 1) {
                ws.inlineString(0, 0, "id");
                int idx = 0;
                for (String field : fields) {
                    int c = idx * 2 + 1;
                    ws.inlineString(0, c, field);
                    ws.inlineString(0, c + 1, "t(" + field + ")");
                    idx++;
                }
            }

            stat.addOneTable(textTable.table, originalToTranslated);

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
        private int crossTableSameOriginalCount = 0;
        private final Map<String, OneT> accumulate = new HashMap<>();
        private final Map<String, List<OneT>> different = new HashMap<>();
        private final Set<String> hasNotTranslateTables = new HashSet<>();

        void incNotTranslate(String table) {
            notTranslateCount++;
            hasNotTranslateTables.add(table);
        }

        void addOneTable(String table, Map<String, String> originalToTranslated) {
            for (Map.Entry<String, String> e : originalToTranslated.entrySet()) {
                String orig = e.getKey();
                String translated = e.getValue();
                OneT newT = new OneT(table, translated);
                OneT old = accumulate.putIfAbsent(orig, newT);

                if (old != null) {
                    crossTableSameOriginalCount++;
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
        }

        void dump() {
            System.out.printf("notTranslateCount :           %d%n", notTranslateCount);
            System.out.printf("crossTableSameOriginalCount : %d%n", crossTableSameOriginalCount);
            System.out.printf("differentTranslate:           %d%n", different.size());

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

}

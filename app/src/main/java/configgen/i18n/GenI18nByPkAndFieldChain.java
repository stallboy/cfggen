package configgen.i18n;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.HasText;
import configgen.util.CachedFileOutputStream;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;
import configgen.value.ValueUtil;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static configgen.value.CfgValue.*;


public final class GenI18nByPkAndFieldChain extends Generator {
    record OneText(String fieldChain,
                   String original,
                   String translated) {
    }

    /**
     * @param description：oneTable.nullableDescriptionName != null 时有效
     */
    record OneRecord(String pk,
                     String description,
                     List<OneText> texts) {
    }

    /**
     * @param nullableDescriptionName：如果table schema设置了lang，则最后输出文件里加上描述字段列，此列不翻译，仅用于参考
     */
    record OneTable(String table,
                    String nullableDescriptionName,
                    List<OneRecord> records) {
    }

    private final String outputDir;
    private final String backupDir;
    private List<OneTable> textTables;
    private OneTable curTable;
    private OneRecord curRecord;
    private final I18nStat stat = new I18nStat();

    public GenI18nByPkAndFieldChain(Parameter parameter) {
        super(parameter);
        outputDir = parameter.get("dir", "../i18n/en");
        backupDir = parameter.get("backup", "../backup");
        if (tag != null) {
            throw new IllegalArgumentException("-gen i18nbyid should has no tag, tag=" + tag);
        }
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue();

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
                if (!curTable.records.isEmpty()){
                    textTables.add(curTable);
                    int txtCount = curTable.records.stream().mapToInt(r -> r.texts.size()).sum();
                    System.out.printf("%40s: %8d %8d%n", curTable.table,  curTable.records.size(), txtCount);
                }
            }
        }


        // 我们用的这个fastexcel lib同样内容写入xlsx，文件会不同，
        // xlsx里面的core.xml里有dcterms:created，是每次创建时时间，所以每次生成文件内容都不同
        // 而我们又希望内容不同时生成xlsx，才提交svn，所以需要如下逻辑，需要内容比较
        LangTextFinder needReplaceFileI18nById = null;
        String readI18nFilename = ctx.getContextCfg().i18nFilename();
        if (readI18nFilename != null && readI18nFilename.equals(outputDir)) {
            needReplaceFileI18nById = ctx.nullableLangTextFinder();
        }

        String lang = Path.of(outputDir).getFileName().toString();
        String outputDirTemp = Path.of(backupDir, lang+"_temp").normalize().toString();
        if (Files.isDirectory(Path.of(outputDirTemp))) {
            throw new RuntimeException("temp directory = %s exist, delete it then retry".formatted(outputDirTemp));
        }
        if (needReplaceFileI18nById != null) {
            if (!new File(outputDirTemp).mkdirs()) {
                throw new RuntimeException("make temp directory = %s failed".formatted(outputDirTemp));
            }
        }

        for (Map.Entry<String, List<OneTable>> e : getTopModuleToTextTables().entrySet()) {
            String topModuleFn = e.getKey() + ".xlsx";
            List<OneTable> tables = e.getValue();
            try (OutputStream os = needReplaceFileI18nById != null ?
                    new FileOutputStream(new File(outputDirTemp, topModuleFn)) :
                    new CachedFileOutputStream(new File(outputDir, topModuleFn));
                 Workbook wb = new Workbook(os, "cfg", "1.0")) {
                for (OneTable ot : tables) {
                    Worksheet ws = wb.newWorksheet(ot.table);
                    new GenOneTable().gen(ot, ws, stat);
                }
            }
        }

        if (needReplaceFileI18nById != null) { // 简化，只有需要replace时才比较
            // 1.先把 <outputDir> -> <outputDirBackup>
            String outputDirBackup = Path.of(backupDir, lang).normalize().toString();
            moveDirFilesToAnotherDir(outputDir, outputDirBackup);

            // 2.然后把<outputDirTemp> -> <outputDir>
            moveDirFilesToAnotherDir(outputDirTemp, outputDir);
            if (!new File(outputDirTemp).delete()) {
                throw new RuntimeException("delete temp directory = %s failed".formatted(outputDirTemp));
            }

            // 3.最后把内容相同的file 从 <outputDir>_backup ---拷贝到---> <outputDir>
            LangTextFinder curLang = TextFinderByPkAndFieldChain.loadOneLang(Path.of(outputDir));
            Map<String, OneTranslateFile> curFiles = getTopModulesToTextFinders(curLang);
            Map<String, OneTranslateFile> oldFiles = getTopModulesToTextFinders(needReplaceFileI18nById);

            for (Map.Entry<String, OneTranslateFile> oe : curFiles.entrySet()) {
                String topModule = oe.getKey();
                String fn = topModule + ".xlsx";
                OneTranslateFile cur = oe.getValue();
                OneTranslateFile old = oldFiles.get(topModule);

                Path dstPath = Path.of(outputDir, fn);
                if (cur.equals(old)) {
                    Files.copy(Path.of(outputDirBackup, fn), dstPath,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);


                } else {
                    Logger.log((old != null ? "modify " : "create ") + dstPath.toAbsolutePath().normalize());
                }
            }
        }

        stat.dump();
    }


    private static void moveDirFilesToAnotherDir(String from, String to) {
        File toDir = new File(to);
        if (toDir.isDirectory()) {
            for (File file : Objects.requireNonNull(toDir.listFiles())) {
                if (!file.delete()) {
                    throw new RuntimeException("delete " + file + " failed");
                }
            }
        } else {
            if (!toDir.mkdirs()) {
                throw new RuntimeException("mkdir " + toDir + " failed");
            }
        }
        for (File file : Objects.requireNonNull(new File(from).listFiles())) {
            if (!file.renameTo(new File(to, file.getName()))) {
                throw new RuntimeException("rename %s to %s failed".formatted(from, to));
            }
        }
    }

    record OneTranslateFile(Map<String, TextFinderByPkAndFieldChain> tables) {
    }

    private static Map<String, OneTranslateFile> getTopModulesToTextFinders(LangTextFinder lang) {
        Map<String, OneTranslateFile> res = new LinkedHashMap<>();
        for (Map.Entry<String, TextFinder> e : lang.getMap().entrySet()) {
            String table = e.getKey();
            TextFinderByPkAndFieldChain finder = (TextFinderByPkAndFieldChain) e.getValue();
            OneTranslateFile file = res.computeIfAbsent(getTopModule(table),
                    k -> new OneTranslateFile(new LinkedHashMap<>()));
            file.tables.put(table, finder);
        }
        return res;
    }

    private Map<String, List<OneTable>> getTopModuleToTextTables() {
        Map<String, List<OneTable>> res = new LinkedHashMap<>();
        for (OneTable ot : textTables) {
            List<OneTable> tables = res.computeIfAbsent(getTopModule(ot.table), k -> new ArrayList<>());
            tables.add(ot);
        }
        return res;
    }

    private static String getTopModule(String table) {
        int idx = table.indexOf('.');
        if (idx != -1) {
            return table.substring(0, idx);
        }
        return "_top";
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

    static class GenOneTable {
        private final List<String> fields = new ArrayList<>();


        void gen(OneTable textTable, Worksheet ws, I18nStat stat) {
            int r = 1;
            boolean hasDescriptionColumn = textTable.nullableDescriptionName != null;
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
                    ws.inlineString(0, 1, textTable.nullableDescriptionName);
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

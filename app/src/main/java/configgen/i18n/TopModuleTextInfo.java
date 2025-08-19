package configgen.i18n;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import static configgen.i18n.TextFinderById.*;

/**
 * key：table name
 */
class TopModuleTextInfo extends LinkedHashMap<String, TextFinderById> {

    public void save(OutputStream os, I18nStat stat) throws IOException {
        try (Workbook wb = new Workbook(os, "cfg", "1.0")) {
            for (var e : entrySet()) {
                String table = e.getKey();
                TextFinderById tf = e.getValue();
                Worksheet ws = wb.newWorksheet(table);
                saveSheet(tf, ws, table, stat);
            }
        }
    }

    void saveSheet(TextFinderById finder, Worksheet ws, String table, I18nStat stat) {
        boolean hasDescriptionColumn = finder.nullableDescriptionName != null;
        int offset = hasDescriptionColumn ? 2 : 1;

        { // 写第一行，header
            ws.inlineString(0, 0, "id");
            if (hasDescriptionColumn) {
                ws.inlineString(0, 1, finder.nullableDescriptionName);
            }
            int idx = 0;
            for (String field : finder.fieldChainToIndex.keySet()) {
                int c = idx * 2 + offset;
                ws.inlineString(0, c, field);
                ws.inlineString(0, c + 1, "t(" + field + ")");
                idx++;
            }
        }

        int r = 1;
        for (var e : finder.pkToTexts.entrySet()) {
            String pk = e.getKey();
            OneRecord record = e.getValue();

            ws.inlineString(r, 0, pk);
            if (hasDescriptionColumn) {
                ws.inlineString(r, 1, record.description());
            }

            int idx = 0;
            for (OneText text : record.texts()) {
                if (text != null) {
                    stat.addOneTranslate(table, text.original(), text.translated());
                    int c = idx * 2 + offset;

                    ws.inlineString(r, c, text.original());
                    ws.inlineString(r, c + 1, text.translated());

                    if (text.translated().isEmpty()) {
                        stat.incNotTranslate(table);
                        ws.style(r, c + 1).fillColor("FF8800").set();
                    }
                }
                idx++;
            }
            r++;
        }
    }

}

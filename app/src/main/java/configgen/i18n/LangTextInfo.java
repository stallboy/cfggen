package configgen.i18n;

import configgen.util.CSVUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static configgen.i18n.TextFinderById.*;

/**
 * key：top module name
 */
public class LangTextInfo extends LinkedHashMap<String, TopModuleTextInfo> {

    public static LangTextInfo of(Map<String, TextFinder> tableMap) {
        LangTextInfo res = new LangTextInfo();
        for (Map.Entry<String, TextFinder> e : tableMap.entrySet()) {
            String table = e.getKey();
            TextFinderById finder = (TextFinderById) e.getValue();
            TopModuleTextInfo file = res.computeIfAbsent(getTopModule(table),
                    k -> new TopModuleTextInfo());
            file.put(table, finder);
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

    public void save(File dstFile) throws IOException {
        List<List<String>> zzz = new ArrayList<>(64 * 1024);
        for (var e : entrySet()) {
            for (var t : e.getValue().entrySet()) {
                String table = t.getKey();
                TextFinderById finder = t.getValue();
                List<String> fieldChainList = finder.fieldChainToIndex.keySet().stream().toList();
                for (var r : finder.pkToTexts.entrySet()) {
                    String pk = r.getKey();
                    OneRecord record = r.getValue();
                    int idx = 0;
                    for (OneText ot : record.texts()) {
                        if (ot != null) {
                            zzz.add(List.of(table, pk, fieldChainList.get(idx), ot.original(), ot.translated()));
                        }
                        idx++;
                    }
                }
            }

        }
        CSVUtil.writeToFile(dstFile, zzz);
    }

}

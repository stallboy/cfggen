package configgen.i18n;

import configgen.util.Logger;

import java.util.*;

class I18nStat {
    private record OneT(String table,
                String translatedText) {
    }

    private int notTranslateCount = 0;
    private int sameOriginalCount = 0;
    private int textCount = 0;

    private final Map<String, OneT> accumulate = new HashMap<>();
    private final Map<String, List<OneT>> different = new HashMap<>();
    private final Set<String> hasNotTranslateTables = new HashSet<>();

    public void addOneTranslate(String table, String orig, String translated) {
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

    public void incNotTranslate(String table) {
        notTranslateCount++;
        hasNotTranslateTables.add(table);
    }

    public void dump() {
        Logger.verbose("              textCount : %d", textCount);
        Logger.verbose("      notTranslateCount : %d", notTranslateCount);
        Logger.verbose("      sameOriginalCount : %d", sameOriginalCount);
        Logger.verbose("differentTranslateCount : %d", different.size());

        Logger.verbose("------------ has not translate text: ------------ ");
        for (String table : hasNotTranslateTables) {
            Logger.verbose(table);
        }

        Logger.verbose("------------ different translate: ------------ ");
        for (Map.Entry<String, List<OneT>> e : different.entrySet()) {
            String orig = e.getKey();
            Logger.verbose(orig);
            for (OneT oneT : e.getValue()) {
                Logger.verbose("    %s \t (%s)", oneT.translatedText, oneT.table);
            }
        }
    }
}

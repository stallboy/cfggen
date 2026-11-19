package configgen.i18n;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TableTranslationLookup {
    private final TranslationBundle translationBundle;
    private final List<TranslationCatalog.@Nullable TextFinder> tableTextFinders;
    private final String[] texts;
    private final String[] emptyTexts;

    public TableTranslationLookup(TranslationBundle translationBundle) {
        this.translationBundle = translationBundle;
        tableTextFinders = new ArrayList<>(translationBundle.langMap().size());
        int languageCount = translationBundle.languageCount();
        texts = new String[languageCount];

        emptyTexts = new String[languageCount];
        for (int i = 0; i < languageCount; i++) {
            emptyTexts[i] = "";
        }
    }

    public void selectTable(String table) {
        tableTextFinders.clear();
        for (TranslationCatalog catalog : translationBundle.langMap().values()) {
            tableTextFinders.add(catalog.getTextFinder(table));
        }
    }

    public String[] resolveTextsForAllLanguages(String pk, List<String> fieldChain, String original) {
        if (original.isEmpty()) {
            return emptyTexts;
        }

        texts[0] = original;
        int i = 1;

        for (TranslationCatalog.TextFinder finder : tableTextFinders) {
            String translated = null;
            if (finder != null) {
                translated = finder.findText(pk, fieldChain, original);
            }
            if (translated == null) {
                translated = original;
            }
            texts[i] = translated;
            i++;
        }
        return texts;
    }
}

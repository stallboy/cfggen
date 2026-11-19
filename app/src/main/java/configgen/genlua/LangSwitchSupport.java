package configgen.genlua;

import org.jspecify.annotations.Nullable;

import configgen.i18n.TranslationBundle;
import configgen.i18n.TranslationCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class LangSwitchSupport {
    private final TranslationBundle translationBundle;
    private final List<String> defaultLangTexts;
    private final List<LangTexts> langTextsList;
    private int index = 0;

    private static class LangTexts {
        final String lang;
        final TranslationCatalog langI18n;
        final List<String> texts;
        TranslationCatalog.@Nullable TextFinder curTableTextFinder;

        LangTexts(String lang, TranslationCatalog langI18n, List<String> texts) {
            this.lang = lang;
            this.langI18n = langI18n;
            this.texts = texts;
        }
    }

    private static final int INIT_SIZE = 1024 * 32;

    LangSwitchSupport(TranslationBundle translationBundle) {
        this.translationBundle = translationBundle;
        defaultLangTexts = new ArrayList<>(INIT_SIZE);
        defaultLangTexts.add("");  // 第一个是id为0，表示空字符串

        langTextsList = new ArrayList<>(translationBundle.langMap().size());
        for (Map.Entry<String, TranslationCatalog> e : translationBundle.langMap().entrySet()) {
            List<String> texts = new ArrayList<>(INIT_SIZE);
            texts.add("");
            langTextsList.add(new LangTexts(e.getKey(), e.getValue(), texts));
        }
    }

    void enterTable(String table) {
        for (LangTexts lt : langTextsList) {
            lt.curTableTextFinder = lt.langI18n.getTextFinder(table);
        }
    }

    int enterText(String pkStr, List<String> fieldChain, String original) {
        if (original.isEmpty()) {
            return 0;  // 空字符串
        }

        defaultLangTexts.add(original);
        for (LangTexts lt : langTextsList) {
            String text = null;
            if (lt.curTableTextFinder != null) {
                text = lt.curTableTextFinder.findText(pkStr, fieldChain, original);
            }
            if (text == null) {
                text = original;
            }
            lt.texts.add(text);
        }
        index++;
        return index;
    }

    Map<String, List<String>> getLang2Texts() {
        Map<String, List<String>> lang2Texts = new LinkedHashMap<>();
        lang2Texts.put(translationBundle.defaultLang(), defaultLangTexts);
        for (LangTexts lt : langTextsList) {
            lang2Texts.put(lt.lang, lt.texts);
        }
        return lang2Texts;
    }
}

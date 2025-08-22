package configgen.genlua;

import configgen.i18n.LangSwitchable;
import configgen.i18n.LangTextFinder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class LangSwitchSupport {
    private final LangSwitchable langSwitch;
    private final List<String> defaultLangTexts;
    private final List<LangTexts> langTextsList;
    private int index = 0;

    private static class LangTexts {
        String lang;
        LangTextFinder langI18n;
        LangTextFinder.TextFinder curTableTextFinder;

        List<String> texts;
    }

    private static final int INIT_SIZE = 1024 * 32;

    LangSwitchSupport(LangSwitchable langSwitch) {
        this.langSwitch = langSwitch;
        defaultLangTexts = new ArrayList<>(INIT_SIZE);
        defaultLangTexts.add("");  // 第一个是id为0，表示空字符串

        langTextsList = new ArrayList<>(langSwitch.langMap().size());
        for (Map.Entry<String, LangTextFinder> e : langSwitch.langMap().entrySet()) {
            List<String> texts = new ArrayList<>(INIT_SIZE);
            texts.add("");
            LangTexts lt = new LangTexts();
            lt.lang = e.getKey();
            lt.langI18n = e.getValue();
            lt.texts = texts;
            langTextsList.add(lt);
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
        lang2Texts.put(langSwitch.defaultLang(), defaultLangTexts);
        for (LangTexts lt : langTextsList) {
            lang2Texts.put(lt.lang, lt.texts);
        }
        return lang2Texts;
    }
}

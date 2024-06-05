package configgen.ctx;

import java.util.ArrayList;
import java.util.List;

public class LangSwitchRuntime {
    private final LangSwitch langSwitch;
    private final List<TextI18n.TableI18n> curTableI18nList;
    private final String[] tmp;
    private final String[] tmpEmpty;

    public LangSwitchRuntime(LangSwitch langSwitch) {
        this.langSwitch = langSwitch;
        curTableI18nList = new ArrayList<>(langSwitch.lang2i18n().size());
        int langCnt = langSwitch.languageCount();
        tmp = new String[langCnt];

        tmpEmpty = new String[langCnt];
        for (int i = 0; i < langCnt; i++) {
            tmpEmpty[i] = "";
        }
    }

    public void enterTable(String table) {
        curTableI18nList.clear();
        for (TextI18n i18n : langSwitch.lang2i18n().values()) {
            curTableI18nList.add(i18n.getTableI18n(table));
        }
    }

    public String[] findAllLangText(String original) {
        if (original.isEmpty()) {
            return tmpEmpty;
        }

        tmp[0] = original;
        int i = 1;
        for (TextI18n.TableI18n i18n : curTableI18nList) {
            String t = null;
            if (i18n != null) {
                t = i18n.findText(original);
            }
            if (t == null) {
                t = original;
            }
            tmp[i] = t;
            i++;
        }
        return tmp;
    }
}

package configgen.i18n;

import java.util.ArrayList;
import java.util.List;

public class LangSwitchableRuntime {
    private final LangSwitchable langSwitch;
    private final List<TextFinder> curTableTextFinderList;
    private final String[] tmp;
    private final String[] tmpEmpty;

    public LangSwitchableRuntime(LangSwitchable langSwitch) {
        this.langSwitch = langSwitch;
        curTableTextFinderList = new ArrayList<>(langSwitch.langMap().size());
        int langCnt = langSwitch.languageCount();
        tmp = new String[langCnt];

        tmpEmpty = new String[langCnt];
        for (int i = 0; i < langCnt; i++) {
            tmpEmpty[i] = "";
        }
    }

    public void enterTable(String table) {
        curTableTextFinderList.clear();
        for (LangTextFinder i18n : langSwitch.langMap().values()) {
            curTableTextFinderList.add(i18n.getTableTextFinder(table));
        }
    }

    public String[] findAllLangText(String pk, List<String> fieldChain, String original) {
        if (original.isEmpty()) {
            return tmpEmpty;
        }

        tmp[0] = original;
        int i = 1;

        for (TextFinder finder : curTableTextFinderList) {
            String t = null;
            if (finder != null) {
                t = finder.findText(pk, fieldChain, original);
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

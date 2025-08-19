package configgen.i18n;

import java.util.*;


/**
 * 多语言
 * @param langMap 在实现多种语言可切换时需要所有语言的信息
 * @param defaultLang 默认语言
 */
public record LangSwitchable(Map<String, LangTextFinder> langMap,
                             String defaultLang) {

    public List<String> languages() {
        List<String> res = new ArrayList<>(langMap.size() + 1);
        res.add(defaultLang);
        res.addAll(langMap.keySet());
        return res;
    }

    public int languageCount() {
        return langMap.size() + 1;
    }

}

package configgen.i18n;

import java.util.*;

/**
 * 一种语言,包含多个table的翻译信息
 */
public class LangTextFinder extends TreeMap<String, TextFinder>{


    public TextFinder getTextFinder(String table) {
        return get(table);
    }

}


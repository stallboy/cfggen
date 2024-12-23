package configgen.ctx;

import java.util.*;

/**
 * 一种语言
 */
public class LangTextFinder {

    /**
     * 包含多个table的翻译信息
     */
    private final Map<String, TextFinder> tableMap = new TreeMap<>();

    public Map<String, TextFinder> getTableTextFinderMap() {
        return tableMap;
    }

    public TextFinder getTableTextFinder(String table) {
        return tableMap.get(table);
    }

}


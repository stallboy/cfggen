package config.task;

// 任务完成条件类型
// 来自：task/complete_condition_type任务完成条件类型.csv
public enum CfgComplete_condition_type {
    KILLMONSTER("KillMonster", 1),
    TALKNPC("TalkNpc", 2),
    COLLECTITEM("CollectItem", 3),
    CONDITIONAND("ConditionAnd", 4),
    CHAT("Chat", 5),
    TESTNOCOLUMN("TestNoColumn", 6),
    AA("aa", 7);

    private final String name;
    private final int value;


    CfgComplete_condition_type(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static final java.util.Map<Integer, CfgComplete_condition_type> map;

    static {
        var symbols = new java.util.LinkedHashMap<Integer, CfgComplete_condition_type>();
        for(CfgComplete_condition_type e : CfgComplete_condition_type.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static CfgComplete_condition_type get(int value) {
        return map.get(value);
    }

    /**
     * 任务完成条件类型（id的范围为1-100）
     */
    public int getId() {
        return value;
    }

    /**
     * 程序用名字
     */
    public String getName() {
        return name;
    }

}

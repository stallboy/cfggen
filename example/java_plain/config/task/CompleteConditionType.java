package config.task;

// 任务完成条件类型
// 来自：task/complete_condition_type任务完成条件类型.csv
public enum CompleteConditionType {
    KILL_MONSTER("KillMonster", 1),
    TALK_NPC("TalkNpc", 2),
    COLLECT_ITEM("CollectItem", 3),
    CONDITION_AND("ConditionAnd", 4),
    CHAT("Chat", 5),
    TEST_NO_COLUMN("TestNoColumn", 6),
    AA("aa", 7);

    private final String name;
    private final int value;


    CompleteConditionType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static final java.util.Map<Integer, CompleteConditionType> map;

    static {
        var symbols = new java.util.LinkedHashMap<Integer, CompleteConditionType>();
        for(CompleteConditionType e : CompleteConditionType.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static CompleteConditionType get(int value) {
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

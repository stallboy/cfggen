package config.task.completecondition;

public sealed interface CfgCompletecondition permits CfgKillMonster, CfgTalkNpc, CfgTestNoColumn, CfgChat, CfgConditionAnd, CfgCollectItem, CfgAa {
    config.task.CfgCompleteconditiontype type();

    default void _resolve(config.ConfigMgr mgr) {
    }

    static CfgCompletecondition _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        switch (tag) {
            case "KillMonster":
                return config.task.completecondition.CfgKillMonster._create(input);
            case "TalkNpc":
                return config.task.completecondition.CfgTalkNpc._create(input);
            case "TestNoColumn":
                return config.task.completecondition.CfgTestNoColumn._create(input);
            case "Chat":
                return config.task.completecondition.CfgChat._create(input);
            case "ConditionAnd":
                return config.task.completecondition.CfgConditionAnd._create(input);
            case "CollectItem":
                return config.task.completecondition.CfgCollectItem._create(input);
            case "aa":
                return config.task.completecondition.CfgAa._create(input);
        }
        throw new IllegalArgumentException(tag + " not found");
    }
}

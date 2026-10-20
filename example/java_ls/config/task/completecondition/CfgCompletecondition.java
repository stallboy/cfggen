package config.task.completecondition;

import configgen.genjava.LoadValueErrs;

public sealed interface CfgCompletecondition permits CfgKillMonster, CfgTalkNpc, CfgTestNoColumn, CfgChat, CfgConditionAnd, CfgCollectItem, CfgAa {
    config.task.CfgCompleteconditiontype type();

    default void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
    }

    static CfgCompletecondition _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        return switch (tag) {
            case "KillMonster" -> config.task.completecondition.CfgKillMonster._create(input);
            case "TalkNpc" -> config.task.completecondition.CfgTalkNpc._create(input);
            case "TestNoColumn" -> config.task.completecondition.CfgTestNoColumn._create(input);
            case "Chat" -> config.task.completecondition.CfgChat._create(input);
            case "ConditionAnd" -> config.task.completecondition.CfgConditionAnd._create(input);
            case "CollectItem" -> config.task.completecondition.CfgCollectItem._create(input);
            case "aa" -> config.task.completecondition.CfgAa._create(input);
            default -> throw input.errors().unknownVariant("CfgCompletecondition", tag);
        };
    }
}

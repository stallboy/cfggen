package config.task.completecondition;

import configgen.genjava.LoadValueErrs;

public sealed interface Completecondition permits KillMonster, TalkNpc, TestNoColumn, Chat, ConditionAnd, CollectItem, Aa {
    config.task.Completeconditiontype type();

    default void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
    }

    static Completecondition _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        return switch (tag) {
            case "KillMonster" -> config.task.completecondition.KillMonster._create(input);
            case "TalkNpc" -> config.task.completecondition.TalkNpc._create(input);
            case "TestNoColumn" -> config.task.completecondition.TestNoColumn._create(input);
            case "Chat" -> config.task.completecondition.Chat._create(input);
            case "ConditionAnd" -> config.task.completecondition.ConditionAnd._create(input);
            case "CollectItem" -> config.task.completecondition.CollectItem._create(input);
            case "aa" -> config.task.completecondition.Aa._create(input);
            default -> throw input.errors().unknownVariant("Completecondition", tag);
        };
    }
}

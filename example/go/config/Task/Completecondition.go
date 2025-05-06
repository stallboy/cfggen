package config.Task
import (
    "config"
)
type Completecondition interface
{
    type() config.Task.Completeconditiontype

    internal virtual void _resolve(Config.LoadErrors errors)
    {
    }

    internal static Completecondition _create(Config.Stream os) {
        switch(os.ReadString()) {
            case "KillMonster":
                return config.Task.Completecondition.KillMonster._create(os);
            case "TalkNpc":
                return config.Task.Completecondition.TalkNpc._create(os);
            case "TestNoColumn":
                return config.Task.Completecondition.TestNoColumn._create(os);
            case "Chat":
                return config.Task.Completecondition.Chat._create(os);
            case "ConditionAnd":
                return config.Task.Completecondition.ConditionAnd._create(os);
            case "CollectItem":
                return config.Task.Completecondition.CollectItem._create(os);
        }
        return null;
    }
}

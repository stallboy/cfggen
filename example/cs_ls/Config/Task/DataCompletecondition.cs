namespace Config.Task;

public abstract class DataCompletecondition
{
    public abstract Task.DataCompleteconditiontype type();

    internal virtual void _resolve(LoadErrors errors)
    {
    }
    internal static DataCompletecondition _create(Stream os)
    {
        var impl = os.ReadStringInPool();
        switch(impl)
        {
            case "KillMonster":
                return Task.Completecondition.DataKillMonster._create(os);
            case "TalkNpc":
                return Task.Completecondition.DataTalkNpc._create(os);
            case "TestNoColumn":
                return Task.Completecondition.DataTestNoColumn._create(os);
            case "Chat":
                return Task.Completecondition.DataChat._create(os);
            case "ConditionAnd":
                return Task.Completecondition.DataConditionAnd._create(os);
            case "CollectItem":
                return Task.Completecondition.DataCollectItem._create(os);
            case "aa":
                return Task.Completecondition.DataAa._create(os);
        }
        throw os.NotFoundImpl(impl, "task.completecondition");
    }
}


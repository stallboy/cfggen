namespace Config.Ai;

public abstract class DataTriggerTick
{

    internal static DataTriggerTick _create(Stream os)
    {
        var impl = os.ReadStringInPool();
        switch(impl)
        {
            case "ConstValue":
                return Ai.TriggerTick.DataConstValue._create(os);
            case "ByLevel":
                return Ai.TriggerTick.DataByLevel._create(os);
            case "ByServerUpDay":
                return Ai.TriggerTick.DataByServerUpDay._create(os);
        }
        throw os.NotFoundImpl(impl, "ai.TriggerTick");
    }
}


namespace Config.Task.Completecondition;

public partial class DataChat : Task.DataCompletecondition
{
    public override Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.Chat;
    }

    public required string Msg { get; init; }

    public override int GetHashCode()
    {
        return Msg.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataChat;
        return o != null && Msg.Equals(o.Msg);
    }

    public override string ToString()
    {
        return "(" + Msg + ")";
    }

    internal new static DataChat _create(Stream os)
    {
        var msg = os.ReadStringInPool();
        return new DataChat {
            Msg = msg,
        };
    }

}

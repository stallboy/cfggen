namespace Config.Ai.TriggerTick;

public partial class DataConstValue : Ai.DataTriggerTick
{
    public required int Value { get; init; }

    public override int GetHashCode()
    {
        return Value.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataConstValue;
        return o != null && Value.Equals(o.Value);
    }

    public override string ToString()
    {
        return "(" + Value + ")";
    }

    internal new static DataConstValue _create(Stream os)
    {
        var value = os.ReadInt32();
        return new DataConstValue {
            Value = value,
        };
    }

}

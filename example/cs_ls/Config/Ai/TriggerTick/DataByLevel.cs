namespace Config.Ai.TriggerTick;

public partial class DataByLevel : Ai.DataTriggerTick
{
    public required int Init { get; init; }
    public required float Coefficient { get; init; }

    public override int GetHashCode()
    {
        return Init.GetHashCode() + Coefficient.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataByLevel;
        return o != null && Init.Equals(o.Init) && Coefficient.Equals(o.Coefficient);
    }

    public override string ToString()
    {
        return "(" + Init + "," + Coefficient + ")";
    }

    internal new static DataByLevel _create(Stream os)
    {
        var init = os.ReadInt32();
        var coefficient = os.ReadSingle();
        return new DataByLevel {
            Init = init,
            Coefficient = coefficient,
        };
    }

}

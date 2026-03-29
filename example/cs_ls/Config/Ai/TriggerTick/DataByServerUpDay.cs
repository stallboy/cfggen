namespace Config.Ai.TriggerTick;

public partial class DataByServerUpDay : Ai.DataTriggerTick
{
    public required int Init { get; init; }
    public required float Coefficient1 { get; init; }
    public required float Coefficient2 { get; init; }

    public override int GetHashCode()
    {
        return Init.GetHashCode() + Coefficient1.GetHashCode() + Coefficient2.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataByServerUpDay;
        return o != null && Init.Equals(o.Init) && Coefficient1.Equals(o.Coefficient1) && Coefficient2.Equals(o.Coefficient2);
    }

    public override string ToString()
    {
        return "(" + Init + "," + Coefficient1 + "," + Coefficient2 + ")";
    }

    internal new static DataByServerUpDay _create(Stream os)
    {
        var init = os.ReadInt32();
        var coefficient1 = os.ReadSingle();
        var coefficient2 = os.ReadSingle();
        return new DataByServerUpDay {
            Init = init,
            Coefficient1 = coefficient1,
            Coefficient2 = coefficient2,
        };
    }

}

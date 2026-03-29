namespace Config.Equip;

public partial class DataTestPackBean
{
    public required string Name { get; init; }
    public required DataRange IRange { get; init; }

    public override int GetHashCode()
    {
        return Name.GetHashCode() + IRange.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataTestPackBean;
        return o != null && Name.Equals(o.Name) && IRange.Equals(o.IRange);
    }

    public override string ToString()
    {
        return "(" + Name + "," + IRange + ")";
    }

    internal static DataTestPackBean _create(Stream os)
    {
        var name = os.ReadStringInPool();
        var iRange = DataRange._create(os);
        return new DataTestPackBean {
            Name = name,
            IRange = iRange,
        };
    }

}

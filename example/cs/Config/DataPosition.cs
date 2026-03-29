namespace Config;

public partial class DataPosition
{
    public required int X { get; init; }
    public required int Y { get; init; }
    public required int Z { get; init; }

    public override int GetHashCode()
    {
        return X.GetHashCode() + Y.GetHashCode() + Z.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataPosition;
        return o != null && X.Equals(o.X) && Y.Equals(o.Y) && Z.Equals(o.Z);
    }

    public override string ToString()
    {
        return "(" + X + "," + Y + "," + Z + ")";
    }

    internal static DataPosition _create(Stream os)
    {
        var x = os.ReadInt32();
        var y = os.ReadInt32();
        var z = os.ReadInt32();
        return new DataPosition {
            X = x,
            Y = y,
            Z = z,
        };
    }

}

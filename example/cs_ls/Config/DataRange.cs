namespace Config;

public partial class DataRange
{
    public required int Min { get; init; } /* 最小 */
    public required int Max { get; init; } /* 最大 */

    public override int GetHashCode()
    {
        return Min.GetHashCode() + Max.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataRange;
        return o != null && Min.Equals(o.Min) && Max.Equals(o.Max);
    }

    public override string ToString()
    {
        return "(" + Min + "," + Max + ")";
    }

    internal static DataRange _create(Stream os)
    {
        var min = os.ReadInt32();
        var max = os.ReadInt32();
        return new DataRange {
            Min = min,
            Max = max,
        };
    }

}

namespace Config.Other;

public enum DataArgCaptureMode
{
    Snapshot,
    Dynamic,
}

public partial class DataArgCaptureModeInfo
{
    public required string Name { get; init; }
    public required string Comment { get; init; }
    public required DataArgCaptureMode eEnum { get; init; }
    
    private static OrderedDictionary<string, DataArgCaptureModeInfo> _all = [];

    public static DataArgCaptureModeInfo? Get(string name)
    {
        return _all.GetValueOrDefault(name);
    }

    public static IReadOnlyList<DataArgCaptureModeInfo> All()
    {
        return _all.Values;
    }
}

public static class DataArgCaptureModeExtensions
{
    internal static readonly DataArgCaptureModeInfo[] _infos = new DataArgCaptureModeInfo[2];

    public static DataArgCaptureModeInfo Info(this DataArgCaptureMode e)
    {
        return _infos[(int)e];
    }
}

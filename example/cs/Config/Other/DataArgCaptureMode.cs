namespace Config.Other;

public partial class DataArgCaptureMode
{
    public static DataArgCaptureMode Snapshot { get; private set; } = null!;
    public static DataArgCaptureMode Dynamic { get; private set; } = null!;

    public required string Name { get; init; }
    public required string Comment { get; init; }
    
    private static OrderedDictionary<string, DataArgCaptureMode> _all = [];

    public static DataArgCaptureMode? Get(string name)
    {
        return _all.GetValueOrDefault(name);
    }

    public static IReadOnlyList<DataArgCaptureMode> All()
    {
        return _all.Values;
    }
}

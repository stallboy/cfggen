namespace Config.Other;

public enum DArgCaptureMode
{
    Snapshot,
    Dynamic,
}

public partial class DArgCaptureModeInfo
{
    public required string Name { get; init; }
    public required int Id { get; init; }
    public required string Comment { get; init; }
    public required DArgCaptureMode eEnum { get; init; }
    
    private static System.Collections.Frozen.FrozenDictionary<string, DArgCaptureModeInfo> _all = null!;

    public static DArgCaptureModeInfo? Get(string name)
    {
        return _all.GetValueOrDefault(name);
    }

    
    private static System.Collections.Frozen.FrozenDictionary<int, DArgCaptureModeInfo> _idMap = null!;

    public static DArgCaptureModeInfo? GetById(int id)
    {
        return _idMap.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DArgCaptureModeInfo> All()
    {
        return _all.Values;
    }
}

public static class DArgCaptureModeExtensions
{
    internal static readonly DArgCaptureModeInfo[] _infos = new DArgCaptureModeInfo[2];

    public static DArgCaptureModeInfo Info(this DArgCaptureMode e)
    {
        return _infos[(int)e];
    }
}

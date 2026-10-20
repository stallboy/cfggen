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
    public required DArgCaptureMode EEnum { get; init; }

    public static DArgCaptureModeInfo? Get(string name)
        => Get(ConfigMgr.GetMgr(), name);

    public static DArgCaptureModeInfo? Get(ConfigMgr config, string name)
    {
        var index = config._other_dArgCaptureMode_All;
        return index.TryGetValue(name, out var result) ? result : null;
    }


    public static DArgCaptureModeInfo? GetById(int id)
        => GetById(ConfigMgr.GetMgr(), id);

    public static DArgCaptureModeInfo? GetById(ConfigMgr config, int id)
    {
        var index = config._other_dArgCaptureMode_IdMap;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DArgCaptureModeInfo> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DArgCaptureModeInfo> All(ConfigMgr config)
        => config._other_dArgCaptureMode_Rows;
}

public static class DArgCaptureModeExtensions
{
    public static DArgCaptureModeInfo Info(this DArgCaptureMode e)
        => e.Info(ConfigMgr.GetMgr());

    public static DArgCaptureModeInfo Info(this DArgCaptureMode e, ConfigMgr config)
        => config._other_dArgCaptureMode_Symbols[(int)e]!;
}

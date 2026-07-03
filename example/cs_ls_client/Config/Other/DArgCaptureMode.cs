using System;
using System.Collections.Generic;
namespace Config.Other
{

public enum DArgCaptureMode
{
    Snapshot,
    Dynamic,
}

public partial class DArgCaptureModeInfo
{
    public string Name { get; init; } = null!;
    public int Id { get; init; }
    public Config.Text Comment { get; init; } = null!;
    public DArgCaptureMode EEnum { get; init; }
    private static IReadOnlyList<DArgCaptureModeInfo> _allList = null!;
    
    private static Dictionary<string, DArgCaptureModeInfo> _all = null!;

    public static DArgCaptureModeInfo? Get(string name)
    {
        return _all.GetValueOrDefault(name);
    }

    
    private static Dictionary<int, DArgCaptureModeInfo> _idMap = null!;

    public static DArgCaptureModeInfo? GetById(int id)
    {
        return _idMap.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DArgCaptureModeInfo> All()
    {
        return _allList;
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
}

using System.Collections.Generic;
namespace Config.Other;

public partial class DataArgCaptureMode
{
    public static DataArgCaptureMode Snapshot { get; private set; } = null!;
    public static DataArgCaptureMode Dynamic { get; private set; } = null!;
    public required string Name { get; init; }
    public required string Comment { get; init; }

    public override int GetHashCode()
    {
        return Name.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataArgCaptureMode;
        return o != null && Name.Equals(o.Name);
    }

    public override string ToString()
    {
        return "(" + Name + "," + Comment + ")";
    }

    
    private static OrderedDictionary<string, DataArgCaptureMode> _all = [];

    public static DataArgCaptureMode? Get(string name)
    {
        return _all.GetValueOrDefault(name);
    }

    public static IReadOnlyList<DataArgCaptureMode> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Name, self);
            if (self.Name.Trim().Length == 0)
                continue;
            switch(self.Name.Trim())
            {
                case "Snapshot":
                    if (Snapshot != null)
                        errors.EnumDup("other.ArgCaptureMode", self.ToString());
                    Snapshot = self;
                    break;
                case "Dynamic":
                    if (Dynamic != null)
                        errors.EnumDup("other.ArgCaptureMode", self.ToString());
                    Dynamic = self;
                    break;
                default:
                    errors.EnumDataAdd("other.ArgCaptureMode", self.ToString());
                    break;
            }
        }

        if (Snapshot == null)
            errors.EnumNull("other.ArgCaptureMode", "Snapshot");
        if (Dynamic == null)
            errors.EnumNull("other.ArgCaptureMode", "Dynamic");
    }

    internal static DataArgCaptureMode _create(Stream os)
    {
        var name = os.ReadStringInPool();
        var comment = os.ReadStringInPool();
        return new DataArgCaptureMode {
            Name = name,
            Comment = comment,
        };
    }

}

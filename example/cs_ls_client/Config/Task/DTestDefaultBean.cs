using System.Collections.Generic;
namespace Config.Task
{

public partial class DTestDefaultBean
{
    public int TestInt { get; init; }
    public bool TestBool { get; init; }
    public string TestString { get; init; } = null!;
    public DPosition TestSubBean { get; init; } = null!;
    public IReadOnlyList<int> TestList { get; init; } = null!;
    public IReadOnlyList<int> TestList2 { get; init; } = null!;
    public IReadOnlyDictionary<int, string> TestMap { get; init; } = null!;
}
}

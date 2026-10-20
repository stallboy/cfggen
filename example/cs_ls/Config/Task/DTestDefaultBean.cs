namespace Config.Task;

public partial class DTestDefaultBean
{
    public required int TestInt { get; init; }
    public required bool TestBool { get; init; }
    public required string TestString { get; init; }
    public required DPosition TestSubBean { get; init; }
    public required IReadOnlyList<int> TestList { get; init; }
    public required IReadOnlyList<int> TestList2 { get; init; }
    public required IReadOnlyDictionary<int, string> TestMap { get; init; }
}

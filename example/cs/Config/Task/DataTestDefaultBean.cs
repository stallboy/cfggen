namespace Config.Task;

public partial class DataTestDefaultBean
{
    public required int TestInt { get; init; }
    public required bool TestBool { get; init; }
    public required string TestString { get; init; }
    public required DataPosition TestSubBean { get; init; }
    public required List<int> TestList { get; init; }
    public required List<int> TestList2 { get; init; }
    public required OrderedDictionary<int, string> TestMap { get; init; }
}

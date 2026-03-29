using System.Collections.Generic;
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

    public override int GetHashCode()
    {
        return TestInt.GetHashCode() + TestBool.GetHashCode() + TestString.GetHashCode() + TestSubBean.GetHashCode() + TestList.GetHashCode() + TestList2.GetHashCode() + TestMap.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataTestDefaultBean;
        return o != null && TestInt.Equals(o.TestInt) && TestBool.Equals(o.TestBool) && TestString.Equals(o.TestString) && TestSubBean.Equals(o.TestSubBean) && TestList.Equals(o.TestList) && TestList2.Equals(o.TestList2) && TestMap.Equals(o.TestMap);
    }

    public override string ToString()
    {
        return "(" + TestInt + "," + TestBool + "," + TestString + "," + TestSubBean + "," + StringUtil.ToString(TestList) + "," + StringUtil.ToString(TestList2) + "," + TestMap + ")";
    }

    internal static DataTestDefaultBean _create(Stream os)
    {
        var testInt = os.ReadInt32();
        var testBool = os.ReadBool();
        var testString = os.ReadStringInPool();
        var testSubBean = DataPosition._create(os);
        List<int> testList = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            testList.Add(os.ReadInt32());
        List<int> testList2 = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            testList2.Add(os.ReadInt32());
        OrderedDictionary<int, string> testMap = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            testMap.Add(os.ReadInt32(), os.ReadStringInPool());
        }
        return new DataTestDefaultBean {
            TestInt = testInt,
            TestBool = testBool,
            TestString = testString,
            TestSubBean = testSubBean,
            TestList = testList,
            TestList2 = testList2,
            TestMap = testMap,
        };
    }

}

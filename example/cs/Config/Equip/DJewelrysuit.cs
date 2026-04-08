namespace Config.Equip;

public partial class DJewelrysuit
{
    public static DJewelrysuit SpecialSuit { get; private set; } = null!;

    public required int SuitID { get; init; } /* 饰品套装ID */
    public required string Ename { get; init; }
    public required string Name { get; init; } /* 策划用名字 */
    public required int Ability1 { get; init; } /* 套装属性类型1（装备套装中的两件时增加的属性） */
    public required int Ability1Value { get; init; } /* 套装属性1 */
    public required int Ability2 { get; init; } /* 套装属性类型2（装备套装中的三件时增加的属性） */
    public required int Ability2Value { get; init; } /* 套装属性2 */
    public required int Ability3 { get; init; } /* 套装属性类型3（装备套装中的四件时增加的属性） */
    public required int Ability3Value { get; init; } /* 套装属性3 */
    public required List<int> SuitList { get; init; } /* 部件1 */
    
    private static OrderedDictionary<int, DJewelrysuit> _all = [];

    public static DJewelrysuit? Get(int suitID)
    {
        return _all.GetValueOrDefault(suitID);
    }

    public static IReadOnlyList<DJewelrysuit> All()
    {
        return _all.Values;
    }
}

namespace Config.Equip;

public partial class DJewelrysuit
{
    public static DJewelrysuit SpecialSuit => SpecialSuitIn(ConfigMgr.GetMgr());
    public static DJewelrysuit SpecialSuitIn(ConfigMgr config) => config._equip_djewelrysuit_Symbol_SpecialSuit!;

    public required int SuitID { get; init; } /* 饰品套装ID */
    public required string Ename { get; init; }
    public required Config.Text Name { get; init; } /* 策划用名字 */
    public required int Ability1 { get; init; } /* 套装属性类型1（装备套装中的两件时增加的属性） */
    public required int Ability1Value { get; init; } /* 套装属性1 */
    public required int Ability2 { get; init; } /* 套装属性类型2（装备套装中的三件时增加的属性） */
    public required int Ability2Value { get; init; } /* 套装属性2 */
    public required int Ability3 { get; init; } /* 套装属性类型3（装备套装中的四件时增加的属性） */
    public required int Ability3Value { get; init; } /* 套装属性3 */
    public required IReadOnlyList<int> SuitList { get; init; } /* 部件1 */

    public static DJewelrysuit? Get(int suitID)
        => Get(ConfigMgr.GetMgr(), suitID);

    public static DJewelrysuit? Get(ConfigMgr config, int suitID)
    {
        var index = config._equip_djewelrysuit_All;
        return index.TryGetValue(suitID, out var result) ? result : null;
    }

    public static IReadOnlyList<DJewelrysuit> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DJewelrysuit> All(ConfigMgr config)
        => config._equip_djewelrysuit_Rows;
}

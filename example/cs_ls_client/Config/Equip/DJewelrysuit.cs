using System;
using System.Collections.Generic;
namespace Config.Equip
{

public partial class DJewelrysuit
{
    public static DJewelrysuit SpecialSuit { get; private set; } = null!;

    public int SuitID { get; init; } /* 饰品套装ID */
    public string Ename { get; init; } = null!;
    public Config.Text Name { get; init; } = null!; /* 策划用名字 */
    public int Ability1 { get; init; } /* 套装属性类型1（装备套装中的两件时增加的属性） */
    public int Ability1Value { get; init; } /* 套装属性1 */
    public int Ability2 { get; init; } /* 套装属性类型2（装备套装中的三件时增加的属性） */
    public int Ability2Value { get; init; } /* 套装属性2 */
    public int Ability3 { get; init; } /* 套装属性类型3（装备套装中的四件时增加的属性） */
    public int Ability3Value { get; init; } /* 套装属性3 */
    public List<int> SuitList { get; init; } = null!; /* 部件1 */
    private static IReadOnlyList<DJewelrysuit> _allList = null!;
    
    private static Dictionary<int, DJewelrysuit> _all = null!;

    public static DJewelrysuit? Get(int suitID)
    {
        return _all.GetValueOrDefault(suitID);
    }

    public static IReadOnlyList<DJewelrysuit> All()
    {
        return _allList;
    }
}
}

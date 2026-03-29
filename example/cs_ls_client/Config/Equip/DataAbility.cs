using System.Collections.Generic;
namespace Config.Equip;

public partial class DataAbility
{
    public static DataAbility Attack { get; private set; } = null!;
    public static DataAbility Defence { get; private set; } = null!;
    public static DataAbility Hp { get; private set; } = null!;
    public static DataAbility Critical { get; private set; } = null!;
    public static DataAbility Critical_resist { get; private set; } = null!;
    public static DataAbility Block { get; private set; } = null!;
    public static DataAbility Break_armor { get; private set; } = null!;
    public required int Id { get; init; } /* 属性类型 */
    public required string Name { get; init; } /* 程序用名字 */

    public override int GetHashCode()
    {
        return Id.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataAbility;
        return o != null && Id.Equals(o.Id);
    }

    public override string ToString()
    {
        return "(" + Id + "," + Name + ")";
    }

    
    private static OrderedDictionary<int, DataAbility> _all = [];

    public static DataAbility? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataAbility> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Id, self);
            if (self.Name.Trim().Length == 0)
                continue;
            switch(self.Name.Trim())
            {
                case "attack":
                    if (Attack != null)
                        errors.EnumDup("equip.ability", self.ToString());
                    Attack = self;
                    break;
                case "defence":
                    if (Defence != null)
                        errors.EnumDup("equip.ability", self.ToString());
                    Defence = self;
                    break;
                case "hp":
                    if (Hp != null)
                        errors.EnumDup("equip.ability", self.ToString());
                    Hp = self;
                    break;
                case "critical":
                    if (Critical != null)
                        errors.EnumDup("equip.ability", self.ToString());
                    Critical = self;
                    break;
                case "critical_resist":
                    if (Critical_resist != null)
                        errors.EnumDup("equip.ability", self.ToString());
                    Critical_resist = self;
                    break;
                case "block":
                    if (Block != null)
                        errors.EnumDup("equip.ability", self.ToString());
                    Block = self;
                    break;
                case "break_armor":
                    if (Break_armor != null)
                        errors.EnumDup("equip.ability", self.ToString());
                    Break_armor = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.ability", self.ToString());
                    break;
            }
        }

        if (Attack == null)
            errors.EnumNull("equip.ability", "attack");
        if (Defence == null)
            errors.EnumNull("equip.ability", "defence");
        if (Hp == null)
            errors.EnumNull("equip.ability", "hp");
        if (Critical == null)
            errors.EnumNull("equip.ability", "critical");
        if (Critical_resist == null)
            errors.EnumNull("equip.ability", "critical_resist");
        if (Block == null)
            errors.EnumNull("equip.ability", "block");
        if (Break_armor == null)
            errors.EnumNull("equip.ability", "break_armor");
    }

    internal static DataAbility _create(Stream os)
    {
        var id = os.ReadInt32();
        var name = os.ReadStringInPool();
        return new DataAbility {
            Id = id,
            Name = name,
        };
    }

}

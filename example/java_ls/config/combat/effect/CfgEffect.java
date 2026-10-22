package config.combat.effect;

public sealed interface CfgEffect permits CfgDamage, CfgHeal, CfgNone {
    static CfgEffect _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        return switch (tag) {
            case "Damage" -> config.combat.effect.CfgDamage._create(input);
            case "Heal" -> config.combat.effect.CfgHeal._create(input);
            case "None" -> config.combat.effect.CfgNone._create(input);
            default -> throw input.errors().unknownVariant("CfgEffect", tag);
        };
    }
}

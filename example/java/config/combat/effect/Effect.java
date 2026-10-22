package config.combat.effect;

public sealed interface Effect permits Damage, Heal, None {
    static Effect _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        return switch (tag) {
            case "Damage" -> config.combat.effect.Damage._create(input);
            case "Heal" -> config.combat.effect.Heal._create(input);
            case "None" -> config.combat.effect.None._create(input);
            default -> throw input.errors().unknownVariant("Effect", tag);
        };
    }
}

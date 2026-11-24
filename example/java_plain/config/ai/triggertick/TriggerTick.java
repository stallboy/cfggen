package config.ai.triggertick;

public sealed interface TriggerTick permits ConstValue, ByLevel, ByServerUpDay {
    static TriggerTick _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        return switch (tag) {
            case "ConstValue" -> config.ai.triggertick.ConstValue._create(input);
            case "ByLevel" -> config.ai.triggertick.ByLevel._create(input);
            case "ByServerUpDay" -> config.ai.triggertick.ByServerUpDay._create(input);
            default -> throw input.errors().unknownVariant("TriggerTick", tag);
        };
    }
}

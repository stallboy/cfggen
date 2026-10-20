package config.ai.triggertick;

public sealed interface CfgTriggerTick permits CfgConstValue, CfgByLevel, CfgByServerUpDay {
    static CfgTriggerTick _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        return switch (tag) {
            case "ConstValue" -> config.ai.triggertick.CfgConstValue._create(input);
            case "ByLevel" -> config.ai.triggertick.CfgByLevel._create(input);
            case "ByServerUpDay" -> config.ai.triggertick.CfgByServerUpDay._create(input);
            default -> throw input.errors().unknownVariant("CfgTriggerTick", tag);
        };
    }
}

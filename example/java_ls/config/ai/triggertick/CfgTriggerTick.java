package config.ai.triggertick;

public sealed interface CfgTriggerTick permits CfgConstValue, CfgByLevel, CfgByServerUpDay {
    static CfgTriggerTick _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        switch (tag) {
            case "ConstValue":
                return config.ai.triggertick.CfgConstValue._create(input);
            case "ByLevel":
                return config.ai.triggertick.CfgByLevel._create(input);
            case "ByServerUpDay":
                return config.ai.triggertick.CfgByServerUpDay._create(input);
        }
        throw new IllegalArgumentException(tag + " not found");
    }
}

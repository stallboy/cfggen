package config.ai.triggertick;

public sealed interface TriggerTick permits ConstValue, ByLevel, ByServerUpDay {
    static TriggerTick _create(configgen.genjava.ConfigInput input) {
        switch (input.readStr()) {
            case "ConstValue":
                return config.ai.triggertick.ConstValue._create(input);
            case "ByLevel":
                return config.ai.triggertick.ByLevel._create(input);
            case "ByServerUpDay":
                return config.ai.triggertick.ByServerUpDay._create(input);
        }
        throw new IllegalArgumentException();
    }
}

package config.ai.triggertick;

public sealed interface TriggerTick permits ConstValue, ByLevel, ByServerUpDay {
    config.ai.Triggerticktype type();

    static TriggerTick _create(configgen.genjava.ConfigInput input) {
        switch(input.readStr()) {
            case "":
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

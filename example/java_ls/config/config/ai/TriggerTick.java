package config.ai;

public interface TriggerTick {
    static TriggerTick _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        switch (tag) {
            case "ConstValue":
                return config.ai.triggertick.ConstValue._create(input);
            case "ByLevel":
                return config.ai.triggertick.ByLevel._create(input);
            case "ByServerUpDay":
                return config.ai.triggertick.ByServerUpDay._create(input);
        }
        throw new IllegalArgumentException(tag + " not found");
    }
}

package config.Ai
import (
    "config"
)
type TriggerTick interface
{
    internal static TriggerTick _create(Config.Stream os) {
        switch(os.ReadString()) {
            case "ConstValue":
                return config.Ai.Triggertick.ConstValue._create(os);
            case "ByLevel":
                return config.Ai.Triggertick.ByLevel._create(os);
            case "ByServerUpDay":
                return config.Ai.Triggertick.ByServerUpDay._create(os);
        }
        return null;
    }
}

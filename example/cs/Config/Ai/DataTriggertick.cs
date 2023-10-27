using System;
using System.Collections.Generic;
using System.IO;
namespace Config.Ai
{
public abstract class DataTriggertick
{
    public abstract Config.Ai.DataTriggerticktype type();

    internal static DataTriggertick _create(Config.Stream os) {
        switch(os.ReadString()) {
            case "":
            case "ConstValue":
                return Config.Ai.Triggertick.DataConstvalue._create(os);
            case "ByLevel":
                return Config.Ai.Triggertick.DataBylevel._create(os);
            case "ByServerUpDay":
                return Config.Ai.Triggertick.DataByserverupday._create(os);
        }
        return null;
    }
}
}

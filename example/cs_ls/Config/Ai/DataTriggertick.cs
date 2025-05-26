using System;
using System.Collections.Generic;

namespace Config.Ai
{
    public abstract class DataTriggertick
    {
        internal static DataTriggertick _create(Config.Stream os)
        {
            switch(os.ReadString())
            {
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

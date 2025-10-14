@import java.util.List
@param String pkg
@param List<String> mapsInMgr
package ${pkg};

public class ConfigMgr {
    private static volatile ConfigMgr mgr;

    public static ConfigMgr getMgr() {
        return mgr;
    }

    public static void setMgr(ConfigMgr newMgr) {
        mgr = newMgr;
    }

    @for(String map : mapsInMgr)
    ${map}

    @endfor
}

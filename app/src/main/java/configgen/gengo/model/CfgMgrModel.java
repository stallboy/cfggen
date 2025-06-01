package configgen.gengo.model;

import configgen.value.CfgValue;

public class CfgMgrModel {
    public final String pkg;
    public final CfgValue cfgValue;

    public CfgMgrModel(String pkg,CfgValue cfgValue) {
        this.pkg = pkg;
        this.cfgValue = cfgValue;
    }
}

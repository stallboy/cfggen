package configgen.value;

import configgen.data.CfgData;

public class Values {
    static CfgData.DCell ofCell(String str) {
        return new CfgData.DCell(str, new CfgData.DRowId("fileName", "sheetName", 0), 0, (byte) 0);
    }

    static CfgValue.VInt ofInt(int v) {
        return new CfgValue.VInt(v, ofCell(String.valueOf(v)));
    }

    static CfgValue.VLong ofLong(long v) {
        return new CfgValue.VLong(v, ofCell(String.valueOf(v)));
    }

    static CfgValue.VString ofStr(String v) {
        return new CfgValue.VString(v, ofCell(v));
    }

    static CfgValue.VBool ofBool(boolean v) {
        return new CfgValue.VBool(v, ofCell(String.valueOf(v)));
    }

    static CfgValue.VFloat ofFloat(float v) {
        return new CfgValue.VFloat(v, ofCell(String.valueOf(v)));
    }

}

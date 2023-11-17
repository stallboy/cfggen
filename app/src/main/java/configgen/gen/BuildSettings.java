package configgen.gen;

import configgen.data.ExcelReader;

public final class BuildSettings {
    public static boolean isIncludePoi = false;
    public static ExcelReader poiReader;

    static {
        try {
            Class<?> readByPoiClass = Class.forName("configgen.data.ReadByPoi");
            Object[] enumConstants = readByPoiClass.getEnumConstants();
            Object instance = enumConstants[0];
            if (instance instanceof ExcelReader reader) {
                isIncludePoi = true;
                poiReader = reader;
            }
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }
}

package configgen.gen;

import configgen.data.ExcelReader;

public final class BuildSettings {
    public static boolean is_include_poi = false;
    public static ExcelReader poiReader;

    static {
        try {
            Class<?> readByPoiClass = Class.forName("configgen.data.ReadByPoi");
            Object[] enumConstants = readByPoiClass.getEnumConstants();
            Object instance = enumConstants[0];
            if (instance instanceof ExcelReader reader) {
                is_include_poi = true;
                poiReader = reader;
            }
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }
}

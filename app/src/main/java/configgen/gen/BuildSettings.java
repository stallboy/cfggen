package configgen.gen;

import configgen.data.ExcelReader;

public final class BuildSettings {
    private static boolean isIncludePoi = false;
    private static ExcelReader poiReader;

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

    public static boolean isIncludePoi() {
        return isIncludePoi;
    }

    public static ExcelReader getPoiReader() {
        return poiReader;
    }

    // 新增方法：获取POI写入器
    public static ExcelReader getPoiWriter() {
        if (!isIncludePoi) {
            return null;
        }
        try {
            Class<?> writeByPoiClass = Class.forName("configgen.data.WriteByPoi");
            Object[] enumConstants = writeByPoiClass.getEnumConstants();
            Object instance = enumConstants[0];
            if (instance instanceof ExcelReader writer) {
                return writer;
            }
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return null;
    }
}

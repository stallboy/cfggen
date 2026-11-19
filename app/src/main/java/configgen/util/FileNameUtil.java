package configgen.util;

import org.jspecify.annotations.Nullable;

/**
 * 数据文件命名约定的解析：从带中文后缀/后缀名的文件名提取code名。
 * 原在 data.DataUtil，因 schema.cfg 等上游包也需要而移到 util（解 schema→data 依赖环）。
 */
public class FileNameUtil {

    public static @Nullable String getCodeName(String fileName) {
        if (fileName.isEmpty()) {
            return null;
        }

        // 只接受首字母是英文字母的
        if (isFirstNotAzChar(fileName)) {
            return null;
        }

        // 不要后缀
        int i = fileName.indexOf('.');
        if (i >= 0) {
            fileName = fileName.substring(0, i);
        }

        // 有没有汉字
        int hanIdx = findFirstHanIndex(fileName);
        if (hanIdx == -1) {
            return fileName.toLowerCase(); // 所有的文件名都小写，但最后尊重cfg文件里的大小写
        }

        // 只要汉字前的，不包括_
        int end = hanIdx;
        if (fileName.charAt(hanIdx - 1) == '_') {
            end = hanIdx - 1;
        }
        return fileName.substring(0, end).toLowerCase();
    }

    public static boolean isFirstNotAzChar(String name) {
        char firstChar = name.charAt(0);
        return ('a' > firstChar || firstChar > 'z') && ('A' > firstChar || firstChar > 'Z');
    }

    public static int findFirstHanIndex(String s) {
        for (int i = 0; i < s.length(); ) {
            int codepoint = s.codePointAt(i);
            if (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
                return i;
            }
            i += Character.charCount(codepoint);
        }
        return -1;
    }
}

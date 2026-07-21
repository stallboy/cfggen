package configgen.genjava.code;

import configgen.schema.CommentData;
import configgen.schema.Nameable;
import configgen.util.LocaleUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 背景：策划给某些 xlsx/sheet 命名无规律，这里把 table 对应的数据文件路径写到生成代码里，
 * 方便不熟悉的人从 table 名反查源文件。
 */
public final class SourceComment {

    public static String of(Nameable nameable, List<String> rawSheetIds) {
        StringBuilder lines = new StringBuilder();
        CommentData cd = nameable.meta().getComment();
        if (cd != null) {
            if (!cd.trailing().isBlank()) {
                appendLine(lines, cd.trailing().trim());
            }else if (!cd.leading().isBlank()) {
                if (cd.leading().lines().count() == 1) {
                    appendLine(lines, cd.leading().trim());
                }
            }
        }
        if (rawSheetIds != null && !rawSheetIds.isEmpty()) {
            // 路径分隔符统一为 '/'：注释中的反斜杠+u 再跟四位十六进制会被 Java 当作 Unicode 转义，
            // 路径里若恰好出现该序列会导致生成的代码编译失败；正斜杠可避开且跨平台一致。
            String paths = rawSheetIds.stream()
                    .map(s -> s.replace('\\', '/'))
                    .collect(Collectors.joining(", "));
            appendLine(lines, LocaleUtil.getFormatedLocaleString("SourceComment.From", "来自: {0}", paths));
        }
        return lines.toString();
    }

    private static void appendLine(StringBuilder lines, String line) {
        if (!lines.isEmpty()) {
            lines.append("\n");
        }
        lines.append("// ").append(line);
    }
}

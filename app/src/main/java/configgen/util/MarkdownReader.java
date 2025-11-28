package configgen.util;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Markdown文件读取器，支持解析frontmatter和正文内容
 */
public class MarkdownReader {

    public record MarkdownDocument(@NotNull Map<String, String> frontmatter,
                                   @NotNull String content) {
    }

    public static MarkdownDocument read(Path path) {
        return read(path, "UTF-8");
    }


    public static MarkdownDocument read(Path path, String encoding) {
        try (BufferedReader reader = Files.newBufferedReader(path, java.nio.charset.Charset.forName(encoding))) {
            return parse(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static MarkdownDocument parse(BufferedReader reader) throws IOException {
        Map<String, String> frontmatter = new LinkedHashMap<>();
        StringBuilder content = new StringBuilder();

        String firstLine = reader.readLine();
        if (firstLine == null) {
            // 空文件
            return new MarkdownDocument(frontmatter, "");
        }

        boolean hasFrontmatter = firstLine.trim().equals("---");

        if (hasFrontmatter) {
            // 解析frontmatter
            parseFrontmatter(reader, frontmatter);
        } else {
            // 没有frontmatter，第一行就是正文
            content.append(firstLine);
            if (!firstLine.isEmpty()) {
                content.append("\n");
            }
        }

        // 读取正文
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
            content.append("\n");
        }

        // 移除末尾多余的换行符
        String contentStr = content.toString();
        if (contentStr.endsWith("\n")) {
            contentStr = contentStr.substring(0, contentStr.length() - 1);
        }

        return new MarkdownDocument(frontmatter, contentStr);
    }

    private static void parseFrontmatter(BufferedReader reader, Map<String, String> frontmatter) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();

            // 遇到结束标记
            if (trimmed.equals("---")) {
                break;
            }

            // 跳过空行
            if (trimmed.isEmpty()) {
                continue;
            }

            // 解析键值对 (key: value)
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                frontmatter.put(key, value);
            }
        }
    }
}
